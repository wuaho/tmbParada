package com.example.tmbparada.data.widgetstate

import com.example.tmbparada.domain.model.Arrival
import com.example.tmbparada.domain.model.ArrivalsSnapshot
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetSettings
import com.example.tmbparada.domain.model.WidgetState
import com.example.tmbparada.domain.model.WidgetTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON encoding of [WidgetState]. The stored DTOs are decoupled from the domain model. */
object WidgetStateCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun encode(state: WidgetState): String = json.encodeToString(StoredWidgetState.serializer(), state.toStored())

    /** Corrupt or unknown data decodes to an unconfigured widget instead of crashing it. */
    fun decode(encoded: String?): WidgetState {
        if (encoded == null) return WidgetState()
        return runCatching { json.decodeFromString(StoredWidgetState.serializer(), encoded).toDomain() }
            .getOrDefault(WidgetState())
    }

    /**
     * Converts the state written by version 1.0 of the app, which kept only the stop code. Old
     * arrivals stored relative minutes, so they are dropped rather than shown with wrong times.
     */
    fun migrateLegacy(encoded: String): WidgetState {
        val legacy = runCatching { json.decodeFromString(LegacyWidgetState.serializer(), encoded) }
            .getOrNull()
        val stopCode = legacy?.stopId?.trim()?.takeIf(String::isNotEmpty) ?: return WidgetState()
        return WidgetState(stop = BusStop(code = stopCode, name = null, lines = legacy.lines))
    }
}

@Serializable
private data class StoredWidgetState(
    val version: Int = 2,
    val stop: StoredStop? = null,
    val snapshot: StoredSnapshot? = null,
    val liveUntilMillis: Long? = null,
    val refreshStartedAtMillis: Long? = null,
    val lastError: RefreshError? = null,
    /** Single favorite written by version 2.0.5; read for migration only. */
    val favoriteLine: String? = null,
    val favoriteLines: List<String> = emptyList(),
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val alertMinutes: Int? = WidgetSettings.DEFAULT_ALERT_MINUTES,
)

@Serializable
private data class StoredStop(
    val code: String,
    val name: String? = null,
    val lines: List<String> = emptyList(),
)

@Serializable
private data class StoredSnapshot(
    val fetchedAtMillis: Long,
    val arrivals: List<StoredArrival> = emptyList(),
)

@Serializable
private data class StoredArrival(
    val line: String,
    val destination: String? = null,
    val arrivalAtMillis: Long,
)

@Serializable
private data class LegacyWidgetState(
    val stopId: String? = null,
    val lines: List<String> = emptyList(),
)

private fun WidgetState.toStored() = StoredWidgetState(
    stop = stop?.let { StoredStop(it.code, it.name, it.lines) },
    snapshot = snapshot?.let { snapshot ->
        StoredSnapshot(
            fetchedAtMillis = snapshot.fetchedAtMillis,
            arrivals = snapshot.arrivals.map { StoredArrival(it.line, it.destination, it.arrivalAtMillis) },
        )
    },
    liveUntilMillis = liveUntilMillis,
    refreshStartedAtMillis = refreshStartedAtMillis,
    lastError = lastError,
    favoriteLines = settings.favoriteLines,
    theme = settings.theme,
    alertMinutes = settings.alertMinutes,
)

private fun StoredWidgetState.toDomain() = WidgetState(
    stop = stop?.let { BusStop(it.code, it.name, it.lines) },
    snapshot = snapshot?.let { snapshot ->
        ArrivalsSnapshot(
            arrivals = snapshot.arrivals.map { Arrival(it.line, it.destination, it.arrivalAtMillis) },
            fetchedAtMillis = snapshot.fetchedAtMillis,
        )
    },
    liveUntilMillis = liveUntilMillis,
    refreshStartedAtMillis = refreshStartedAtMillis,
    lastError = lastError,
    settings = WidgetSettings(
        favoriteLines = favoriteLines.ifEmpty { listOfNotNull(favoriteLine) },
        theme = theme,
        alertMinutes = alertMinutes,
    ),
)
