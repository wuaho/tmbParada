package com.example.tmbparada.domain

import com.example.tmbparada.domain.model.ArrivalsResult
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.WidgetState
import kotlinx.coroutines.flow.Flow

/** Live arrival predictions. Implementations never throw; failures are reported in the result. */
interface ArrivalsRepository {
    suspend fun fetchArrivals(stopCode: String): ArrivalsResult
}

/** Offline catalog of stops and the lines that serve them. */
interface StopCatalog {
    fun search(query: String): Flow<List<BusStop>>
}

/** Persistent per-widget state. [update] is atomic with respect to other updates. */
interface WidgetStateStore {
    fun observe(widgetId: Int): Flow<WidgetState>

    fun observeAll(): Flow<Map<Int, WidgetState>>

    suspend fun get(widgetId: Int): WidgetState

    suspend fun update(widgetId: Int, transform: (WidgetState) -> WidgetState): WidgetState

    suspend fun delete(widgetId: Int)
}
