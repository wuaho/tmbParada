package com.example.tmbparada.widget.ui

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsSnapshot
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPresenterTest {
    private val now = 10 * MINUTE
    private val stop = BusStop("2903", "Selva de Mar")

    private fun present(state: WidgetState) = WidgetPresenter.present(state, now) as WidgetUiModel.Stop

    @Test
    fun `widgets without a stop ask to be configured`() {
        assertEquals(WidgetUiModel.NotConfigured, WidgetPresenter.present(WidgetState(), now))
    }

    @Test
    fun `groups arrivals by line and destination with rounded down minutes`() {
        val arrivals = listOf(
            Arrival("V15", "Barceloneta", now + 13 * MINUTE),
            Arrival("H12", "Gornal", now + 59_000L),
            Arrival("V15", "Barceloneta", now + 3 * MINUTE + 50_000L),
            Arrival("V15", "Barceloneta", now + 25 * MINUTE),
            Arrival("V15", "Hospital", now + 5 * MINUTE),
            Arrival("7", null, now - 10_000L),
            Arrival("19", null, now - 2 * MINUTE),
        )

        assertEquals(
            listOf(
                ArrivalRow("7", null, listOf(0)),
                ArrivalRow("H12", "Gornal", listOf(0)),
                ArrivalRow("V15", "Barceloneta", listOf(3, 13)),
                ArrivalRow("V15", "Hospital", listOf(5)),
            ),
            WidgetPresenter.groupArrivals(arrivals, now),
        )
    }

    @Test
    fun `the favorite line comes first and is flagged`() {
        val arrivals = listOf(
            Arrival("H16", "Pg. Zona Franca", now + MINUTE),
            Arrival("V29", "Diagonal Mar", now + 5 * MINUTE),
            Arrival("7", null, now + 2 * MINUTE),
        )

        val rows = WidgetPresenter.groupArrivals(arrivals, now, favoriteLine = "V29")

        assertEquals(listOf("V29", "H16", "7"), rows.map(ArrivalRow::line))
        assertEquals(listOf(true, false, false), rows.map(ArrivalRow::isFavorite))
    }

    @Test
    fun `live sessions report the remaining time`() {
        val model = present(
            WidgetState(
                stop = stop,
                snapshot = ArrivalsSnapshot(listOf(Arrival("7", null, now + MINUTE)), fetchedAtMillis = now),
                liveUntilMillis = now + 4 * MINUTE + 1L,
            ),
        )

        assertEquals(Status.Live(minutesLeft = 5, updatedAtMillis = now), model.status)
        assertEquals(null, model.placeholder)
        assertFalse(model.isStale)
    }

    @Test
    fun `idle widgets report how old their data is and mark it stale`() {
        val fresh = present(WidgetState(stop = stop, snapshot = ArrivalsSnapshot(emptyList(), now - 2 * MINUTE)))
        val stale = present(WidgetState(stop = stop, snapshot = ArrivalsSnapshot(emptyList(), now - 5 * MINUTE)))

        assertEquals(Status.Updated(minutesAgo = 2), fresh.status)
        assertEquals(Placeholder.NO_BUSES, fresh.placeholder)
        assertFalse(fresh.isStale)
        assertTrue(stale.isStale)
    }

    @Test
    fun `very old arrivals are hidden`() {
        val arrivals = listOf(Arrival("7", null, now + MINUTE))
        val model = present(WidgetState(stop = stop, snapshot = ArrivalsSnapshot(arrivals, now - 61 * MINUTE)))

        assertTrue(model.rows.isEmpty())
        assertEquals(Placeholder.TAP_TO_REFRESH, model.placeholder)
    }

    @Test
    fun `in-flight requests show progress until they time out`() {
        val loading = present(WidgetState(stop = stop, refreshStartedAtMillis = now - 5_000L))
        val interrupted = present(WidgetState(stop = stop, refreshStartedAtMillis = now - 2 * MINUTE))

        assertTrue(loading.isRefreshing)
        assertEquals(Status.Updating, loading.status)
        assertEquals(Placeholder.LOADING, loading.placeholder)
        assertFalse(interrupted.isRefreshing)
        assertEquals(Status.NeverUpdated, interrupted.status)
        assertEquals(Placeholder.TAP_TO_REFRESH, interrupted.placeholder)
    }

    @Test
    fun `errors take precedence over freshness but keep previous arrivals`() {
        val arrivals = listOf(Arrival("7", null, now + 2 * MINUTE))
        val model = present(
            WidgetState(
                stop = stop,
                snapshot = ArrivalsSnapshot(arrivals, now - MINUTE),
                lastError = RefreshError.NETWORK,
            ),
        )

        assertEquals(Status.Failed(RefreshError.NETWORK), model.status)
        assertEquals(listOf(ArrivalRow("7", null, listOf(2))), model.rows)

        val empty = present(WidgetState(stop = stop, lastError = RefreshError.MISSING_CREDENTIALS))
        assertEquals(Placeholder.FAILED, empty.placeholder)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
