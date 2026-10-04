package com.example.tmbparada.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WidgetState(
    val stopId: String = TmbConfig.defaultStopId,
    val lines: List<String> = emptyList(),
    val arrivals: List<BusArrival> = emptyList(),
    val lastUpdatedEpochMillis: Long? = null,
    val activeUntilEpochMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    fun isLive(nowMillis: Long = System.currentTimeMillis()): Boolean =
        activeUntilEpochMillis?.let { it > nowMillis } == true
}

class TmbStateRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val json = Json { ignoreUnknownKeys = true }

    fun read(widgetId: Int): WidgetState {
        val encoded = preferences.getString(stateKey(widgetId), null) ?: return WidgetState()
        return runCatching { json.decodeFromString<WidgetState>(encoded) }
            .getOrDefault(WidgetState())
    }

    fun save(widgetId: Int, state: WidgetState) {
        preferences.edit()
            .putString(stateKey(widgetId), json.encodeToString(state))
            .apply()
    }

    fun beginLive(widgetId: Int, nowMillis: Long = System.currentTimeMillis()): WidgetState {
        return read(widgetId).copy(
            activeUntilEpochMillis = nowMillis + TmbConfig.LIVE_WINDOW_MILLIS,
            isRefreshing = true,
            errorMessage = null,
        ).also { save(widgetId, it) }
    }

    fun delete(widgetId: Int) {
        preferences.edit().remove(stateKey(widgetId)).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "tmb_widget_state"

        fun stateKey(widgetId: Int): String = "state_$widgetId"
    }
}
