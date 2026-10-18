package com.example.tmbparada.domain.model

/**
 * Display preferences of one widget.
 *
 * [favoriteLine] is listed first, shown in the large "next bus" view, and is the only line
 * that triggers the "time to go" alert when set.
 */
data class WidgetSettings(
    val favoriteLine: String? = null,
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
) {
    /** Keeps the favorite only if the new stop is served by that line. */
    fun forStop(stop: BusStop): WidgetSettings =
        copy(favoriteLine = favoriteLine?.takeIf { it in stop.lines })
}

enum class WidgetTheme {
    /** Material You colors taken from the wallpaper. */
    SYSTEM,

    /** White (or black at night) with TMB red accents. */
    TMB,

    /** A light dark veil with white text, so the wallpaper shows through. */
    TRANSPARENT,
}
