package com.example.tmbparada.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tmbparada.data.TmbConfig
import com.example.tmbparada.data.TmbStateRepository
import java.util.concurrent.TimeUnit

object TmbRefreshScheduler {
    private const val WORK_NAME_PREFIX = "tmb-live-refresh-"
    const val INPUT_WIDGET_ID = "widget_id"

    fun start(context: Context, widgetId: Int) {
        val appContext = context.applicationContext
        TmbStateRepository(appContext).beginLive(widgetId)

        val request = OneTimeWorkRequestBuilder<TmbRefreshWorker>()
            .setInputData(workDataOf(INPUT_WIDGET_ID to widgetId))
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            workName(widgetId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleNext(context: Context, widgetId: Int, delayMillis: Long) {
        if (delayMillis <= 0L) return

        val request = OneTimeWorkRequestBuilder<TmbRefreshWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(INPUT_WIDGET_ID to widgetId))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(widgetId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    fun cancel(context: Context, widgetId: Int) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(widgetId))
    }

    fun intervalMillis(): Long =
        TmbConfig.REFRESH_INTERVAL_SECONDS * 1_000L

    private fun workName(widgetId: Int): String = "$WORK_NAME_PREFIX$widgetId"
}
