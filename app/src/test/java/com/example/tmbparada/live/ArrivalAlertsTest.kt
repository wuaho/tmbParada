package com.example.tmbparada.live

import com.example.tmbparada.widget.ui.ArrivalRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalAlertsTest {
    private fun row(line: String, minutes: Int, destination: String? = null) =
        ArrivalRow(line, destination, listOf(minutes))

    private fun previous(vararg rows: ArrivalRow) = rows.associate { ArrivalAlerts.key(it) to it.minutes.first() }

    @Test
    fun `alerts when the next bus crosses the threshold`() {
        val before = previous(row("V29", 4), row("H16", 6))
        val now = listOf(row("V29", 3), row("H16", 5))

        assertEquals(listOf(row("V29", 3)), ArrivalAlerts.due(now, before, emptySet(), emptyList(), thresholdMinutes = 3))
        // V29 was already within 5 minutes before, so only H16 crosses that threshold.
        assertEquals(listOf(row("H16", 5)), ArrivalAlerts.due(now, before, emptySet(), emptyList(), thresholdMinutes = 5))
    }

    @Test
    fun `buses already within the threshold when first seen do not alert`() {
        val now = listOf(row("V29", 2))

        assertTrue(ArrivalAlerts.due(now, emptyMap(), emptySet(), emptyList(), thresholdMinutes = 3).isEmpty())
        assertTrue(ArrivalAlerts.due(now, previous(row("V29", 2)), emptySet(), emptyList(), thresholdMinutes = 3).isEmpty())
    }

    @Test
    fun `buses at the stop, alerted rows, other lines and disabled alerts stay silent`() {
        val before = previous(row("V29", 4), row("7", 4))

        assertTrue(ArrivalAlerts.due(listOf(row("V29", 0)), before, emptySet(), emptyList(), 3).isEmpty())
        assertTrue(ArrivalAlerts.due(listOf(row("V29", 3)), before, setOf(ArrivalAlerts.key(row("V29", 3))), emptyList(), 3).isEmpty())
        assertEquals(
            listOf(row("V29", 3)),
            ArrivalAlerts.due(listOf(row("V29", 3), row("7", 3)), before, emptySet(), listOf("V29"), 3),
        )
        assertTrue(ArrivalAlerts.due(listOf(row("V29", 3)), before, emptySet(), emptyList(), null).isEmpty())
    }
}
