package com.example.tmbparada.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.height
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.example.tmbparada.R
import com.example.tmbparada.data.BusArrival
import com.example.tmbparada.data.TmbStateRepository
import com.example.tmbparada.data.WidgetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TmbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TmbWidget()
}

class TmbWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TmbWidgetContent(TmbStateRepository(context).read())
        }
    }
}

private val widgetBackground = ColorProvider(R.color.widget_background)
private val primaryText = ColorProvider(R.color.widget_primary_text)
private val secondaryText = ColorProvider(R.color.widget_secondary_text)
private val accent = ColorProvider(R.color.widget_accent)
private val actionText = ColorProvider(R.color.widget_action_text)

@androidx.compose.runtime.Composable
private fun TmbWidgetContent(state: WidgetState) {
    val now = System.currentTimeMillis()
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val updatedAt = state.lastUpdatedEpochMillis?.let { dateFormat.format(Date(it)) }
    val status = when {
        state.isLive(now) -> "En directo..."
        updatedAt != null -> "Actualizado a las $updatedAt"
        else -> "Sin actualizar"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
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
                    text = "TMB / PARADA",
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
            Row(
                modifier = GlanceModifier
                    .clickable(actionRunCallback<RefreshActionCallback>())
                    .background(accent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "RECARGAR",
                    style = TextStyle(
                        color = actionText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (state.arrivals.isEmpty()) {
            Text(
                text = state.lines.takeIf { it.isNotEmpty() }?.let { "Líneas: ${it.joinToString()}" }
                    ?: "Pulsa RECARGAR para consultar los buses",
                style = TextStyle(color = secondaryText, fontSize = 12.sp),
            )
        } else {
            state.arrivals.take(MAX_ARRIVALS).forEach { arrival ->
                ArrivalRow(arrival)
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = state.errorMessage?.let { "Error: $it" } ?: status,
            style = TextStyle(color = if (state.errorMessage == null) secondaryText else accent, fontSize = 10.sp),
        )
    }
}

@androidx.compose.runtime.Composable
private fun ArrivalRow(arrival: BusArrival) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = arrival.line,
            modifier = GlanceModifier.width(48.dp),
            style = TextStyle(color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = "->",
            modifier = GlanceModifier.width(28.dp),
            style = TextStyle(color = secondaryText, fontSize = 12.sp),
        )
        Text(
            text = "${arrival.minutes} min${arrival.destination?.let { "  $it" } ?: ""}",
            style = TextStyle(color = primaryText, fontSize = 14.sp),
        )
    }
}

private const val MAX_ARRIVALS = 4

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        TmbRefreshScheduler.start(context)
        TmbWidget().update(context, glanceId)
    }
}
