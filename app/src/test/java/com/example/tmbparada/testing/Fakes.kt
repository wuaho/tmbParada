package com.example.tmbparada.testing

import com.example.tmbparada.domain.ArrivalsRepository
import com.example.tmbparada.domain.Clock
import com.example.tmbparada.domain.WidgetStateStore
import com.example.tmbparada.domain.model.ArrivalsResult
import com.example.tmbparada.domain.model.WidgetState
import com.example.tmbparada.domain.refresh.RefreshScheduler
import com.example.tmbparada.domain.refresh.WidgetRenderer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeClock(var nowMillis: Long = 1_000_000L) : Clock {
    override fun nowMillis(): Long = nowMillis

    fun advanceBy(millis: Long) {
        nowMillis += millis
    }
}

class FakeWidgetStateStore : WidgetStateStore {
    val states = MutableStateFlow<Map<Int, WidgetState>>(emptyMap())

    override fun observe(widgetId: Int): Flow<WidgetState> = states.map { it[widgetId] ?: WidgetState() }

    override fun observeAll(): Flow<Map<Int, WidgetState>> = states

    override suspend fun get(widgetId: Int): WidgetState = states.value[widgetId] ?: WidgetState()

    override suspend fun update(widgetId: Int, transform: (WidgetState) -> WidgetState): WidgetState {
        val updated = transform(get(widgetId))
        states.value = states.value + (widgetId to updated)
        return updated
    }

    override suspend fun delete(widgetId: Int) {
        states.value = states.value - widgetId
    }
}

class FakeArrivalsRepository(var result: ArrivalsResult = ArrivalsResult.Success(emptyList())) :
    ArrivalsRepository {
    val requestedStops = mutableListOf<String>()

    /** Runs while the request is "in flight", e.g. to simulate a reconfiguration. */
    var onFetch: suspend () -> Unit = {}

    override suspend fun fetchArrivals(stopCode: String): ArrivalsResult {
        requestedStops += stopCode
        onFetch()
        return result
    }
}

class FakeRefreshScheduler : RefreshScheduler {
    sealed interface Call {
        data class Now(val widgetId: Int) : Call
        data class After(val widgetId: Int, val delayMillis: Long) : Call
        data class Cancel(val widgetId: Int) : Call
    }

    val calls = mutableListOf<Call>()

    override fun runNow(widgetId: Int) {
        calls += Call.Now(widgetId)
    }

    override fun runAfter(widgetId: Int, delayMillis: Long) {
        calls += Call.After(widgetId, delayMillis)
    }

    override fun cancel(widgetId: Int) {
        calls += Call.Cancel(widgetId)
    }
}

class FakeWidgetRenderer : WidgetRenderer {
    val rendered = mutableListOf<Int>()

    override suspend fun render(widgetId: Int) {
        rendered += widgetId
    }
}
