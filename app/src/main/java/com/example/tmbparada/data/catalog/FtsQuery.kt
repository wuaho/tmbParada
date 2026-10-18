package com.example.tmbparada.data.catalog

import java.text.Normalizer

internal fun normalizeForSearch(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(ACCENT_MARKS_REGEX, "")

/** Turns free text into an FTS prefix query: `"Perú 2"` becomes `"peru* 2*"`. */
internal fun String.toFtsPrefixQuery(): String =
    trim()
        .split(WHITESPACE_REGEX)
        .map { token -> normalizeForSearch(token).filter(Char::isLetterOrDigit).lowercase() }
        .filter(String::isNotEmpty)
        .joinToString(" ") { "$it*" }
        .ifEmpty { NO_MATCH_QUERY }

private val ACCENT_MARKS_REGEX = Regex("\\p{Mn}+")
private val WHITESPACE_REGEX = Regex("\\s+")
private const val NO_MATCH_QUERY = "__no_match__*"
