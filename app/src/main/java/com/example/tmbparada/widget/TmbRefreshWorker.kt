package com.example.tmbparada.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
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
        val state = repository.read()
        val activeUntil = state.activeUntilEpochMillis ?: return Result.success()
        val now = System.currentTimeMillis()

        if (now >= activeUntil) {
            repository.save(state.copy(activeUntilEpochMillis = null))
            TmbWidget().updateAll(applicationContext)
            return Result.success()
        }

        runCatching { client.fetchArrivals(state.stopId) }
            .onSuccess { response ->
                val lines = response.arrivals.map(BusArrival::line).distinct()
                repository.save(
                    state.copy(
                        lines = lines,
                        arrivals = response.arrivals,
                        lastUpdatedEpochMillis = System.currentTimeMillis(),
                        errorMessage = null,
                    ),
                )
            }
            .onFailure { error ->
                repository.save(
                    state.copy(errorMessage = error.message ?: "Could not query TMB"),
                )
            }

        val remainingMillis = activeUntil - System.currentTimeMillis()
        if (remainingMillis > 0L) {
            TmbWidget().updateAll(applicationContext)
            TmbRefreshScheduler.scheduleNext(
                applicationContext,
                minOf(TmbRefreshScheduler.intervalMillis(), remainingMillis),
            )
        } else {
            repository.save(repository.read().copy(activeUntilEpochMillis = null))
            TmbWidget().updateAll(applicationContext)
        }
        return Result.success()
    }
}
