package com.example.tmbparada.widget.ui

import com.example.tmbparada.widget.ui.WidgetLayout.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {
    @Test
    fun `short wide widgets use a compact header and keep one whole row`() {
        val layout = WidgetLayout.forSize(widthDp = 270f, heightDp = 90f)

        assertEquals(Mode.LIST, layout.mode)
        assertTrue(layout.compactHeader)
        assertEquals(1, layout.capacity)
        assertFitsHeight(layout, 90f)
    }

    @Test
    fun `wide widgets list as many rows as fit`() {
        val layout = WidgetLayout.forSize(widthDp = 380f, heightDp = 180f)

        assertEquals(Mode.LIST, layout.mode)
        assertEquals(false, layout.compactHeader)
        assertEquals(3, layout.rows)
        assertFitsHeight(layout, 180f)
    }

    @Test
    fun `square widgets show a grid of tiles`() {
        val layout = WidgetLayout.forSize(widthDp = 300f, heightDp = 300f)

        assertEquals(Mode.GRID, layout.mode)
        assertEquals(2, layout.columns)
        assertEquals(3, layout.rows)
        assertFitsHeight(layout, 300f)
    }

    @Test
    fun `narrow widgets stack single tiles`() {
        val layout = WidgetLayout.forSize(widthDp = 170f, heightDp = 200f)

        assertEquals(Mode.GRID, layout.mode)
        assertEquals(1, layout.columns)
        assertEquals(2, layout.rows)
    }

    @Test
    fun `falls back to a list when no tile fits the height`() {
        val layout = WidgetLayout.forSize(widthDp = 120f, heightDp = 95f)

        assertEquals(Mode.LIST, layout.mode)
        assertEquals(1, layout.capacity)
    }

    @Test
    fun `narrow lists drop the destination, then the following bus, before clipping times`() {
        assertEquals(WidgetLayout.ListColumns.FULL, WidgetLayout.forSize(300f, 100f).listColumns)
        // The size of the widget in the reported screenshot: "5 min" was cut to "5 mi".
        assertEquals(WidgetLayout.ListColumns.NO_DESTINATION, WidgetLayout.forSize(200f, 100f).listColumns)
        assertEquals(WidgetLayout.ListColumns.NEXT_ONLY, WidgetLayout.forSize(150f, 95f).listColumns)
    }

    @Test
    fun `respects the glance child limit on very large widgets`() {
        val list = WidgetLayout.forSize(widthDp = 600f, heightDp = 420f)
        val grid = WidgetLayout.forSize(widthDp = 600f, heightDp = 900f)

        assertTrue(list.rows <= WidgetLayout.MAX_LIST_ROWS)
        assertTrue(grid.rows <= WidgetLayout.MAX_GRID_ROWS)
        assertTrue(grid.columns <= WidgetLayout.MAX_GRID_COLUMNS)
    }

    private fun assertFitsHeight(layout: WidgetLayout, heightDp: Float) {
        val itemHeight = if (layout.mode == Mode.LIST) WidgetLayout.ROW_HEIGHT else WidgetLayout.TILE_HEIGHT
        val gap = if (layout.mode == Mode.LIST) 0f else WidgetLayout.TILE_GAP
        val used = layout.padding * 2 + layout.headerHeight + layout.headerSpacing +
            layout.rows * itemHeight + (layout.rows - 1) * gap
        assertTrue("content needs ${used}dp of $heightDp", used <= heightDp)
    }
}
