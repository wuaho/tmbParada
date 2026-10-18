package com.example.tmbparada.live

import com.example.tmbparada.widget.ui.ArrivalRow

/**
 * Decides when to tell the rider to leave: once per line and destination during a live
 * session, when its next bus is [THRESHOLD_MINUTES] or fewer minutes away. Buses already at
 * the stop ("now") are skipped, since there is no time left to act on the alert. With a
 * favorite line, only that line alerts.
 */
object ArrivalAlerts {
    const val THRESHOLD_MINUTES = 3

    fun due(rows: List<ArrivalRow>, alreadyAlerted: Set<String>, favoriteLine: String? = null): List<ArrivalRow> =
        rows.filter { row ->
            row.minutes.firstOrNull() in 1..THRESHOLD_MINUTES &&
                key(row) !in alreadyAlerted &&
                (favoriteLine == null || row.line == favoriteLine)
        }

    fun key(row: ArrivalRow): String = "${row.line}|${row.destination.orEmpty()}"
}
