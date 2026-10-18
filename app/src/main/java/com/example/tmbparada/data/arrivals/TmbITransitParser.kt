package com.example.tmbparada.data.arrivals

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses iTransit responses:
 *
 * ```json
 * {"timestamp": 1697616000000, "parades": [{"codi_parada": "2903", "linies_trajectes": [
 *   {"nom_linia": "V29", "desti_trajecte": "Diagonal Mar",
 *    "propers_busos": [{"temps_arribada": 1697616480000}, {"temps_arribada": 1697617200000}]}
 * ]}]}
 * ```
 *
 * Arrival times are absolute; they are converted to seconds relative to the response
 * `timestamp`, so a skewed phone clock does not shift the countdown.
 */
object TmbITransitParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** Throws [SerializationException] when the body does not have the expected shape. */
    fun parse(responseBody: String): List<ParsedArrival> {
        val root = json.parseToJsonElement(responseBody) as? JsonObject
            ?: throw SerializationException("iTransit response is not an object")
        val timestamp = root["timestamp"].long()
            ?: throw SerializationException("iTransit response has no timestamp")
        val stops = root["parades"] as? JsonArray
            ?: throw SerializationException("iTransit response has no parades")

        return stops
            .flatMap { stop -> (stop as? JsonObject)?.get("linies_trajectes").objects() }
            .flatMap { route ->
                val line = route["nom_linia"].text() ?: return@flatMap emptyList()
                val destination = route["desti_trajecte"].text()
                route["propers_busos"].objects().mapNotNull { bus ->
                    val arrivalAt = bus["temps_arribada"].long() ?: return@mapNotNull null
                    ParsedArrival(
                        line = line,
                        destination = destination,
                        secondsUntilArrival = ((arrivalAt - timestamp) / 1_000L).coerceAtLeast(0L),
                    )
                }
            }
            .distinct()
            .sortedBy(ParsedArrival::secondsUntilArrival)
    }

    private fun JsonElement?.objects(): List<JsonObject> =
        (this as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

    private fun JsonElement?.text(): String? =
        (this as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)

    private fun JsonElement?.long(): Long? =
        (this as? JsonPrimitive)?.contentOrNull?.trim()?.toDoubleOrNull()?.toLong()
}
