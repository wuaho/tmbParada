package com.example.tmbparada.data.arrivals

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Blocking client for the TMB arrival endpoints. Call it from a background dispatcher. */
class TmbApiClient(
    private val httpClient: OkHttpClient,
    private val credentials: TmbCredentials,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
) {
    /** `GET /v1/itransit/bus/parades/{stop}`: the next buses of every line, as absolute times. */
    fun fetchITransit(stopCode: String): String = get("v1/itransit/bus/parades", stopCode)

    /** `GET /v1/ibus/stops/{stop}`: only the first bus of every line. */
    fun fetchIBus(stopCode: String): String = get("v1/ibus/stops", stopCode)

    /** Returns the raw response body, or throws [TmbHttpException] / [java.io.IOException]. */
    private fun get(path: String, stopCode: String): String {
        val url = baseUrl.newBuilder()
            .addPathSegments(path)
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
