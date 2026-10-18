package com.example.tmbparada.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.tmbparada.R
import com.example.tmbparada.config.StopPickerActivity
import com.example.tmbparada.domain.BusLines
import com.example.tmbparada.domain.model.RefreshError
import com.example.tmbparada.widget.RefreshAction

private val ContentPadding = 12.dp
private val HeaderHeight = 40.dp
private val HeaderSpacing = 8.dp
private val RowHeight = 32.dp

@Composable
fun WidgetContent(widgetId: Int, model: WidgetUiModel) {
    val context = LocalContext.current
    val openStopPicker = actionStartActivity(StopPickerActivity.intent(context, widgetId))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(ContentPadding),
    ) {
        when (model) {
            WidgetUiModel.NotConfigured -> NotConfiguredContent(openStopPicker)
            is WidgetUiModel.Stop -> StopContent(model, openStopPicker)
        }
    }
}

@Composable
private fun NotConfiguredContent(openStopPicker: Action) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().clickable(openStopPicker),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_bus),
            contentDescription = null,
            modifier = GlanceModifier.size(32.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_choose_stop_title),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Text(
            text = context.getString(R.string.widget_choose_stop_body),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
    }
}

@Composable
private fun StopContent(model: WidgetUiModel.Stop, openStopPicker: Action) {
    val context = LocalContext.current
    val visibleRows = rowCapacity(LocalSize.current.height)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(model, openStopPicker)
        Spacer(GlanceModifier.height(HeaderSpacing))
        if (model.rows.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = context.placeholderText(model.placeholder ?: Placeholder.TAP_TO_REFRESH),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                model.rows.take(visibleRows).forEach { row ->
                    ArrivalRowContent(row, isStale = model.isStale)
                }
            }
        }
    }
}

@Composable
private fun Header(model: WidgetUiModel.Stop, openStopPicker: Action) {
    val context = LocalContext.current
    val stopLabel = context.getString(R.string.widget_stop_code, model.stopCode)
    val statusLabel = context.statusText(model.status)

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight().clickable(openStopPicker)) {
            Text(
                text = model.stopName ?: stopLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = if (model.stopName != null) "$stopLabel · $statusLabel" else statusLabel,
                style = TextStyle(
                    color = if (model.status is Status.Failed) {
                        GlanceTheme.colors.error
                    } else {
                        GlanceTheme.colors.onSurfaceVariant
                    },
                    fontSize = 11.sp,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        RefreshButton(isRefreshing = model.isRefreshing)
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .size(HeaderHeight)
            .cornerRadius(HeaderHeight / 2)
            .background(GlanceTheme.colors.secondaryContainer)
            .clickable(actionRunCallback<RefreshAction>()),
        contentAlignment = Alignment.Center,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size(20.dp),
                color = GlanceTheme.colors.onSecondaryContainer,
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            )
        }
    }
}

@Composable
private fun ArrivalRowContent(row: ArrivalRow, isStale: Boolean) {
    val context = LocalContext.current
    val timeColor = if (isStale) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.primary

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(RowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(44.dp)
                .height(24.dp)
                .cornerRadius(6.dp)
                .background(ColorProvider(Color(BusLines.colorFor(row.line)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = row.line,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = row.destination ?: context.getString(R.string.widget_destination_unknown),
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = context.minutesText(row.minutes.first(), approximate = isStale),
            style = TextStyle(color = timeColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        row.minutes.getOrNull(1)?.let { following ->
            Text(
                text = " · " + context.minutesText(following, approximate = isStale),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1,
            )
        }
    }
}

private fun rowCapacity(widgetHeight: Dp): Int {
    val available = widgetHeight - ContentPadding * 2 - HeaderHeight - HeaderSpacing
    return (available.value / RowHeight.value).toInt().coerceAtLeast(1)
}

private fun Context.minutesText(minutes: Int, approximate: Boolean): String {
    val text = if (minutes == 0) getString(R.string.widget_now) else getString(R.string.widget_minutes, minutes)
    return if (approximate) "~$text" else text
}

private fun Context.statusText(status: Status): String = when (status) {
    Status.Updating -> getString(R.string.widget_status_updating)
    Status.NeverUpdated -> getString(R.string.widget_status_never)
    is Status.Live -> getString(R.string.widget_status_live, status.minutesLeft)
    is Status.Failed -> errorText(status.error)
    is Status.Updated -> when {
        status.minutesAgo < 1 -> getString(R.string.widget_status_just_now)
        status.minutesAgo < 60 -> getString(R.string.widget_status_minutes_ago, status.minutesAgo.toInt())
        else -> getString(R.string.widget_status_hours_ago, (status.minutesAgo / 60).toInt())
    }
}

private fun Context.placeholderText(placeholder: Placeholder): String = getString(
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
