package com.example.tmbparada.domain.model

/**
 * Display preferences of one widget.
 *
 * - [favoriteLines] are listed first (the first one in the large "next bus" view) and are the
 *   only lines that alert. With none, every line is listed by arrival time and any can alert.
 * - [alertMinutes] is how early the "time to go" alert fires; `null` turns it off.
 */
data class WidgetSettings(
    val favoriteLines: List<String> = emptyList(),
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val alertMinutes: Int? = DEFAULT_ALERT_MINUTES,
) {
    /** Keeps only the favorites served by the new stop. */
    fun forStop(stop: BusStop): WidgetSettings =
        copy(favoriteLines = favoriteLines.filter { it in stop.lines })

    companion object {
        const val DEFAULT_ALERT_MINUTES = 3
        val ALERT_MINUTE_OPTIONS = listOf(1, 2, 3, 4, 5, 7, 10)
    }
}

enum class WidgetTheme {
    /** Material You colors taken from the wallpaper. */
    SYSTEM,

    /** White (or black at night) with TMB red accents. */
    TMB,

    /** A light dark veil with white text, so the wallpaper shows through. */
    TRANSPARENT,
}
