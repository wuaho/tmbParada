package com.example.tmbparada.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tmbparada.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onChangeStop: (Int) -> Unit, onCustomize: (Int) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    RequestNotificationPermission()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.credentialsConfigured) MissingCredentialsCard()

            if (state.widgets.isEmpty()) {
                EmptyState(canPin = state.canPinWidgets, onAddWidget = viewModel::addWidget)
            } else {
                SectionTitle(R.string.home_widgets_title)
                state.widgets.forEach { widget ->
                    WidgetCard(
                        widget = widget,
                        onStart = { viewModel.startLive(widget.widgetId) },
                        onStop = { viewModel.stopLive(widget.widgetId) },
                        onChangeStop = { onChangeStop(widget.widgetId) },
                        onCustomize = { onCustomize(widget.widgetId) },
                    )
                }
                if (state.canPinWidgets) {
                    OutlinedButton(onClick = viewModel::addWidget, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_add_another))
                    }
                }
            }

            if (AppLanguage.isSupported) {
                SectionTitle(R.string.home_settings_title)
                LanguageSetting()
            }

            Text(
                stringResource(R.string.home_how_it_works),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MissingCredentialsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_api_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.home_api_missing), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyState(canPin: Boolean, onAddWidget: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_bus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (canPin) {
                Button(onClick = onAddWidget) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_add_widget))
                }
            } else {
                Text(
                    stringResource(R.string.home_add_widget_manually),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WidgetCard(
    widget: PlacedWidget,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onChangeStop: () -> Unit,
    onCustomize: () -> Unit,
) {
    val stop = widget.stop
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            stop == null -> stringResource(R.string.home_widget_not_configured)
                            stop.name != null -> stop.name
                            else -> stringResource(R.string.widget_stop_code, stop.code)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (stop != null && stop.name != null) {
                        Text(
                            stringResource(R.string.widget_stop_code, stop.code),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (widget.isLive) {
                    AssistChip(
                        onClick = onStop,
                        label = { Text(stringResource(R.string.widget_status_live_no_data)) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.ic_live_dot),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(10.dp),
                            )
                        },
                    )
                }
            }

            if (stop == null) {
                FilledTonalButton(onClick = onChangeStop) { Text(stringResource(R.string.widget_choose_stop_title)) }
                return@Column
            }

            if (stop.lines.isNotEmpty()) LineBadges(stop.lines)
            WidgetSummary(widget)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (widget.isLive) {
                    FilledTonalButton(onClick = onStop) { Text(stringResource(R.string.live_action_stop)) }
                } else {
                    FilledTonalButton(onClick = onStart) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.widget_start))
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCustomize) { Text(stringResource(R.string.action_customize)) }
            }
            TextButton(onClick = onChangeStop, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.home_change_stop))
            }
        }
    }
}

/** One line with the widget's favorites and alert, so settings are visible at a glance. */
@Composable
private fun WidgetSummary(widget: PlacedWidget) {
    val settings = widget.settings
    val alert = settings.alertMinutes
        ?.let { stringResource(R.string.home_alert, stringResource(R.string.widget_minutes, it)) }
        ?: stringResource(R.string.home_alert_off)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (settings.favoriteLines.isNotEmpty()) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                settings.favoriteLines.joinToString(", ") + " · ",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(alert, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LanguageSetting() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val current = remember { AppLanguage.current(context) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable { showDialog = true },
            headlineContent = { Text(stringResource(R.string.home_language)) },
            supportingContent = { Text(current.label()) },
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.home_language)) },
            text = {
                Column {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = language == current,
                                    role = Role.RadioButton,
                                    onClick = {
                                        showDialog = false
                                        AppLanguage.set(context, language)
                                    },
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = language == current, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(language.label())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.picker_close)) }
            },
        )
    }
}

@Composable
private fun AppLanguage.label(): String = nativeName ?: stringResource(R.string.language_system)
