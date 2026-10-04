package com.example.tmbparada.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TmbTransitCatalogParserTest {
    @Test
    fun `parses bus lines from geojson`() {
        val response = """
            {"features":[{"properties":{"CODI_LINIA":229,"NOM_LINIA":"V29","DESC_LINIA":"Diagonal Mar / Roquetes","ORIGEN_LINIA":"Diagonal Mar","DESTI_LINIA":"Roquetes"}}]}
        """.trimIndent()

        val result = TmbTransitCatalogParser.parseLines(response)

        assertEquals(BusLine("229", "V29", "Diagonal Mar / Roquetes", "Diagonal Mar", "Roquetes"), result.single())
    }

    @Test
    fun `parses stop id name direction and order from geojson`() {
        val response = """
            {"features":[{"properties":{"CODI_PARADA":2903,"NOM_PARADA":"Diagonal Mar","ORDRE":1,"SENTIT":"A"}}]}
        """.trimIndent()

        val result = TmbTransitCatalogParser.parseStops(response)

        assertEquals(BusStop("2903", "Diagonal Mar", "A", 1), result.single())
    }
}
