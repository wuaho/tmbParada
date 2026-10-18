package com.example.tmbparada.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

class FtsQueryTest {
    @Test
    fun `removes accents while preserving searchable text`() {
        assertEquals("Selva de Mar - Peru", normalizeForSearch("Selva de Mar - Perú"))
        assertEquals("Placa Catalunya", normalizeForSearch("Plaça Catalunya"))
    }

    @Test
    fun `builds an accent insensitive prefix query`() {
        assertEquals("peru*", "perú".toFtsPrefixQuery())
        assertEquals("selva* de* mar*", "  Selva de   Mar ".toFtsPrefixQuery())
    }

    @Test
    fun `drops punctuation-only tokens and never builds an empty query`() {
        assertEquals("selva* mar*", "Selva - Mar".toFtsPrefixQuery())
        assertEquals("__no_match__*", " - ".toFtsPrefixQuery())
    }
}
