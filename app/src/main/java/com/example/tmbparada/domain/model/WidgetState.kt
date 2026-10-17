package com.example.tmbparada.domain.model

/**
 * Everything persisted for one widget instance.
 *
 * - [stop] is the configuration chosen by the user.
 * - [snapshot] holds the last successful response, kept across failures.
 * - [liveUntilMillis] is set while a live refresh session is running.
 * - [refreshStartedAtMillis] is set while a request is in flight.
 */
data class WidgetState(
    val stop: BusStop? = null,
    val snapshot: ArrivalsSnapshot? = null,
    val liveUntilMillis: Long? = null,
    val refreshStartedAtMillis: Long? = null,
    val lastError: RefreshError? = null,
) {
    fun isLive(nowMillis: Long): Boolean = liveUntilMillis?.let { nowMillis < it } == true
}
