package com.example.tmbparada.live

import android.content.Context
import com.example.tmbparada.domain.refresh.RefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs live refresh steps as in-process coroutines while [LiveSessionService] keeps the process
 * in the foreground. Unlike WorkManager, the cadence is not deferred by Android's battery
 * optimizations, so updates arrive when promised.
 */
class LiveSessionScheduler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val runStep: suspend (widgetId: Int) -> Unit,
) : RefreshScheduler {
    private val jobs = ConcurrentHashMap<Int, Job>()

    override fun runNow(widgetId: Int) {
        jobs.remove(widgetId)?.cancel()
        LiveSessionService.start(context, widgetId)
        launchStep(widgetId, delayMillis = 0L)
    }

    /** Called from within the running step, which finishes on its own; it is not cancelled. */
    override fun runAfter(widgetId: Int, delayMillis: Long) {
        launchStep(widgetId, delayMillis)
    }

    override fun cancel(widgetId: Int) {
        jobs.remove(widgetId)?.cancel()
    }

    private fun launchStep(widgetId: Int, delayMillis: Long) {
        jobs[widgetId] = scope.launch {
            delay(delayMillis)
            runStep(widgetId)
        }
    }
}
