package com.example.tmbparada.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetFreshnessTest {
    @Test
    fun `formats relative freshness in minutes`() {
        val now = 10 * 60_000L

        assertEquals("No data yet", WidgetFreshness.relativeLabel(null, now))
        assertEquals("Updated just now", WidgetFreshness.relativeLabel(now - 20_000L, now))
        assertEquals("Updated 2m ago", WidgetFreshness.relativeLabel(now - 2 * 60_000L, now))
        assertEquals("Updated 2h ago", WidgetFreshness.relativeLabel(now - 2 * 60 * 60_000L, now))
    }
}
