package com.example.tmbparada.widget.ui

import kotlin.math.floor

/**
 * Chooses how arrivals are arranged for a widget size, in dp.
 *
 * - Wide widgets list one line and destination per row.
 * - Square or narrow widgets show a grid of tiles, with as many columns as fit.
 * - Short widgets use a compact single-line header so at least one row always fits whole.
 */
data class WidgetLayout(
    val mode: Mode,
    val compactHeader: Boolean,
    val columns: Int,
    val rows: Int,
    val listColumns: ListColumns = ListColumns.FULL,
) {
    enum class Mode { LIST, GRID }

    /** What a list row shows besides the badge and the next bus, from widest to narrowest. */
    enum class ListColumns { FULL, NO_DESTINATION, NEXT_ONLY }

    val capacity: Int get() = columns * rows

    val padding: Float get() = if (compactHeader) COMPACT_PADDING else PADDING
    val headerHeight: Float get() = if (compactHeader) COMPACT_HEADER_HEIGHT else HEADER_HEIGHT
    val headerSpacing: Float get() = if (compactHeader) COMPACT_HEADER_SPACING else HEADER_SPACING

    companion object {
        const val PADDING = 12f
        const val COMPACT_PADDING = 10f
        const val HEADER_HEIGHT = 40f
        const val COMPACT_HEADER_HEIGHT = 28f
        const val HEADER_SPACING = 8f
        const val COMPACT_HEADER_SPACING = 4f
        const val ROW_HEIGHT = 34f
        const val TILE_HEIGHT = 56f
        const val TILE_MIN_WIDTH = 120f
        const val TILE_GAP = 6f

        /** Widgets at least this tall relative to their width are laid out as a grid. */
        const val GRID_ASPECT_RATIO = 0.75f

        /** Below this width a list row cannot show destination and both times. */
        const val LIST_MIN_WIDTH = 230f

        /** List row columns: badge plus its spacing, destination, following and next bus. */
        const val BADGE_WIDTH = 52f
        const val DESTINATION_MIN_WIDTH = 72f
        const val FOLLOWING_WIDTH = 56f
        const val NEXT_WIDTH = 64f

        /** Glance containers accept at most 10 children. */
        const val MAX_LIST_ROWS = 8
        const val MAX_GRID_ROWS = 4
        const val MAX_GRID_COLUMNS = 3

        fun forSize(widthDp: Float, heightDp: Float): WidgetLayout {
            val compact = heightDp < PADDING * 2 + HEADER_HEIGHT + HEADER_SPACING + ROW_HEIGHT
            val padding = if (compact) COMPACT_PADDING else PADDING
            val header = if (compact) COMPACT_HEADER_HEIGHT else HEADER_HEIGHT
            val spacing = if (compact) COMPACT_HEADER_SPACING else HEADER_SPACING
            val contentWidth = widthDp - padding * 2
            val contentHeight = heightDp - padding * 2 - header - spacing

            val gridRows = fit(contentHeight, TILE_HEIGHT, TILE_GAP).coerceAtMost(MAX_GRID_ROWS)
            val prefersGrid = heightDp >= widthDp * GRID_ASPECT_RATIO || widthDp < LIST_MIN_WIDTH
            if (prefersGrid && gridRows >= 1) {
                val columns = fit(contentWidth, TILE_MIN_WIDTH, TILE_GAP).coerceIn(1, MAX_GRID_COLUMNS)
                return WidgetLayout(Mode.GRID, compact, columns, gridRows)
            }

            val listRows = fit(contentHeight, ROW_HEIGHT, 0f).coerceIn(1, MAX_LIST_ROWS)
            return WidgetLayout(Mode.LIST, compact, columns = 1, rows = listRows, listColumns = listColumns(contentWidth))
        }

        /** Drops the destination, then the following bus, so the next bus is never clipped. */
        private fun listColumns(contentWidth: Float): ListColumns = when {
            contentWidth >= BADGE_WIDTH + DESTINATION_MIN_WIDTH + FOLLOWING_WIDTH + NEXT_WIDTH -> ListColumns.FULL
            contentWidth >= BADGE_WIDTH + FOLLOWING_WIDTH + NEXT_WIDTH -> ListColumns.NO_DESTINATION
            else -> ListColumns.NEXT_ONLY
        }

        /** How many items of [size] fit in [available], separated by [gap]. */
        private fun fit(available: Float, size: Float, gap: Float): Int =
            floor((available + gap) / (size + gap)).toInt().coerceAtLeast(0)
    }
}
