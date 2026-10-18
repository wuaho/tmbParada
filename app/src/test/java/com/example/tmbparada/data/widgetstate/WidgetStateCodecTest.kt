package com.example.tmbparada.data.widgetstate

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsSnapshot
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetSettings
import com.example.tmbparada.domain.model.WidgetState
import com.example.tmbparada.domain.model.WidgetTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStateCodecTest {
    @Test
    fun `round trips a complete state`() {
        val state = WidgetState(
            stop = BusStop("2903", "Selva de Mar - Perú", listOf("7", "H16")),
            snapshot = ArrivalsSnapshot(listOf(Arrival("H16", "Pg. Zona Franca", 90_000L)), 10_000L),
            liveUntilMillis = 600_000L,
            refreshStartedAtMillis = 20_000L,
            lastError = RefreshError.NETWORK,
            settings = WidgetSettings(favoriteLine = "H16", theme = WidgetTheme.TRANSPARENT),
        )

        assertEquals(state, WidgetStateCodec.decode(WidgetStateCodec.encode(state)))
    }

    @Test
    fun `decodes missing or corrupt data as an unconfigured widget`() {
        assertEquals(WidgetState(), WidgetStateCodec.decode(null))
        assertEquals(WidgetState(), WidgetStateCodec.decode("{not json"))
    }

    @Test
    fun `tolerates unknown fields and error values from newer versions`() {
        val encoded = """{"version": 3, "stop": {"code": "1"}, "lastError": "SOMETHING_NEW", "extra": true}"""

        assertEquals(WidgetState(stop = BusStop("1", null)), WidgetStateCodec.decode(encoded))
    }

    @Test
    fun `migrates the stop from the legacy format and drops relative arrivals`() {
        val legacy = """
            {"stopId": "2903", "lines": ["7", "H16"], "arrivals": [{"line": "7", "minutes": 3}],
             "lastUpdatedEpochMillis": 5, "isRefreshing": true}
        """.trimIndent()

        assertEquals(
            WidgetState(stop = BusStop("2903", name = null, lines = listOf("7", "H16"))),
            WidgetStateCodec.migrateLegacy(legacy),
        )
        assertEquals(WidgetState(), WidgetStateCodec.migrateLegacy("""{"lines": []}"""))
    }
}
