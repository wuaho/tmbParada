package com.example.tmbparada.domain.refresh

/** Runs [LiveRefreshController.runRefreshStep] in the background, one request per widget at a time. */
interface RefreshScheduler {
    /** Runs a refresh step as soon as possible, replacing anything already scheduled. */
    fun runNow(widgetId: Int)

    /** Runs a refresh step after [delayMillis], after the currently running step finishes. */
    fun runAfter(widgetId: Int, delayMillis: Long)

    fun cancel(widgetId: Int)
}

/** Pushes the latest stored state to the home-screen widget. */
fun interface WidgetRenderer {
    suspend fun render(widgetId: Int)
}
