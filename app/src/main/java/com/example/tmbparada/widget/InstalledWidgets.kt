package com.example.tmbparada.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

/** Widget instances currently placed on the home screen. */
class InstalledWidgets(private val context: Context) {
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun ids(): List<Int> =
        appWidgetManager.getAppWidgetIds(ComponentName(context, TmbWidgetReceiver::class.java)).toList()

    val isPinningSupported: Boolean
        get() = appWidgetManager.isRequestPinAppWidgetSupported

    /** Asks the launcher to add a widget; the launcher then opens the stop picker. */
    suspend fun requestPin(): Boolean =
        GlanceAppWidgetManager(context).requestPinGlanceAppWidget(TmbWidgetReceiver::class.java)
}
