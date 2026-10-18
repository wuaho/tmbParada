package com.example.tmbparada.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.unit.ColorProvider
import com.example.tmbparada.domain.model.WidgetTheme
import androidx.glance.color.ColorProvider as dayNight

/** The colors the widget draws with, resolved from the [WidgetTheme] chosen by the user. */
data class WidgetPalette(
    val background: ColorProvider,
    val text: ColorProvider,
    val secondaryText: ColorProvider,
    /** Time of the next bus. */
    val accent: ColorProvider,
    /** A bus that is arriving now. */
    val arriving: ColorProvider,
    val error: ColorProvider,
    /** Refresh button and grid tiles. */
    val chip: ColorProvider,
    val onChip: ColorProvider,
    /** Dot shown next to the status while a live session runs. */
    val live: ColorProvider,
)

val LocalWidgetPalette = staticCompositionLocalOf<WidgetPalette> { error("No WidgetPalette provided") }

@Composable
fun widgetPalette(theme: WidgetTheme): WidgetPalette = when (theme) {
    WidgetTheme.SYSTEM -> GlanceTheme.colors.let { colors ->
        WidgetPalette(
            background = colors.widgetBackground,
            text = colors.onSurface,
            secondaryText = colors.onSurfaceVariant,
            accent = colors.primary,
            arriving = colors.tertiary,
            error = colors.error,
            chip = colors.secondaryContainer,
            onChip = colors.onSecondaryContainer,
            live = colors.error,
        )
    }
    WidgetTheme.TMB -> WidgetPalette(
        background = dayNight(day = Color.White, night = Color(0xFF161616)),
        text = dayNight(day = Color(0xFF1A1A1A), night = Color(0xFFF2F2F2)),
        secondaryText = dayNight(day = Color(0xFF5F5F5F), night = Color(0xFFB8B8B8)),
        accent = dayNight(day = TMB_RED, night = TMB_RED_LIGHT),
        arriving = dayNight(day = TMB_RED, night = TMB_RED_LIGHT),
        error = dayNight(day = Color(0xFFB3261E), night = Color(0xFFF2B8B5)),
        chip = dayNight(day = Color(0xFFFDE8EA), night = Color(0xFF3B1C1F)),
        onChip = dayNight(day = TMB_RED, night = TMB_RED_LIGHT),
        live = dayNight(day = TMB_RED, night = TMB_RED_LIGHT),
    )
    WidgetTheme.TRANSPARENT -> WidgetPalette(
        background = ColorProvider(Color(0x59000000)),
        text = ColorProvider(Color.White),
        secondaryText = ColorProvider(Color(0xD9FFFFFF)),
        accent = ColorProvider(Color.White),
        arriving = ColorProvider(Color(0xFFFF9E80)),
        error = ColorProvider(Color(0xFFFFB4AB)),
        chip = ColorProvider(Color(0x33FFFFFF)),
        onChip = ColorProvider(Color.White),
        live = ColorProvider(Color(0xFFFF5252)),
    )
}

private val TMB_RED = Color(0xFFE2001A)
private val TMB_RED_LIGHT = Color(0xFFFF6B74)
