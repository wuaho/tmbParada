package com.example.tmbparada.widget.ui

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetState
import kotlin.math.ceil

/** Maps stored [WidgetState] to [WidgetUiModel] at a given instant. */
object WidgetPresenter {
    /** Arrivals become approximate once the last update is this old, e.g. after failures. */
    const val STALE_AFTER_MILLIS = 5 * 60_000L

    /** A request still marked as in flight after this long was interrupted. */
    const val REFRESH_TIMEOUT_MILLIS = 60_000L

    /** An arrival is kept for a short while after its predicted time: buses are often late. */
    const val DEPARTED_GRACE_MILLIS = 30_000L

    const val MAX_ARRIVALS_PER_ROW = 2

    /** Errors still worth showing once the session is over, because only the user can fix them. */
    private val PERSISTENT_ERRORS = setOf(RefreshError.MISSING_CREDENTIALS, RefreshError.UNAUTHORIZED)

    fun present(state: WidgetState, nowMillis: Long): WidgetUiModel {
        val stop = state.stop ?: return WidgetUiModel.NotConfigured

        val isRefreshing = state.refreshStartedAtMillis
            ?.let { nowMillis - it in 0 until REFRESH_TIMEOUT_MILLIS } == true
        val liveUntil = state.liveUntilMillis?.takeIf { it > nowMillis }
        if (liveUntil == null && !isRefreshing) {
            return WidgetUiModel.Idle(
                stopName = stop.name,
                stopCode = stop.code,
                error = state.lastError?.takeIf { it in PERSISTENT_ERRORS },
                theme = state.settings.theme,
            )
        }

        val snapshot = state.snapshot
        val ageMillis = snapshot?.let { (nowMillis - it.fetchedAtMillis).coerceAtLeast(0L) }
        val rows = snapshot?.let { groupArrivals(it.arrivals, nowMillis, state.settings.favoriteLine) }.orEmpty()

        val status = when {
            isRefreshing -> Status.Updating
            state.lastError != null -> Status.Failed(state.lastError)
            else -> Status.Live(minutesLeft(liveUntil ?: nowMillis, nowMillis), snapshot?.fetchedAtMillis)
        }

        val placeholder = when {
            rows.isNotEmpty() -> null
            state.lastError != null && !isRefreshing -> Placeholder.FAILED
            snapshot != null && !isRefreshing -> Placeholder.NO_BUSES
            else -> Placeholder.LOADING
        }

        return WidgetUiModel.Active(
            stopName = stop.name,
            stopCode = stop.code,
            rows = rows,
            placeholder = placeholder,
            status = status,
            isRefreshing = isRefreshing,
            isStale = ageMillis != null && ageMillis >= STALE_AFTER_MILLIS,
            theme = state.settings.theme,
        )
    }

    /**
     * Groups arrivals by line and destination, ordered by the next bus of each group, with the
     * [favoriteLine] first.
     */
    fun groupArrivals(arrivals: List<Arrival>, nowMillis: Long, favoriteLine: String? = null): List<ArrivalRow> =
        arrivals
            .filter { it.arrivalAtMillis >= nowMillis - DEPARTED_GRACE_MILLIS }
            .sortedBy(Arrival::arrivalAtMillis)
            .groupBy { it.line to it.destination }
            .map { (key, group) ->
                ArrivalRow(
                    line = key.first,
                    destination = key.second,
                    minutes = group.take(MAX_ARRIVALS_PER_ROW).map { minutesUntil(it.arrivalAtMillis, nowMillis) },
                    isFavorite = key.first == favoriteLine,
                )
            }
            .sortedByDescending(ArrivalRow::isFavorite)

    /** Rounds down, so riders are never told they have more time than they do. */
    private fun minutesUntil(arrivalAtMillis: Long, nowMillis: Long): Int =
        ((arrivalAtMillis - nowMillis).coerceAtLeast(0L) / MILLIS_PER_MINUTE).toInt()

    private fun minutesLeft(untilMillis: Long, nowMillis: Long): Int =
        ceil((untilMillis - nowMillis) / MILLIS_PER_MINUTE.toDouble()).toInt().coerceAtLeast(1)

    private const val MILLIS_PER_MINUTE = 60_000L
}
