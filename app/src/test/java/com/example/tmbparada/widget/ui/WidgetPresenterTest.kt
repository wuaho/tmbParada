package com.example.tmbparada.widget.ui

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsSnapshot
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetState
import com.example.tmbparada.domain.model.WidgetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPresenterTest {
    private val now = 10 * MINUTE
    private val stop = BusStop("2903", "Selva de Mar")

    private val liveUntil = now + 5 * MINUTE

    /** Presents a widget during a live session. */
    private fun present(state: WidgetState) =
        WidgetPresenter.present(state.copy(liveUntilMillis = state.liveUntilMillis ?: liveUntil), now) as WidgetUiModel.Active

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
    fun `favorite lines come first and are flagged`() {
        val arrivals = listOf(
            Arrival("H16", "Pg. Zona Franca", now + MINUTE),
            Arrival("V29", "Diagonal Mar", now + 5 * MINUTE),
            Arrival("7", null, now + 2 * MINUTE),
        )

        val rows = WidgetPresenter.groupArrivals(arrivals, now, favoriteLines = listOf("V29", "7"))

        assertEquals(listOf("7", "V29", "H16"), rows.map(ArrivalRow::line))
        assertEquals(listOf(true, true, false), rows.map(ArrivalRow::isFavorite))
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
    fun `data that ages during a session is marked stale`() {
        val fresh = present(WidgetState(stop = stop, snapshot = ArrivalsSnapshot(emptyList(), now - 2 * MINUTE)))
        val stale = present(WidgetState(stop = stop, snapshot = ArrivalsSnapshot(emptyList(), now - 5 * MINUTE)))

        assertEquals(Placeholder.NO_BUSES, fresh.placeholder)
        assertFalse(fresh.isStale)
        assertTrue(stale.isStale)
    }

    @Test
    fun `without a live session arrivals are hidden and the widget offers to start one`() {
        val arrivals = listOf(Arrival("7", null, now + MINUTE))
        val ended = WidgetState(
            stop = stop,
            snapshot = ArrivalsSnapshot(arrivals, now - MINUTE),
            liveUntilMillis = now - 1L,
        )

        assertEquals(
            WidgetUiModel.Idle("Selva de Mar", "2903", error = null, theme = WidgetTheme.SYSTEM),
            WidgetPresenter.present(ended, now),
        )
        assertEquals(
            WidgetUiModel.Idle("Selva de Mar", "2903", error = null, theme = WidgetTheme.SYSTEM),
            WidgetPresenter.present(ended.copy(liveUntilMillis = null), now),
        )
    }

    @Test
    fun `idle widgets only keep errors the user has to fix`() {
        val idle = WidgetState(stop = stop)

        assertEquals(
            RefreshError.MISSING_CREDENTIALS,
            (WidgetPresenter.present(idle.copy(lastError = RefreshError.MISSING_CREDENTIALS), now) as WidgetUiModel.Idle).error,
        )
        assertEquals(
            null,
            (WidgetPresenter.present(idle.copy(lastError = RefreshError.NETWORK), now) as WidgetUiModel.Idle).error,
        )
    }

    @Test
    fun `in-flight requests show progress until they time out`() {
        val loading = WidgetPresenter.present(WidgetState(stop = stop, refreshStartedAtMillis = now - 5_000L), now)
        val interrupted = WidgetPresenter.present(WidgetState(stop = stop, refreshStartedAtMillis = now - 2 * MINUTE), now)

        val active = loading as WidgetUiModel.Active
        assertTrue(active.isRefreshing)
        assertEquals(Status.Updating, active.status)
        assertEquals(Placeholder.LOADING, active.placeholder)
        assertTrue(interrupted is WidgetUiModel.Idle)
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
