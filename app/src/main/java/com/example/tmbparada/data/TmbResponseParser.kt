package com.example.tmbparada.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.math.ceil

object TmbResponseParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val lineKeys = setOf("line", "linea", "routeshortname", "routename", "route")
    private val secondsKeys = setOf("tins", "timeinseconds", "arrivalinseconds")
    private val minuteKeys = setOf(
        "min",
        "minutes",
        "arrivalminutes",
        "estimatedminutes",
        "timetominutes",
    )
    private val destinationKeys = setOf("destination", "destino", "headsign")

    fun parse(responseBody: String, stopId: String): TmbStopResponse {
        val root = json.parseToJsonElement(responseBody)
        // Decode the envelope as a typed model first, while recursively parsing its flexible payload.
        json.decodeFromJsonElement<TmbApiEnvelope>(root)

        val arrivals = buildList {
            collectArrivals(root, this)
        }
            .distinctBy { Triple(it.line, it.minutes, it.destination) }
            .sortedBy { it.minutes }

        return TmbStopResponse(stopId = stopId, arrivals = arrivals)
    }

    private fun collectArrivals(element: JsonElement, result: MutableList<BusArrival>) {
        when (element) {
            is JsonArray -> element.forEach { collectArrivals(it, result) }
            is JsonObject -> {
                val normalizedKeys = element.keys.associateBy(::normalizeKey)
                val line = lineKeys.firstNotNullOfOrNull { key ->
                    normalizedKeys[key]?.let { element[it]?.asText() }
                }?.takeIf(String::isNotBlank)
                val timeEntry = secondsKeys.firstNotNullOfOrNull { key ->
                    normalizedKeys[key]?.let { key to it }
                } ?: minuteKeys.firstNotNullOfOrNull { key ->
                    normalizedKeys[key]?.let { key to it }
                }

                if (line != null && timeEntry != null) {
                    val minutes = parseMinutes(timeEntry.first, timeEntry.second)
                    if (minutes != null && minutes >= 0) {
                        val destination = destinationKeys.firstNotNullOfOrNull { key ->
                            normalizedKeys[key]?.let { element[it]?.asText() }
                        }?.takeIf(String::isNotBlank)
                        result += BusArrival(line = line, minutes = minutes, destination = destination)
                    }
                }

                element.values.forEach { collectArrivals(it, result) }
            }
            else -> Unit
        }
    }

    private fun parseMinutes(key: String, value: JsonElement): Int? {
        val number = value.asText()?.trim()?.toDoubleOrNull() ?: return null
        return if (secondsKeys.contains(key)) {
            ceil(number / 60.0).toInt()
        } else {
            number.toInt()
        }
    }

    private fun normalizeKey(key: String): String =
        key.lowercase().filter(Char::isLetterOrDigit)

    private fun JsonElement.asText(): String? =
        (this as? JsonPrimitive)?.contentOrNull
}
