package com.example.tmbparada.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.tmbparada.R
import com.example.tmbparada.data.BusArrival
import com.example.tmbparada.data.TmbStateRepository
import com.example.tmbparada.data.WidgetState

class TmbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TmbWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repository = TmbStateRepository(context)
        appWidgetIds.forEach { widgetId ->
            TmbRefreshScheduler.cancel(context, widgetId)
            repository.delete(widgetId)
        }
    }
}

class TmbWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = (id as AppWidgetId).appWidgetId
        provideContent {
            TmbWidgetContent(TmbStateRepository(context).read(widgetId))
        }
    }
}

private val widgetBackground = ColorProvider(R.color.widget_background)
private val primaryText = ColorProvider(R.color.widget_primary_text)
private val secondaryText = ColorProvider(R.color.widget_secondary_text)
private val accent = ColorProvider(R.color.widget_accent)
private val actionText = ColorProvider(R.color.widget_action_text)
private val successStatus = ColorProvider(R.color.widget_success_status)
private val errorStatus = ColorProvider(R.color.widget_error_status)
private val warningStatus = ColorProvider(R.color.widget_warning_status)

private val lineBadgeColors = listOf(
    Color(0xFF2667A8),
    Color(0xFF2E7D32),
    Color(0xFF8E24AA),
    Color(0xFFC45100),
    Color(0xFF00695C),
    Color(0xFFAD1457),
)

@androidx.compose.runtime.Composable
private fun TmbWidgetContent(state: WidgetState) {
    val now = System.currentTimeMillis()
    val isStale = WidgetFreshness.isStale(state.lastUpdatedEpochMillis, now)
    val statusText = when {
        state.isRefreshing -> "Updating..."
        state.errorMessage != null -> "Update failed - tap refresh to retry"
        else -> WidgetFreshness.relativeLabel(state.lastUpdatedEpochMillis, now)
    }
    val statusColor = when {
        state.isRefreshing -> accent
        state.errorMessage != null -> errorStatus
        isStale -> warningStatus
        state.lastUpdatedEpochMillis != null -> successStatus
        else -> secondaryText
    }
    val refreshBorderColor = when {
        state.errorMessage != null -> errorStatus
        isStale -> warningStatus
        else -> secondaryText
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "TMB / STOP",
                    style = TextStyle(
                        color = secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = state.stopId,
                    style = TextStyle(
                        color = primaryText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            RefreshButton(
                isRefreshing = state.isRefreshing,
                borderColor = refreshBorderColor,
            )
        }

        if (state.isRefreshing) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                modifier = GlanceModifier.fillMaxWidth(),
                color = accent,
            )
        }

        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Vertical.Top,
        ) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            if (state.arrivals.isEmpty()) {
                Text(
                    text = when {
                        state.isRefreshing -> "Fetching live arrivals..."
                        state.errorMessage != null -> "Could not update arrivals"
                        state.lastUpdatedEpochMillis != null -> "No buses due right now"
                        else -> "Tap refresh to load arrivals"
                    },
                    style = TextStyle(color = secondaryText, fontSize = 12.sp),
                )
            } else {
                state.arrivals.take(MAX_ARRIVALS).forEach { arrival ->
                    ArrivalRow(arrival)
                }
            }
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(statusColor)
                    .cornerRadius(3.dp),
            ) {}
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = statusText,
                style = TextStyle(color = secondaryText, fontSize = 11.sp),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun RefreshButton(
    isRefreshing: Boolean,
    borderColor: ColorProvider,
) {
    Box(
        modifier = GlanceModifier
            .size(48.dp)
            .background(borderColor)
            .cornerRadius(24.dp)
            .clickable(actionRunCallback<RefreshActionCallback>()),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .background(widgetBackground)
                .cornerRadius(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(color = accent)
            } else {
                Text(
                    text = "↻",
                    style = TextStyle(
                        color = borderColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ArrivalRow(arrival: BusArrival) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(48.dp)
                .height(28.dp)
                .background(lineBadgeColor(arrival.line))
                .cornerRadius(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = arrival.line,
                style = TextStyle(
                    color = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Text(
            text = arrival.destination ?: "Upcoming arrival",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = primaryText, fontSize = 13.sp),
        )
        Text(
            text = "${arrival.minutes} min",
            style = TextStyle(
                color = primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private fun lineBadgeColor(line: String): ColorProvider =
    ColorProvider(lineBadgeColors[(line.hashCode() and Int.MAX_VALUE) % lineBadgeColors.size])

private const val MAX_ARRIVALS = 4

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val widgetId = (glanceId as AppWidgetId).appWidgetId
        TmbRefreshScheduler.start(context, widgetId)
        TmbWidget().update(context, glanceId)
    }
}
