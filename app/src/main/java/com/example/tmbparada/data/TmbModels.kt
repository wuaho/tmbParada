package com.example.tmbparada.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Top-level shape of the TMB API. The data payload differs slightly between API versions. */
@Serializable
data class TmbApiEnvelope(
    val code: JsonElement? = null,
    val version: JsonElement? = null,
    val datetime: JsonElement? = null,
    val data: JsonElement? = null,
)

data class TmbStopResponse(
    val stopId: String,
    val arrivals: List<BusArrival>,
)

@Serializable
data class BusArrival(
    val line: String,
    val minutes: Int,
    val destination: String? = null,
)
