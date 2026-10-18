package com.example.tmbparada.domain.refresh

import com.example.tmbparada.domain.model.ArrivalsResult
import com.example.tmbparada.domain.model.ArrivalsSnapshot
import com.example.tmbparada.domain.model.WidgetState

/**
 * Pure state transitions of a live refresh session.
 *
 * Idle widgets never touch the network. Tapping refresh opens a session of
 * [sessionDurationMillis] during which arrivals are requested every [refreshIntervalMillis].
 */
class LiveRefreshPolicy(
    val sessionDurationMillis: Long = DEFAULT_SESSION_DURATION_MILLIS,
    val refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS,
) {
    init {
        require(sessionDurationMillis > 0 && refreshIntervalMillis > 0)
    }

    fun startSession(state: WidgetState, nowMillis: Long): WidgetState =
        state.copy(
            liveUntilMillis = nowMillis + sessionDurationMillis,
            refreshStartedAtMillis = nowMillis,
            lastError = null,
        )

    fun shouldFetch(state: WidgetState, nowMillis: Long): Boolean =
        state.stop != null && state.isLive(nowMillis)

    fun markRefreshing(state: WidgetState, nowMillis: Long): WidgetState =
        state.copy(refreshStartedAtMillis = nowMillis)

    /** Applies a response, ignoring it when the widget was reconfigured while it was in flight. */
    fun applyResult(
        state: WidgetState,
        stopCode: String,
        result: ArrivalsResult,
        nowMillis: Long,
    ): WidgetState {
        if (state.stop?.code != stopCode) return state
        return when (result) {
            is ArrivalsResult.Success -> state.copy(
                snapshot = ArrivalsSnapshot(result.arrivals, fetchedAtMillis = nowMillis),
                refreshStartedAtMillis = null,
                lastError = null,
            )
            is ArrivalsResult.Failure -> state.copy(
                refreshStartedAtMillis = null,
                lastError = result.error,
            )
        }
    }

    /** Delay until the next step, or `null` when the session has ended. */
    fun nextStepDelay(state: WidgetState, nowMillis: Long): Long? {
        val remaining = (state.liveUntilMillis ?: return null) - nowMillis
        return if (remaining > 0) minOf(refreshIntervalMillis, remaining) else null
    }

    fun endSession(state: WidgetState): WidgetState =
        state.copy(liveUntilMillis = null, refreshStartedAtMillis = null)

    companion object {
        const val DEFAULT_SESSION_DURATION_MILLIS = 10 * 60_000L
        const val DEFAULT_REFRESH_INTERVAL_MILLIS = 20_000L
    }
}
