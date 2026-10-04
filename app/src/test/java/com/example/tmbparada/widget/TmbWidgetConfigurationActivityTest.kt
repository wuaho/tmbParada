package com.example.tmbparada.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TmbWidgetConfigurationActivityTest {
    @Test
    fun `accepts numeric stop ids`() {
        assertTrue(isValidStopId("2903"))
    }

    @Test
    fun `rejects blank and non numeric stop ids`() {
        assertFalse(isValidStopId(""))
        assertFalse(isValidStopId("29A3"))
        assertFalse(isValidStopId(" 2903 "))
    }
}
