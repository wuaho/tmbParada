package com.example.tmbparada.live

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tmbparada.MainActivity
import com.example.tmbparada.R
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.WidgetState
import com.example.tmbparada.widget.ui.ArrivalRow
import com.example.tmbparada.widget.ui.WidgetPresenter
import com.example.tmbparada.widget.ui.errorText
import com.example.tmbparada.widget.ui.minutesText
import com.example.tmbparada.widget.ui.rowTimesText
import com.example.tmbparada.widget.ui.timeOfDay

/**
 * Builds the live session notification (one per widget, updated in place) and the "time to
 * go" alert. On Android 16 the live notification asks to be promoted to a Live Update, which
 * shows it prominently on the lock screen and as a chip in the status bar.
 */
class LiveNotifications(private val context: Context) {

    init {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(LIVE_CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.live_channel_name))
                .setDescription(context.getString(R.string.live_channel_description))
                .setSound(null, null)
                .setVibrationEnabled(false)
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(ALERT_CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.alert_channel_name))
                .setDescription(context.getString(R.string.alert_channel_description))
                .setVibrationEnabled(true)
                .build(),
        )
    }

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun starting(widgetId: Int): Notification =
        baseLiveBuilder(widgetId)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.widget_placeholder_loading))
            .build()

    fun live(widgetId: Int, state: WidgetState, nowMillis: Long): Notification {
        val stop = state.stop
        val title = stop?.name ?: stop?.let { context.getString(R.string.widget_stop_code, it.code) }
        val snapshot = state.snapshot
        val rows = snapshot?.let { WidgetPresenter.groupArrivals(it.arrivals, nowMillis) }.orEmpty()
            .take(MAX_ROWS)

        val summary = when {
            state.lastError != null -> context.errorText(state.lastError)
            rows.isNotEmpty() -> rows.joinToString("  ·  ") { "${it.line} ${context.minutesText(it.minutes.first())}" }
            snapshot != null -> context.getString(R.string.widget_placeholder_no_buses)
            else -> context.getString(R.string.widget_placeholder_loading)
        }
        val details = rows.joinToString("\n") { row ->
            listOfNotNull(row.line, row.destination, context.rowTimesText(row)).joinToString("  ")
        }

        val builder = baseLiveBuilder(widgetId)
            .setContentTitle(title ?: context.getString(R.string.app_name))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details.ifEmpty { summary }))
            .setSubText(
                state.liveUntilMillis?.let { context.getString(R.string.live_until, context.timeOfDay(it)) },
            )
            .addAction(0, context.getString(R.string.live_action_refresh), action(widgetId, LiveSessionActionReceiver.ACTION_REFRESH))
            .addAction(0, context.getString(R.string.live_action_stop), action(widgetId, LiveSessionActionReceiver.ACTION_STOP))

        // A running clock since the last update shows at a glance that the data is fresh.
        if (snapshot != null) {
            builder.setWhen(snapshot.fetchedAtMillis).setShowWhen(true).setUsesChronometer(true)
        }
        rows.firstOrNull()?.let { builder.extras.putString(EXTRA_SHORT_CRITICAL_TEXT, "${it.line} ${context.minutesText(it.minutes.first())}") }
        return builder.build()
    }

    fun alert(widgetId: Int, stop: BusStop, row: ArrivalRow): Notification =
        NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_bus)
            .setContentTitle(context.getString(R.string.alert_title, row.line, context.minutesText(row.minutes.first())))
            .setContentText(
                listOfNotNull(row.destination, stop.name ?: context.getString(R.string.widget_stop_code, stop.code))
                    .joinToString(" · "),
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(widgetId))
            .setAutoCancel(true)
            .setTimeoutAfter(ALERT_TIMEOUT_MILLIS)
            .build()

    private fun baseLiveBuilder(widgetId: Int): NotificationCompat.Builder =
        NotificationCompat.Builder(context, LIVE_CHANNEL)
            .setSmallIcon(R.drawable.ic_bus)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(widgetId))
            .addExtras(android.os.Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })

    private fun openApp(widgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            widgetId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun action(widgetId: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            widgetId,
            Intent(context, LiveSessionActionReceiver::class.java)
                .setAction(action)
                // A distinct URI per widget keeps the widgets' PendingIntents apart.
                .setData(Uri.parse("tmbparada://live/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val LIVE_CHANNEL = "live_session"
        const val ALERT_CHANNEL = "arrival_alert"
        const val MAX_ROWS = 5
        const val ALERT_TIMEOUT_MILLIS = 3 * 60_000L

        // Notification.EXTRA_REQUEST_PROMOTED_ONGOING and EXTRA_SHORT_CRITICAL_TEXT (API 36),
        // spelled out so the app builds against SDK 35. Older Android versions ignore them.
        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
    }
}
