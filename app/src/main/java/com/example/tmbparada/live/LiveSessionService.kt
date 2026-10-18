package com.example.tmbparada.live

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.tmbparada.appContainer
import com.example.tmbparada.domain.model.WidgetState
import com.example.tmbparada.widget.ui.WidgetPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Foreground service that exists while at least one widget has a live session. It keeps the
 * refresh cadence reliable and shows each session as an ongoing notification, visible on the
 * lock screen, plus a one-time alert when a bus is about to arrive.
 *
 * The sessions themselves live in the widget state store; this service only mirrors them.
 */
class LiveSessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var foregroundWidgetId: Int? = null
    private val shownWidgetIds = mutableSetOf<Int>()
    private val alertedRows = mutableMapOf<Int, MutableSet<String>>()

    private val notifications by lazy { LiveNotifications(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // startForegroundService() requires this call within a few seconds.
        if (foregroundWidgetId == null) {
            enterForeground(widgetId, notifications.starting(widgetId))
        }
        if (observeJob == null) {
            observeJob = scope.launch { observeSessions() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun observeSessions() {
        val container = appContainer
        // The ticker ends sessions whose deadline passed without a final write, e.g. after a crash.
        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(TICK_MILLIS)
            }
        }
        container.widgetStateStore.observeAll()
            .combine(ticker) { states, _ -> states }
            .collect { states -> render(states, container.clock.nowMillis()) }
    }

    private fun render(states: Map<Int, WidgetState>, nowMillis: Long) {
        // Sessions past their deadline whose final step never ran (e.g. the process was killed)
        // are closed here, so their widgets switch to the idle view instead of keeping old times.
        states.filter { (_, state) -> state.liveUntilMillis != null && !state.isLive(nowMillis) }
            .keys
            .forEach { widgetId -> scope.launch { appContainer.liveRefresh.stopLiveSession(widgetId) } }

        val live = states.filter { (_, state) -> state.stop != null && state.isLive(nowMillis) }

        if (live.isEmpty()) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundWidgetId = null
            cancelEnded(emptySet())
            stopSelf()
            return
        }

        live.forEach { (widgetId, state) ->
            val notification = notifications.live(widgetId, state, nowMillis)
            if (foregroundWidgetId !in live.keys) {
                // Move the foreground role before cancelling the session that held it.
                enterForeground(widgetId, notification)
            } else {
                post(widgetId, notification)
            }
            shownWidgetIds += widgetId
            postAlerts(widgetId, state, nowMillis)
        }
        cancelEnded(live.keys)
    }

    private fun cancelEnded(liveWidgetIds: Set<Int>) {
        (shownWidgetIds - liveWidgetIds).forEach { widgetId ->
            NotificationManagerCompat.from(this).cancel(widgetId)
            alertedRows.remove(widgetId)
        }
        shownWidgetIds.retainAll(liveWidgetIds)
    }

    private fun postAlerts(widgetId: Int, state: WidgetState, nowMillis: Long) {
        val stop = state.stop ?: return
        val favoriteLine = state.settings.favoriteLine
        val rows = state.snapshot?.let { WidgetPresenter.groupArrivals(it.arrivals, nowMillis, favoriteLine) }.orEmpty()
        val alerted = alertedRows.getOrPut(widgetId) { mutableSetOf() }
        ArrivalAlerts.due(rows, alerted, favoriteLine).forEach { row ->
            alerted += ArrivalAlerts.key(row)
            post(widgetId, notifications.alert(widgetId, stop, row), tag = ArrivalAlerts.key(row))
        }
    }

    private fun enterForeground(widgetId: Int, notification: Notification) {
        val id = widgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID } ?: STARTING_NOTIFICATION_ID
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, id, notification, type)
        foregroundWidgetId = widgetId
    }

    @SuppressLint("MissingPermission") // Checked by canPostNotifications().
    private fun post(widgetId: Int, notification: Notification, tag: String? = null) {
        if (!notifications.canPostNotifications()) return
        NotificationManagerCompat.from(this).notify(tag, widgetId, notification)
    }

    companion object {
        private const val TAG = "LiveSessionService"
        private const val TICK_MILLIS = 30_000L
        private const val STARTING_NOTIFICATION_ID = Int.MAX_VALUE

        fun start(context: Context, widgetId: Int) {
            val intent = Intent(context, LiveSessionService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (error: IllegalStateException) {
                // Android refuses foreground services started from the background outside the
                // allowed cases. The session still runs; it is just not kept alive.
                Log.w(TAG, "Could not start the live session service", error)
            }
        }
    }
}
