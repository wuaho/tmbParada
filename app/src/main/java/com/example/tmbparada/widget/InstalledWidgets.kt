package com.example.tmbparada.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.tmbparada.config.StopPickerActivity

/** Widget instances currently placed on the home screen. */
class InstalledWidgets(private val context: Context) {
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun ids(): List<Int> =
        appWidgetManager.getAppWidgetIds(ComponentName(context, TmbWidgetReceiver::class.java)).toList()

    val isPinningSupported: Boolean
        get() = appWidgetManager.isRequestPinAppWidgetSupported

    /**
     * Asks the launcher to add a widget. Launchers do not open the configuration screen for
     * pinned widgets, so once placed, the launcher opens the stop picker through the success
     * callback; the picker then returns to the home screen.
     */
    suspend fun requestPin(): Boolean {
        // Mutable: the launcher fills in the new widget's EXTRA_APPWIDGET_ID.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val onPinned = PendingIntent.getActivity(
            context,
            0,
            StopPickerActivity.pinnedIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or mutable,
        )
        return GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
            TmbWidgetReceiver::class.java,
            /* preview = */ null,
            /* previewState = */ null,
            /* successCallback = */ onPinned,
        )
    }
}
