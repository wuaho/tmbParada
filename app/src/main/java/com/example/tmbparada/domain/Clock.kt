package com.example.tmbparada.domain

/** Source of wall-clock time, injectable so time-based logic can be tested. */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        val System: Clock = Clock { java.lang.System.currentTimeMillis() }
    }
}
