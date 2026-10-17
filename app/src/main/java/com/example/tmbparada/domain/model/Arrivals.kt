package com.example.tmbparada.domain.model

/**
 * A predicted bus arrival. The arrival time is stored as an absolute timestamp so the
 * remaining minutes can be recomputed whenever the widget is rendered.
 */
data class Arrival(
    val line: String,
    val destination: String?,
    val arrivalAtMillis: Long,
)

data class ArrivalsSnapshot(
    val arrivals: List<Arrival>,
    val fetchedAtMillis: Long,
)

enum class RefreshError {
    MISSING_CREDENTIALS,
    UNAUTHORIZED,
    NETWORK,
    SERVER,
    INVALID_RESPONSE,
}

sealed interface ArrivalsResult {
    data class Success(val arrivals: List<Arrival>) : ArrivalsResult
    data class Failure(val error: RefreshError) : ArrivalsResult
}
