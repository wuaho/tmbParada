package com.example.tmbparada.live

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tmbparada.appContainer
import kotlinx.coroutines.launch

/** Handles the Refresh and Stop buttons of the live notification. */
class LiveSessionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val container = context.appContainer
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_REFRESH -> container.liveRefresh.startLiveSession(widgetId)
                    ACTION_STOP -> container.liveRefresh.stopLiveSession(widgetId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.tmbparada.action.REFRESH"
        const val ACTION_STOP = "com.example.tmbparada.action.STOP"
    }
}
