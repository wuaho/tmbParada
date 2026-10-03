package com.example.tmbparada.data

import com.example.tmbparada.BuildConfig

object TmbConfig {
    const val API_BASE_URL = "https://api.tmb.cat"
    const val LIVE_WINDOW_MILLIS = 10 * 60 * 1000L
    const val REFRESH_INTERVAL_SECONDS = 30L

    val appId: String
        get() = BuildConfig.TMB_APP_ID

    val appKey: String
        get() = BuildConfig.TMB_APP_KEY

    val defaultStopId: String
        get() = BuildConfig.TMB_STOP_ID
}
