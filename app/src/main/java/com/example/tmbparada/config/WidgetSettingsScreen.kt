package com.example.tmbparada.config

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tmbparada.R
import com.example.tmbparada.domain.model.WidgetSettings
import com.example.tmbparada.domain.model.WidgetTheme
import com.example.tmbparada.ui.LineBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    viewModel: StopPickerViewModel,
    onDone: (WidgetSettings) -> Unit,
    onClose: () -> Unit,
) {
    val stored by viewModel.settings.collectAsStateWithLifecycle()
    val stop by viewModel.currentStop.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.picker_close))
                    }
                },
            )
        },
    ) { padding ->
        val initial = stored ?: return@Scaffold
        var favoriteLines by remember(initial) { mutableStateOf(initial.favoriteLines) }
        var theme by remember(initial) { mutableStateOf(initial.theme) }
        var alertMinutes by remember(initial) { mutableStateOf(initial.alertMinutes) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            stop?.let { currentStop ->
                Text(
                    currentStop.name ?: stringResource(R.string.widget_stop_code, currentStop.code),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Section(
                R.string.settings_favorite_title,
                if (favoriteLines.isEmpty()) R.string.settings_favorite_none_body else R.string.settings_favorite_body,
            ) {
                FavoriteLinesPicker(
                    lines = stop?.lines.orEmpty(),
                    selected = favoriteLines,
                    onToggle = { line ->
                        // Keep the order in which lines were chosen: the first one is the big one.
                        favoriteLines = if (line in favoriteLines) favoriteLines - line else favoriteLines + line
                    },
                )
            }

            Section(R.string.settings_alert_title, R.string.settings_alert_body) {
                AlertMinutesPicker(selected = alertMinutes, onSelect = { alertMinutes = it })
            }

            Section(R.string.settings_theme_title, body = null) {
                Column {
                    ThemeOption(WidgetTheme.SYSTEM, R.string.settings_theme_system, R.string.settings_theme_system_body, theme) { theme = it }
                    ThemeOption(WidgetTheme.TMB, R.string.settings_theme_tmb, R.string.settings_theme_tmb_body, theme) { theme = it }
                    ThemeOption(WidgetTheme.TRANSPARENT, R.string.settings_theme_transparent, R.string.settings_theme_transparent_body, theme) { theme = it }
                }
            }

            Button(
                onClick = {
                    onDone(WidgetSettings(favoriteLines = favoriteLines, theme = theme, alertMinutes = alertMinutes))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_done))
            }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, @StringRes body: Int?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        body?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteLinesPicker(lines: List<String>, selected: List<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            FilterChip(
                selected = line in selected,
                onClick = { onToggle(line) },
                label = { LineBadge(line) },
                leadingIcon = if (line in selected) {
                    { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlertMinutesPicker(selected: Int?, onSelect: (Int?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.settings_alert_off)) },
        )
        WidgetSettings.ALERT_MINUTE_OPTIONS.forEach { minutes ->
            FilterChip(
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                label = { Text(stringResource(R.string.widget_minutes, minutes)) },
            )
        }
    }
}

@Composable
private fun ThemeOption(
    option: WidgetTheme,
    @StringRes title: Int,
    @StringRes body: Int,
    selected: WidgetTheme,
    onSelect: (WidgetTheme) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = option == selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        ThemeSwatch(option)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A tiny widget mock-up in each style's colors. */
@Composable
private fun ThemeSwatch(theme: WidgetTheme) {
    val shape = RoundedCornerShape(8.dp)
    val modifier = Modifier.size(width = 44.dp, height = 32.dp)
    val (background, accent) = when (theme) {
        WidgetTheme.SYSTEM -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.primary
        WidgetTheme.TMB -> Color.White to Color(0xFFE2001A)
        WidgetTheme.TRANSPARENT -> Color(0x59000000) to Color.White
    }
    // The transparent style is drawn over a stand-in wallpaper so the veil is visible.
    val wallpaper = Brush.linearGradient(listOf(Color(0xFF7FA7D9), Color(0xFFE7B07A)))
    Box(
        modifier = modifier
            .background(if (theme == WidgetTheme.TRANSPARENT) wallpaper else Brush.linearGradient(listOf(background, background)), shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (theme == WidgetTheme.TRANSPARENT) background else Color.Transparent, shape)
                .padding(6.dp),
        ) {
            Box(
                Modifier
                    .size(width = 18.dp, height = 6.dp)
                    .background(accent, RoundedCornerShape(3.dp))
                    .align(Alignment.BottomEnd),
            )
        }
    }
}
