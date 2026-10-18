package com.example.tmbparada.live

import com.example.tmbparada.widget.ui.ArrivalRow
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrivalAlertsTest {
    private val v29 = ArrivalRow("V29", "Diagonal Mar", listOf(3, 15))
    private val h16 = ArrivalRow("H16", "Pg. Zona Franca", listOf(1))
    private val seven = ArrivalRow("7", null, listOf(4, 11))
    private val atStop = ArrivalRow("59", "Pl. Reina Maria Cristina", listOf(0, 9))

    @Test
    fun `alerts for buses within the threshold that are not at the stop yet`() {
        assertEquals(listOf(v29, h16), ArrivalAlerts.due(listOf(v29, h16, seven, atStop), emptySet()))
    }

    @Test
    fun `alerts each line and destination only once per session`() {
        val alerted = setOf(ArrivalAlerts.key(v29))

        assertEquals(listOf(h16), ArrivalAlerts.due(listOf(v29, h16), alerted))
        assertEquals(
            listOf(ArrivalRow("V29", "Besòs", listOf(2))),
            ArrivalAlerts.due(listOf(ArrivalRow("V29", "Besòs", listOf(2))), alerted),
        )
    }
}
