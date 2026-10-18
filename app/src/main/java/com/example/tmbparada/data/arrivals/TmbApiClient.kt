package com.example.tmbparada.data.arrivals

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Blocking client for `GET /v1/ibus/stops/{stop}`. Call it from a background dispatcher. */
class TmbApiClient(
    private val httpClient: OkHttpClient,
    private val credentials: TmbCredentials,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
) {
    /** Returns the raw response body, or throws [TmbHttpException] / [java.io.IOException]. */
    fun fetchStopArrivals(stopCode: String): String {
        val url = baseUrl.newBuilder()
            .addPathSegments("v1/ibus/stops")
            .addPathSegment(stopCode)
            .addQueryParameter("app_id", credentials.appId)
            .addQueryParameter("app_key", credentials.appKey)
            .build()

        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw TmbHttpException(response.code)
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.tmb.cat"
    }
}

class TmbHttpException(val code: Int) : RuntimeException("TMB API returned HTTP $code")
