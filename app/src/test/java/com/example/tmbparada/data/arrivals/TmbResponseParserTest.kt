package com.example.tmbparada.data.arrivals

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmbResponseParserTest {
    @Test
    fun `parses line, destination, and seconds from ibus payload`() {
        val response = """
            {
              "status": "success",
              "data": {
                "ibus": [
                  {"routeId": "212", "line": "V15", "destination": "Barceloneta", "t-in-s": 240, "t-in-min": 4},
                  {"routeId": "112", "line": "H12", "destination": "Gornal", "t-in-s": "75", "t-in-min": 1}
                ]
              }
            }
        """.trimIndent()

        val arrivals = TmbResponseParser.parse(response)

        assertEquals(
            listOf(
                ParsedArrival("H12", "Gornal", 75),
                ParsedArrival("V15", "Barceloneta", 240),
            ),
            arrivals,
        )
    }

    @Test
    fun `accepts minute fields from alternate api payloads`() {
        val response = """{"data": [{"linea": "D40", "destino": "Placa Espanya", "minutes": 6}]}"""

        assertEquals(listOf(ParsedArrival("D40", "Placa Espanya", 360)), TmbResponseParser.parse(response))
    }

    @Test
    fun `ignores entries without a usable time and removes duplicates`() {
        val response = """
            {"data": {"ibus": [
              {"line": "7", "t-in-s": 30},
              {"line": "7", "t-in-s": 30},
              {"line": "19", "t-in-s": "soon"},
              {"line": "59", "t-in-s": -5}
            ]}}
        """.trimIndent()

        assertEquals(listOf(ParsedArrival("7", null, 30)), TmbResponseParser.parse(response))
    }

    @Test
    fun `returns no arrivals for an empty stop`() {
        assertTrue(TmbResponseParser.parse("""{"status": "success", "data": {"ibus": []}}""").isEmpty())
    }

    @Test(expected = SerializationException::class)
    fun `rejects bodies that are not json`() {
        TmbResponseParser.parse("<html>Service unavailable</html>")
    }
}
