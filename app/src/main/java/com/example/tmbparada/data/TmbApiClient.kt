package com.example.tmbparada.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl

class TmbApiClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    fun fetchArrivals(stopId: String): TmbStopResponse {
        require(TmbConfig.appId.isNotBlank() && TmbConfig.appKey.isNotBlank()) {
            "TMB_APP_ID and TMB_APP_KEY are not configured"
        }

        val url = TmbConfig.API_BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("v1")
            .addPathSegment("ibus")
            .addPathSegment("stops")
            .addPathSegment(stopId)
            .addQueryParameter("app_id", TmbConfig.appId)
            .addQueryParameter("app_key", TmbConfig.appKey)
            .build()

        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw TmbApiException("TMB API returned HTTP ${response.code}")
            }
            return TmbResponseParser.parse(body, stopId)
        }
    }
}

class TmbApiException(message: String) : RuntimeException(message)
