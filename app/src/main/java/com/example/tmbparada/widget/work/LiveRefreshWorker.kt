package com.example.tmbparada.widget.work

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tmbparada.appContainer

/** Executes one step of a live session; the controller chains the next one. */
class LiveRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.failure()

        applicationContext.appContainer.liveRefresh.runRefreshStep(widgetId)
        return Result.success()
    }

    companion object {
        const val KEY_WIDGET_ID = "widget_id"
    }
}
