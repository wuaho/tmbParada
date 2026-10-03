package com.example.tmbparada.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TmbResponseParserTest {
    @Test
    fun `parses line and seconds remaining from ibus payload`() {
        val response = """
            {
              "version": "1.0",
              "data": {
                "ibus": [
                  {"line": "H12", "destination": "Gornal", "t-in-s": "75"},
                  {"line": "V15", "destination": "Barceloneta", "t-in-s": 240}
                ]
              }
            }
        """.trimIndent()

        val result = TmbResponseParser.parse(response, "1001")

        assertEquals("1001", result.stopId)
        assertEquals(listOf("H12", "V15"), result.arrivals.map { it.line })
        assertEquals(listOf(2, 4), result.arrivals.map { it.minutes })
    }

    @Test
    fun `accepts minute fields from alternate api payloads`() {
        val response = """
            {"data": [{"linea": "D40", "destino": "Placa Espanya", "minutes": 6}]}
        """.trimIndent()

        val result = TmbResponseParser.parse(response, "2002")

        assertEquals("D40", result.arrivals.single().line)
        assertEquals(6, result.arrivals.single().minutes)
    }
}
