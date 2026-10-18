package com.example.tmbparada.widget.ui

import android.content.Context
import android.text.format.DateFormat
import com.example.tmbparada.R
import com.example.tmbparada.domain.model.RefreshError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Localized texts shared by the widget and the live notification.

fun Context.minutesText(minutes: Int, approximate: Boolean = false): String {
    val text = if (minutes == 0) getString(R.string.widget_now) else getString(R.string.widget_minutes, minutes)
    return if (approximate) "~$text" else text
}

/** Next bus and the following ones of a row, e.g. "8 min · 20 min". */
fun Context.rowTimesText(row: ArrivalRow): String = row.minutes.joinToString(" · ") { minutesText(it) }

fun Context.statusText(status: Status): String = when (status) {
    Status.Updating -> getString(R.string.widget_status_updating)
    Status.NeverUpdated -> getString(R.string.widget_status_never)
    is Status.Live -> status.updatedAtMillis
        ?.let { getString(R.string.widget_status_live, timeWithSeconds(it)) }
        ?: getString(R.string.widget_status_live_no_data)
    is Status.Failed -> errorText(status.error)
    is Status.Updated -> when {
        status.minutesAgo < 1 -> getString(R.string.widget_status_just_now)
        status.minutesAgo < 60 -> getString(R.string.widget_status_minutes_ago, status.minutesAgo.toInt())
        else -> getString(R.string.widget_status_hours_ago, (status.minutesAgo / 60).toInt())
    }
}

fun Context.placeholderText(placeholder: Placeholder): String = getString(
    when (placeholder) {
        Placeholder.LOADING -> R.string.widget_placeholder_loading
        Placeholder.TAP_TO_REFRESH -> R.string.widget_placeholder_tap_to_refresh
        Placeholder.NO_BUSES -> R.string.widget_placeholder_no_buses
        Placeholder.FAILED -> R.string.widget_placeholder_failed
    },
)

fun Context.errorText(error: RefreshError): String = getString(
    when (error) {
        RefreshError.MISSING_CREDENTIALS -> R.string.error_missing_credentials
        RefreshError.UNAUTHORIZED -> R.string.error_unauthorized
        RefreshError.NETWORK -> R.string.error_network
        RefreshError.SERVER -> R.string.error_server
        RefreshError.INVALID_RESPONSE -> R.string.error_invalid_response
    },
)

/** Clock time with seconds ("9:56:12"), so riders can see each live update land. */
fun Context.timeWithSeconds(epochMillis: Long): String = formatTime(epochMillis, if (is24h()) "Hms" else "hms")

fun Context.timeOfDay(epochMillis: Long): String = formatTime(epochMillis, if (is24h()) "Hm" else "hm")

private fun Context.is24h() = DateFormat.is24HourFormat(this)

private fun formatTime(epochMillis: Long, skeleton: String): String {
    val locale = Locale.getDefault()
    return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(epochMillis))
}
