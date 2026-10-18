package com.example.tmbparada.domain.refresh

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsResult
import com.example.tmbparada.domain.model.ArrivalsSnapshot
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetState
import com.example.tmbparada.testing.FakeArrivalsRepository
import com.example.tmbparada.testing.FakeClock
import com.example.tmbparada.testing.FakeRefreshScheduler
import com.example.tmbparada.testing.FakeRefreshScheduler.Call
import com.example.tmbparada.testing.FakeWidgetRenderer
import com.example.tmbparada.testing.FakeWidgetStateStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRefreshControllerTest {
    private val clock = FakeClock(nowMillis = 0L)
    private val store = FakeWidgetStateStore()
    private val arrivals = FakeArrivalsRepository()
    private val scheduler = FakeRefreshScheduler()
    private val renderer = FakeWidgetRenderer()
    private val controller = LiveRefreshController(
        store = store,
        arrivals = arrivals,
        scheduler = scheduler,
        renderer = renderer,
        policy = LiveRefreshPolicy(sessionDurationMillis = 60_000L, refreshIntervalMillis = 30_000L),
        clock = clock,
    )
    private val stop = BusStop("2903", "Selva de Mar", listOf("7"))

    @Test
    fun `configuring a widget resets its data and starts a live session`() = runTest {
        store.update(WIDGET) {
            WidgetState(
                stop = BusStop("1", "Old"),
                snapshot = ArrivalsSnapshot(emptyList(), 0L),
                lastError = RefreshError.NETWORK,
            )
        }

        controller.configure(WIDGET, stop)

        val state = store.get(WIDGET)
        assertEquals(stop, state.stop)
        assertNull(state.snapshot)
        assertNull(state.lastError)
        assertEquals(60_000L, state.liveUntilMillis)
        assertEquals(listOf<Call>(Call.Now(WIDGET)), scheduler.calls)
        assertEquals(listOf(WIDGET), renderer.rendered)
    }

    @Test
    fun `refresh is ignored for widgets without a stop`() = runTest {
        controller.startLiveSession(WIDGET)

        assertEquals(WidgetState(), store.get(WIDGET))
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun `a live session fetches every interval and then ends`() = runTest {
        arrivals.result = ArrivalsResult.Success(listOf(Arrival("7", "Zona Universitària", 120_000L)))
        controller.configure(WIDGET, stop)
        scheduler.calls.clear()

        controller.runRefreshStep(WIDGET)
        assertEquals(listOf<Call>(Call.After(WIDGET, 30_000L)), scheduler.calls.toList())
        assertEquals(ArrivalsSnapshot(listOf(Arrival("7", "Zona Universitària", 120_000L)), 0L), store.get(WIDGET).snapshot)

        clock.advanceBy(45_000L)
        controller.runRefreshStep(WIDGET)
        assertEquals(Call.After(WIDGET, 15_000L), scheduler.calls.last())

        clock.advanceBy(15_000L)
        controller.runRefreshStep(WIDGET)

        assertEquals(listOf("2903", "2903"), arrivals.requestedStops)
        assertEquals(2, scheduler.calls.size)
        assertNull(store.get(WIDGET).liveUntilMillis)
        assertNull(store.get(WIDGET).refreshStartedAtMillis)
    }

    @Test
    fun `failures are stored and the session keeps retrying`() = runTest {
        arrivals.result = ArrivalsResult.Failure(RefreshError.NETWORK)
        controller.configure(WIDGET, stop)

        controller.runRefreshStep(WIDGET)

        assertEquals(RefreshError.NETWORK, store.get(WIDGET).lastError)
        assertEquals(Call.After(WIDGET, 30_000L), scheduler.calls.last())
    }

    @Test
    fun `a response for a stop replaced mid-request is discarded`() = runTest {
        val newStop = BusStop("1001", "Pl. Catalunya")
        arrivals.result = ArrivalsResult.Success(listOf(Arrival("7", null, 1L)))
        arrivals.onFetch = { store.update(WIDGET) { it.copy(stop = newStop) } }
        controller.configure(WIDGET, stop)

        controller.runRefreshStep(WIDGET)

        assertEquals(newStop, store.get(WIDGET).stop)
        assertNull(store.get(WIDGET).snapshot)
    }

    @Test
    fun `deleting a widget cancels its work and forgets its state`() = runTest {
        controller.configure(WIDGET, stop)

        controller.onWidgetDeleted(WIDGET)

        assertEquals(Call.Cancel(WIDGET), scheduler.calls.last())
        assertTrue(store.states.value.isEmpty())
    }

    private companion object {
        const val WIDGET = 42
    }
}
