package com.example.tmbparada.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WidgetState(
    val stopId: String = TmbConfig.defaultStopId,
    val lines: List<String> = emptyList(),
    val arrivals: List<BusArrival> = emptyList(),
    val lastUpdatedEpochMillis: Long? = null,
    val activeUntilEpochMillis: Long? = null,
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

    fun read(): WidgetState {
        val encoded = preferences.getString(KEY_STATE, null) ?: return WidgetState()
        return runCatching { json.decodeFromString<WidgetState>(encoded) }
            .getOrDefault(WidgetState())
    }

    fun save(state: WidgetState) {
        preferences.edit()
            .putString(KEY_STATE, json.encodeToString(state))
            .apply()
    }

    fun beginLive(nowMillis: Long = System.currentTimeMillis()): WidgetState {
        return read().copy(
            activeUntilEpochMillis = nowMillis + TmbConfig.LIVE_WINDOW_MILLIS,
            errorMessage = null,
        ).also(::save)
    }

    private companion object {
        const val PREFERENCES_NAME = "tmb_widget_state"
        const val KEY_STATE = "state"
    }
}
