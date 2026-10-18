package com.example.tmbparada.widget.ui

import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.domain.model.WidgetTheme

/** Render-ready description of a widget, free of Android types so it can be unit tested. */
sealed interface WidgetUiModel {
    data object NotConfigured : WidgetUiModel

    /**
     * No live session: arrivals are hidden, since they would soon be wrong, and the widget
     * offers to start one. Only errors the user must fix (API keys) are shown.
     */
    data class Idle(
        val stopName: String?,
        val stopCode: String,
        val error: RefreshError?,
        val theme: WidgetTheme,
    ) : WidgetUiModel

    /** A live session is running or a request is in flight. */
    data class Active(
        val stopName: String?,
        val stopCode: String,
        val rows: List<ArrivalRow>,
        val placeholder: Placeholder?,
        val status: Status,
        val isRefreshing: Boolean,
        val isStale: Boolean,
        val theme: WidgetTheme,
    ) : WidgetUiModel
}

/** One line and destination with its next arrivals, in whole minutes (0 means arriving). */
data class ArrivalRow(
    val line: String,
    val destination: String?,
    val minutes: List<Int>,
    val isFavorite: Boolean = false,
)

/** Shown instead of rows when there are no arrivals to list. */
enum class Placeholder {
    LOADING,
    NO_BUSES,
    FAILED,
}

sealed interface Status {
    data object Updating : Status
    /** [updatedAtMillis] is the time of the last successful update, if any. */
    data class Live(val minutesLeft: Int, val updatedAtMillis: Long?) : Status
    data class Failed(val error: RefreshError) : Status
}
