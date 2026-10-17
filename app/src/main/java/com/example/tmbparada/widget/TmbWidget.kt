package com.example.tmbparada.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import com.example.tmbparada.appContainer
import com.example.tmbparada.widget.ui.WidgetContent
import com.example.tmbparada.widget.ui.WidgetPresenter
import kotlinx.coroutines.flow.first

class TmbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TmbWidget()
}

/** Renders stored state; it never fetches by itself. See LiveRefreshController for refreshes. */
class TmbWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.appContainer
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val states = container.widgetStateStore.observe(widgetId)
        val initialState = states.first()

        provideContent {
            val state by states.collectAsState(initialState)
            GlanceTheme {
                WidgetContent(
                    widgetId = widgetId,
                    model = WidgetPresenter.present(state, container.clock.nowMillis()),
                )
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        context.appContainer.liveRefresh.onWidgetDeleted(widgetId)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        context.appContainer.liveRefresh.startLiveSession(widgetId)
    }
}
