package com.example.tmbparada.widget.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tmbparada.domain.refresh.RefreshScheduler
import java.util.concurrent.TimeUnit

/**
 * Chains one-time requests under a unique name per widget. There is deliberately no periodic
 * work: an idle widget costs nothing, and the chain stops when the live session ends.
 */
class WorkManagerRefreshScheduler(context: Context) : RefreshScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun runNow(widgetId: Int) {
        workManager.enqueueUniqueWork(workName(widgetId), ExistingWorkPolicy.REPLACE, request(widgetId, 0L))
    }

    override fun runAfter(widgetId: Int, delayMillis: Long) {
        workManager.enqueueUniqueWork(
            workName(widgetId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request(widgetId, delayMillis),
        )
    }

    override fun cancel(widgetId: Int) {
        workManager.cancelUniqueWork(workName(widgetId))
    }

    private fun request(widgetId: Int, delayMillis: Long): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<LiveRefreshWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(LiveRefreshWorker.KEY_WIDGET_ID to widgetId))
            .build()

    private fun workName(widgetId: Int) = "tmb-live-refresh-$widgetId"
}
