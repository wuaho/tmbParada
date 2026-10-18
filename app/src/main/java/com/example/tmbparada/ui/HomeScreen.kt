package com.example.tmbparada.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tmbparada.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onChangeStop: (Int) -> Unit, onCustomize: (Int) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RequestNotificationPermission()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge)
            CredentialsCard(state.credentialsConfigured)
            WidgetsCard(
                state = state,
                onAddWidget = viewModel::addWidget,
                onChangeStop = onChangeStop,
                onCustomize = onCustomize,
            )
            Text(
                stringResource(R.string.home_how_it_works),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CredentialsCard(configured: Boolean) {
    Card(
        colors = if (configured) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_api_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (configured) R.string.home_api_ready else R.string.home_api_missing),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun WidgetsCard(
    state: HomeUiState,
    onAddWidget: () -> Unit,
    onChangeStop: (Int) -> Unit,
    onCustomize: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_widgets_title), style = MaterialTheme.typography.titleMedium)
            if (state.widgets.isEmpty()) {
                Text(stringResource(R.string.home_widgets_empty), style = MaterialTheme.typography.bodyMedium)
            }
            state.widgets.forEach { widget ->
                val stop = widget.stop
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            when {
                                stop == null -> stringResource(R.string.home_widget_not_configured)
                                stop.name != null -> stop.name
                                else -> stringResource(R.string.widget_stop_code, stop.code)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (stop != null) LineBadges(stop.lines)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { onChangeStop(widget.widgetId) }) {
                            Text(stringResource(R.string.home_change_stop))
                        }
                        if (stop != null) {
                            TextButton(onClick = { onCustomize(widget.widgetId) }) {
                                Text(stringResource(R.string.action_customize))
                            }
                        }
                    }
                }
            }
            if (state.canPinWidgets) {
                Button(onClick = onAddWidget) { Text(stringResource(R.string.home_add_widget)) }
            } else {
                Text(stringResource(R.string.home_add_widget_manually), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
