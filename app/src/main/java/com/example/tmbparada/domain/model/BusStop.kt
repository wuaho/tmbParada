package com.example.tmbparada.domain.model

/** A TMB bus stop as selected by the user. [name] is unknown for stops migrated from old versions. */
data class BusStop(
    val code: String,
    val name: String?,
    val lines: List<String> = emptyList(),
)
