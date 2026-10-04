package com.example.tmbparada.widget

object WidgetFreshness {
    fun relativeLabel(lastUpdatedEpochMillis: Long?, nowMillis: Long): String {
        if (lastUpdatedEpochMillis == null) return "No data yet"

        val ageMillis = (nowMillis - lastUpdatedEpochMillis).coerceAtLeast(0L)
        val minutes = ageMillis / MILLIS_PER_MINUTE
        return when {
            minutes < 1L -> "Updated just now"
            minutes < 60L -> "Updated ${minutes}m ago"
            else -> "Updated ${minutes / 60L}h ago"
        }
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}
