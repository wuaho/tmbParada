package com.example.tmbparada.data.arrivals

/** TMB Open Data application credentials, injected at build time from `local.properties`. */
data class TmbCredentials(val appId: String, val appKey: String) {
    val isConfigured: Boolean
        get() = appId.isNotBlank() && appKey.isNotBlank()
}
