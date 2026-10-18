package com.example.tmbparada.data.arrivals

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.roundToLong

/** An arrival as reported by the API, relative to the moment the response was produced. */
data class ParsedArrival(
    val line: String,
    val destination: String?,
    val secondsUntilArrival: Long,
)

/**
 * Extracts arrivals from iBus responses. The payload shape differs slightly between API
 * versions, so any object carrying a line and a remaining time is accepted, at any depth.
 */
object TmbResponseParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val lineKeys = listOf("line", "linea", "routeshortname", "routename", "route")
    private val secondsKeys = listOf("tins", "timeinseconds", "arrivalinseconds")
    private val minuteKeys = listOf(
        "tinmin",
        "min",
        "minutes",
        "arrivalminutes",
        "estimatedminutes",
        "timetominutes",
    )
    private val destinationKeys = listOf("destination", "destino", "headsign")

    /** Throws [kotlinx.serialization.SerializationException] when the body is not JSON. */
    fun parse(responseBody: String): List<ParsedArrival> =
        buildList { collectArrivals(json.parseToJsonElement(responseBody), this) }
            .distinct()
            .sortedBy(ParsedArrival::secondsUntilArrival)

    private fun collectArrivals(element: JsonElement, result: MutableList<ParsedArrival>) {
        when (element) {
            is JsonArray -> element.forEach { collectArrivals(it, result) }
            is JsonObject -> {
                parseArrival(element)?.let(result::add)
                element.values.forEach { collectArrivals(it, result) }
            }
            else -> Unit
        }
    }

    private fun parseArrival(element: JsonObject): ParsedArrival? {
        val fields = element.entries.associate { (key, value) -> normalizeKey(key) to value }
        val line = fields.firstText(lineKeys) ?: return null
        val seconds = fields.firstNumber(secondsKeys)
            ?: fields.firstNumber(minuteKeys)?.times(SECONDS_PER_MINUTE)
            ?: return null
        if (seconds < 0) return null

        return ParsedArrival(
            line = line,
            destination = fields.firstText(destinationKeys),
            secondsUntilArrival = seconds.roundToLong(),
        )
    }

    private fun Map<String, JsonElement>.firstText(keys: List<String>): String? =
        keys.firstNotNullOfOrNull { key -> this[key]?.asText()?.trim()?.takeIf(String::isNotEmpty) }

    private fun Map<String, JsonElement>.firstNumber(keys: List<String>): Double? =
        keys.firstNotNullOfOrNull { key -> this[key]?.asText()?.trim()?.toDoubleOrNull() }

    private fun normalizeKey(key: String): String =
        key.lowercase().filter(Char::isLetterOrDigit)

    private fun JsonElement.asText(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private const val SECONDS_PER_MINUTE = 60.0
}
