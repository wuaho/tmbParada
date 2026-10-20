package com.example.tmbparada.live

import com.example.tmbparada.widget.ui.ArrivalRow

/**
 * Decides when to tell the rider to leave: when the next bus of a line and destination
 * crosses the alert threshold during a live session, once per line and destination.
 *
 * Only crossings alert. A bus already within the threshold when it is first seen (for example
 * right after starting a session) is on screen anyway, and buses at the stop ("now") leave no
 * time to act. With favorite lines, only those lines alert.
 */
object ArrivalAlerts {
    fun due(
        rows: List<ArrivalRow>,
        previousMinutes: Map<String, Int>,
        alreadyAlerted: Set<String>,
        favoriteLines: Collection<String>,
        thresholdMinutes: Int?,
    ): List<ArrivalRow> {
        if (thresholdMinutes == null) return emptyList()
        return rows.filter { row ->
            val key = key(row)
            val before = previousMinutes[key]
            val now = row.minutes.firstOrNull()
            before != null && before > thresholdMinutes &&
                now != null && now in 1..thresholdMinutes &&
                key !in alreadyAlerted &&
                (favoriteLines.isEmpty() || row.line in favoriteLines)
        }
    }

    fun key(row: ArrivalRow): String = "${row.line}|${row.destination.orEmpty()}"
}
