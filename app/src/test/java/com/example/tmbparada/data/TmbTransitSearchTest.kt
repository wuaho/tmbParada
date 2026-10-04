package com.example.tmbparada.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TmbTransitSearchTest {
    @Test
    fun `removes accents while preserving searchable text`() {
        assertEquals("Selva de Mar - Peru", normalizeForSearch("Selva de Mar - Perú"))
        assertEquals("Diagonal - Pere IV", normalizeForSearch("Diagonal - Pere IV"))
    }

    @Test
    fun `builds an accent insensitive prefix query`() {
        assertEquals("peru*", "perú".toFtsPrefixQuery())
        assertEquals("selva* de* mar*", "Selva de Mar".toFtsPrefixQuery())
    }
}
