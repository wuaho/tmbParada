package com.example.tmbparada.data.arrivals

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class TmbITransitParserTest {
    @Test
    fun `lists every upcoming bus relative to the response timestamp`() {
        val response = """
            {
              "timestamp": 1000000,
              "parades": [{
                "codi_parada": "2903",
                "linies_trajectes": [
                  {"codi_linia": "229", "nom_linia": "V29", "desti_trajecte": "Diagonal Mar",
                   "propers_busos": [{"temps_arribada": 1480000, "id_bus": 1}, {"temps_arribada": 2200000}]},
                  {"nom_linia": "7", "desti_trajecte": "Zona Universitària",
                   "propers_busos": [{"temps_arribada": "1120000"}]}
                ]
              }]
            }
        """.trimIndent()

        assertEquals(
            listOf(
                ParsedArrival("7", "Zona Universitària", 120),
                ParsedArrival("V29", "Diagonal Mar", 480),
                ParsedArrival("V29", "Diagonal Mar", 1200),
            ),
            TmbITransitParser.parse(response),
        )
    }

    @Test
    fun `skips malformed entries and clamps buses already due`() {
        val response = """
            {"timestamp": 1000000, "parades": [{"linies_trajectes": [
              {"desti_trajecte": "No line", "propers_busos": [{"temps_arribada": 1100000}]},
              {"nom_linia": "H16", "propers_busos": [{"temps_arribada": 990000}, {"id_bus": 3}]}
            ]}]}
        """.trimIndent()

        assertEquals(listOf(ParsedArrival("H16", null, 0)), TmbITransitParser.parse(response))
    }

    @Test(expected = SerializationException::class)
    fun `rejects responses without a timestamp`() {
        TmbITransitParser.parse("""{"parades": []}""")
    }

    @Test(expected = SerializationException::class)
    fun `rejects ibus-shaped responses`() {
        TmbITransitParser.parse("""{"status": "success", "data": {"ibus": []}}""")
    }
}
