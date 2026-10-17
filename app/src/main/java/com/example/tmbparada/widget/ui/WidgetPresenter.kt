package com.example.tmbparada.widget.ui

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.WidgetState
import kotlin.math.ceil

/** Maps stored [WidgetState] to [WidgetUiModel] at a given instant. */
object WidgetPresenter {
    /** Arrivals become approximate once the last update is this old. */
    const val STALE_AFTER_MILLIS = 5 * 60_000L

    /** Arrivals are hidden once the last update is this old; they would only mislead. */
    const val EXPIRED_AFTER_MILLIS = 60 * 60_000L

    /** A request still marked as in flight after this long was interrupted. */
    const val REFRESH_TIMEOUT_MILLIS = 60_000L

    /** An arrival is kept for a short while after its predicted time: buses are often late. */
    const val DEPARTED_GRACE_MILLIS = 30_000L

    const val MAX_ARRIVALS_PER_ROW = 2

    fun present(state: WidgetState, nowMillis: Long): WidgetUiModel {
        val stop = state.stop ?: return WidgetUiModel.NotConfigured

        val snapshot = state.snapshot
        val ageMillis = snapshot?.let { (nowMillis - it.fetchedAtMillis).coerceAtLeast(0L) }
        val isRefreshing = state.refreshStartedAtMillis
            ?.let { nowMillis - it in 0 until REFRESH_TIMEOUT_MILLIS } == true
        val usableSnapshot = snapshot?.takeIf { ageMillis != null && ageMillis < EXPIRED_AFTER_MILLIS }
        val rows = usableSnapshot?.let { groupArrivals(it.arrivals, nowMillis) }.orEmpty()
        val liveUntil = state.liveUntilMillis?.takeIf { it > nowMillis }

        val status = when {
            isRefreshing -> Status.Updating
            state.lastError != null -> Status.Failed(state.lastError)
            liveUntil != null -> Status.Live(minutesLeft(liveUntil, nowMillis))
            ageMillis != null -> Status.Updated(minutesAgo = ageMillis / MILLIS_PER_MINUTE)
            else -> Status.NeverUpdated
        }

        val placeholder = when {
            rows.isNotEmpty() -> null
            isRefreshing -> Placeholder.LOADING
            state.lastError != null -> Placeholder.FAILED
            usableSnapshot != null -> Placeholder.NO_BUSES
            else -> Placeholder.TAP_TO_REFRESH
        }

        return WidgetUiModel.Stop(
            stopName = stop.name,
            stopCode = stop.code,
            rows = rows,
            placeholder = placeholder,
            status = status,
            isRefreshing = isRefreshing,
            isStale = ageMillis != null && ageMillis >= STALE_AFTER_MILLIS,
        )
    }

    /** Groups arrivals by line and destination, ordered by the next bus of each group. */
    fun groupArrivals(arrivals: List<Arrival>, nowMillis: Long): List<ArrivalRow> =
        arrivals
            .filter { it.arrivalAtMillis >= nowMillis - DEPARTED_GRACE_MILLIS }
            .sortedBy(Arrival::arrivalAtMillis)
            .groupBy { it.line to it.destination }
            .map { (key, group) ->
                ArrivalRow(
                    line = key.first,
                    destination = key.second,
                    minutes = group.take(MAX_ARRIVALS_PER_ROW).map { minutesUntil(it.arrivalAtMillis, nowMillis) },
                )
            }

    /** Rounds down, so riders are never told they have more time than they do. */
    private fun minutesUntil(arrivalAtMillis: Long, nowMillis: Long): Int =
        ((arrivalAtMillis - nowMillis).coerceAtLeast(0L) / MILLIS_PER_MINUTE).toInt()

    private fun minutesLeft(untilMillis: Long, nowMillis: Long): Int =
        ceil((untilMillis - nowMillis) / MILLIS_PER_MINUTE.toDouble()).toInt().coerceAtLeast(1)

    private const val MILLIS_PER_MINUTE = 60_000L
}
