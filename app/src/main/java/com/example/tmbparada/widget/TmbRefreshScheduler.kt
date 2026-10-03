package com.example.tmbparada.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tmbparada.data.TmbConfig
import com.example.tmbparada.data.TmbStateRepository
import java.util.concurrent.TimeUnit

object TmbRefreshScheduler {
    const val WORK_NAME = "tmb-live-refresh"

    fun start(context: Context) {
        val appContext = context.applicationContext
        TmbStateRepository(appContext).beginLive()

        val request = OneTimeWorkRequestBuilder<TmbRefreshWorker>().build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleNext(context: Context, delayMillis: Long) {
        if (delayMillis <= 0L) return

        val request = OneTimeWorkRequestBuilder<TmbRefreshWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    fun intervalMillis(): Long =
        TmbConfig.REFRESH_INTERVAL_SECONDS * 1_000L
}
