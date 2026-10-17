package com.example.tmbparada.domain

/** Presentation rules for TMB line names shared by the widget and the app. */
object BusLines {
    private val linePattern = Regex("^([A-Za-z]*)(\\d*)(.*)$")

    /** Orders lines as riders expect: 7, 19, 59, D20, H12, V15. */
    val ordering: Comparator<String> = compareBy<String>(
        { parts(it).prefix.isNotEmpty() },
        { parts(it).prefix.uppercase() },
        { parts(it).number ?: Int.MAX_VALUE },
        { parts(it).rest },
    )

    /**
     * ARGB badge color following the TMB network colors: H lines are blue, V lines green,
     * D lines purple, and conventional lines red.
     */
    fun colorFor(line: String): Long = when (parts(line).prefix.uppercase()) {
        "H" -> HORIZONTAL_BLUE
        "V" -> VERTICAL_GREEN
        "D" -> DIAGONAL_PURPLE
        "X" -> EXPRESS_ORANGE
        "N" -> NIGHT_NAVY
        else -> CONVENTIONAL_RED
    }

    private data class Parts(val prefix: String, val number: Int?, val rest: String)

    private fun parts(line: String): Parts {
        val match = linePattern.matchEntire(line.trim()) ?: return Parts("", null, line)
        val (prefix, digits, rest) = match.destructured
        return Parts(prefix, digits.toIntOrNull(), rest)
    }

    const val HORIZONTAL_BLUE = 0xFF1D5DA8
    const val VERTICAL_GREEN = 0xFF2E8B3E
    const val DIAGONAL_PURPLE = 0xFF7A3F98
    const val EXPRESS_ORANGE = 0xFFD9670A
    const val NIGHT_NAVY = 0xFF27336B
    const val CONVENTIONAL_RED = 0xFFC8231B
}
