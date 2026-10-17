package com.example.tmbparada.domain.refresh

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsResult
import com.example.tmbparada.domain.model.ArrivalsSnapshot
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRefreshPolicyTest {
    private val policy = LiveRefreshPolicy(sessionDurationMillis = 600_000L, refreshIntervalMillis = 30_000L)
    private val configured = WidgetState(stop = BusStop("2903", "Selva de Mar"))

    @Test
    fun `starting a session opens the live window and clears errors`() {
        val started = policy.startSession(configured.copy(lastError = RefreshError.NETWORK), nowMillis = 1_000L)

        assertEquals(601_000L, started.liveUntilMillis)
        assertEquals(1_000L, started.refreshStartedAtMillis)
        assertNull(started.lastError)
    }

    @Test
    fun `fetches only for configured widgets inside the live window`() {
        val live = policy.startSession(configured, nowMillis = 0L)

        assertTrue(policy.shouldFetch(live, nowMillis = 599_999L))
        assertFalse(policy.shouldFetch(live, nowMillis = 600_000L))
        assertFalse(policy.shouldFetch(configured, nowMillis = 0L))
        assertFalse(policy.shouldFetch(WidgetState(liveUntilMillis = 10L), nowMillis = 0L))
    }

    @Test
    fun `successful results replace the snapshot`() {
        val arrivals = listOf(Arrival("V15", "Barceloneta", 90_000L))
        val state = configured.copy(refreshStartedAtMillis = 1L, lastError = RefreshError.SERVER)

        val updated = policy.applyResult(state, "2903", ArrivalsResult.Success(arrivals), nowMillis = 5_000L)

        assertEquals(ArrivalsSnapshot(arrivals, fetchedAtMillis = 5_000L), updated.snapshot)
        assertNull(updated.refreshStartedAtMillis)
        assertNull(updated.lastError)
    }

    @Test
    fun `failures keep the previous snapshot`() {
        val snapshot = ArrivalsSnapshot(listOf(Arrival("V15", null, 90_000L)), fetchedAtMillis = 1L)
        val state = configured.copy(snapshot = snapshot, refreshStartedAtMillis = 2L)

        val updated = policy.applyResult(state, "2903", ArrivalsResult.Failure(RefreshError.NETWORK), 5_000L)

        assertEquals(snapshot, updated.snapshot)
        assertEquals(RefreshError.NETWORK, updated.lastError)
        assertNull(updated.refreshStartedAtMillis)
    }

    @Test
    fun `results for a previous stop are discarded`() {
        val result = ArrivalsResult.Success(listOf(Arrival("7", null, 1L)))

        assertEquals(configured, policy.applyResult(configured, "1001", result, nowMillis = 5_000L))
    }

    @Test
    fun `next step waits one interval, shortened to the end of the window`() {
        val live = configured.copy(liveUntilMillis = 100_000L)

        assertEquals(30_000L, policy.nextStepDelay(live, nowMillis = 0L))
        assertEquals(10_000L, policy.nextStepDelay(live, nowMillis = 90_000L))
        assertNull(policy.nextStepDelay(live, nowMillis = 100_000L))
        assertNull(policy.nextStepDelay(configured, nowMillis = 0L))
    }
}
