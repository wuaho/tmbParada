package com.example.tmbparada.widget

import android.content.Context
import androidx.glance.appwidget.AppWidgetId
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tmbparada.data.TmbApiClient
import com.example.tmbparada.data.BusArrival
import com.example.tmbparada.data.TmbStateRepository

class TmbRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    private val repository = TmbStateRepository(appContext)
    private val client = TmbApiClient()

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(TmbRefreshScheduler.INPUT_WIDGET_ID, INVALID_WIDGET_ID)
        if (widgetId == INVALID_WIDGET_ID) return Result.failure()

        val state = repository.read(widgetId)
        val activeUntil = state.activeUntilEpochMillis ?: return Result.success()
        val now = System.currentTimeMillis()

        if (now >= activeUntil) {
            repository.save(widgetId, state.copy(activeUntilEpochMillis = null, isRefreshing = false))
            TmbWidget().update(applicationContext, AppWidgetId(widgetId))
            return Result.success()
        }

        repository.save(widgetId, state.copy(isRefreshing = true))
        TmbWidget().update(applicationContext, AppWidgetId(widgetId))

        runCatching { client.fetchArrivals(state.stopId) }
            .onSuccess { response ->
                val lines = response.arrivals.map(BusArrival::line).distinct()
                repository.save(widgetId,
                    state.copy(
                        lines = lines,
                        arrivals = response.arrivals,
                        lastUpdatedEpochMillis = System.currentTimeMillis(),
                        isRefreshing = false,
                        errorMessage = null,
                    ),
                )
            }
            .onFailure { error ->
                repository.save(widgetId,
                    state.copy(
                        isRefreshing = false,
                        errorMessage = error.message ?: "Could not query TMB",
                    ),
                )
            }

        val remainingMillis = activeUntil - System.currentTimeMillis()
        if (remainingMillis > 0L) {
            TmbWidget().update(applicationContext, AppWidgetId(widgetId))
            TmbRefreshScheduler.scheduleNext(
                applicationContext,
                widgetId,
                minOf(TmbRefreshScheduler.intervalMillis(), remainingMillis),
            )
        } else {
            repository.save(
                widgetId,
                repository.read(widgetId).copy(activeUntilEpochMillis = null, isRefreshing = false),
            )
            TmbWidget().update(applicationContext, AppWidgetId(widgetId))
        }
        return Result.success()
    }

    private companion object {
        const val INVALID_WIDGET_ID = -1
    }
}
