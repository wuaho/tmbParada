package com.example.tmbparada.widget.ui

import com.example.tmbparada.domain.model.RefreshError

/** Render-ready description of a widget, free of Android types so it can be unit tested. */
sealed interface WidgetUiModel {
    data object NotConfigured : WidgetUiModel

    data class Stop(
        val stopName: String?,
        val stopCode: String,
        val rows: List<ArrivalRow>,
        val placeholder: Placeholder?,
        val status: Status,
        val isRefreshing: Boolean,
        val isStale: Boolean,
    ) : WidgetUiModel
}

/** One line and destination with its next arrivals, in whole minutes (0 means "now"). */
data class ArrivalRow(
    val line: String,
    val destination: String?,
    val minutes: List<Int>,
)

/** Shown instead of rows when there are no arrivals to list. */
enum class Placeholder {
    LOADING,
    TAP_TO_REFRESH,
    NO_BUSES,
    FAILED,
}

sealed interface Status {
    data object Updating : Status
    data object NeverUpdated : Status
    data class Live(val minutesLeft: Int) : Status
    data class Updated(val minutesAgo: Long) : Status
    data class Failed(val error: RefreshError) : Status
}
