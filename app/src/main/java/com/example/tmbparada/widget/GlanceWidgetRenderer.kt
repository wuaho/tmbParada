package com.example.tmbparada.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.tmbparada.domain.refresh.WidgetRenderer

class GlanceWidgetRenderer(private val context: Context) : WidgetRenderer {
    override suspend fun render(widgetId: Int) {
        // Throws for ids that no longer belong to a widget, e.g. one removed mid-refresh.
        val glanceId = runCatching { GlanceAppWidgetManager(context).getGlanceIdBy(widgetId) }
            .getOrNull() ?: return
        TmbWidget().update(context, glanceId)
    }
}
