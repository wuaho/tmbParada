package com.example.tmbparada.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class BusLine(
    val id: String,
    val name: String,
    val description: String,
    val origin: String,
    val destination: String,
)

data class BusStop(
    val id: String,
    val name: String,
    val direction: String,
    val order: Int,
)

object TmbTransitCatalogParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseLines(responseBody: String): List<BusLine> =
        json.parseToJsonElement(responseBody).features()
            .mapNotNull { feature ->
                val properties = feature.properties() ?: return@mapNotNull null
                BusLine(
                    id = properties.text("CODI_LINIA") ?: return@mapNotNull null,
                    name = properties.text("NOM_LINIA") ?: return@mapNotNull null,
                    description = properties.text("DESC_LINIA").orEmpty(),
                    origin = properties.text("ORIGEN_LINIA").orEmpty(),
                    destination = properties.text("DESTI_LINIA").orEmpty(),
                )
            }
            .distinctBy(BusLine::id)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, BusLine::name))

    fun parseStops(responseBody: String): List<BusStop> =
        json.parseToJsonElement(responseBody).features()
            .mapNotNull { feature ->
                val properties = feature.properties() ?: return@mapNotNull null
                BusStop(
                    id = properties.text("CODI_PARADA") ?: return@mapNotNull null,
                    name = properties.text("NOM_PARADA") ?: return@mapNotNull null,
                    direction = properties.text("SENTIT").orEmpty(),
                    order = properties.number("ORDRE") ?: Int.MAX_VALUE,
                )
            }
            .distinctBy { it.id to it.direction }
            .sortedWith(
                compareBy<BusStop> { it.direction }
                    .thenBy { it.order }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )

    private fun JsonElement.features(): List<JsonElement> =
        (this as? JsonObject)?.get("features") as? JsonArray ?: emptyList()

    private fun JsonElement.properties(): JsonObject? =
        (this as? JsonObject)?.get("properties") as? JsonObject

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.number(key: String): Int? =
        text(key)?.toIntOrNull()
}
