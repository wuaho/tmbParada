package com.example.tmbparada.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BusLinesTest {
    @Test
    fun `orders numeric lines before lettered lines, numerically`() {
        val lines = listOf("V15", "59", "H12", "7", "D20", "136", "V3", "19")

        assertEquals(
            listOf("7", "19", "59", "136", "D20", "H12", "V3", "V15"),
            lines.sortedWith(BusLines.ordering),
        )
    }

    @Test
    fun `colors lines by network family`() {
        assertEquals(BusLines.HORIZONTAL_BLUE, BusLines.colorFor("H12"))
        assertEquals(BusLines.VERTICAL_GREEN, BusLines.colorFor("v15"))
        assertEquals(BusLines.DIAGONAL_PURPLE, BusLines.colorFor("D50"))
        assertEquals(BusLines.CONVENTIONAL_RED, BusLines.colorFor("59"))
    }
}
