package com.example.tmbparada.domain.refresh

import com.example.tmbparada.domain.ArrivalsRepository
import com.example.tmbparada.domain.Clock
import com.example.tmbparada.domain.WidgetStateStore
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.WidgetSettings
import com.example.tmbparada.domain.model.WidgetState

/** Coordinates configuration, live refresh sessions, and cleanup for widget instances. */
class LiveRefreshController(
    private val store: WidgetStateStore,
    private val arrivals: ArrivalsRepository,
    private val scheduler: RefreshScheduler,
    private val renderer: WidgetRenderer,
    private val policy: LiveRefreshPolicy = LiveRefreshPolicy(),
    private val clock: Clock = Clock.System,
) {
    /**
     * Assigns [stop] to a widget, discarding data from any previous stop, and loads arrivals.
     * Display settings are kept; a favorite line the new stop does not serve is dropped.
     */
    suspend fun configure(widgetId: Int, stop: BusStop) {
        store.update(widgetId) { WidgetState(stop = stop, settings = it.settings.forStop(stop)) }
        startLiveSession(widgetId)
    }

    suspend fun updateSettings(widgetId: Int, settings: WidgetSettings) {
        store.update(widgetId) { it.copy(settings = settings) }
        renderer.render(widgetId)
    }

    /** Opens (or extends) a live session and refreshes immediately. */
    suspend fun startLiveSession(widgetId: Int) {
        val state = store.update(widgetId) { current ->
            if (current.stop == null) current else policy.startSession(current, clock.nowMillis())
        }
        if (state.stop != null) scheduler.runNow(widgetId)
        renderer.render(widgetId)
    }

    /** One step of a live session: fetch, store, and schedule the next step or end the session. */
    suspend fun runRefreshStep(widgetId: Int) {
        val state = store.get(widgetId)
        val stop = state.stop
        if (stop == null || !policy.shouldFetch(state, clock.nowMillis())) {
            store.update(widgetId, policy::endSession)
            renderer.render(widgetId)
            return
        }

        store.update(widgetId) { policy.markRefreshing(it, clock.nowMillis()) }
        renderer.render(widgetId)

        val result = arrivals.fetchArrivals(stop.code)
        val updated = store.update(widgetId) {
            policy.applyResult(it, stop.code, result, clock.nowMillis())
        }

        val delay = policy.nextStepDelay(updated, clock.nowMillis())
        if (delay != null) {
            scheduler.runAfter(widgetId, delay)
        } else {
            store.update(widgetId, policy::endSession)
        }
        renderer.render(widgetId)
    }

    /** Ends a live session early, e.g. from the notification. */
    suspend fun stopLiveSession(widgetId: Int) {
        scheduler.cancel(widgetId)
        store.update(widgetId, policy::endSession)
        renderer.render(widgetId)
    }

    suspend fun onWidgetDeleted(widgetId: Int) {
        scheduler.cancel(widgetId)
        store.delete(widgetId)
    }
}
