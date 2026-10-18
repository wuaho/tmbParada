package com.example.tmbparada.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tmbparada.appContainer
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.WidgetSettings
import com.example.tmbparada.ui.theme.TmbTheme
import kotlinx.coroutines.launch

/**
 * Widget configuration screen. The launcher opens it when a widget is added; the widget and
 * the app also open it to change the stop of an existing widget.
 */
class StopPickerActivity : ComponentActivity() {
    private val widgetId: Int by lazy {
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    private val viewModel: StopPickerViewModel by viewModels {
        viewModelFactory {
            initializer {
                val container = appContainer
                StopPickerViewModel(
                    widgetId = widgetId,
                    catalog = container.stopCatalog,
                    store = container.widgetStateStore,
                    liveRefresh = container.liveRefresh,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Backing out of the initial configuration must cancel the widget placement.
        setResult(Activity.RESULT_CANCELED)

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val startCustomizing = intent.getBooleanExtra(EXTRA_CUSTOMIZE, false)
        setContent {
            TmbTheme {
                var customizing by rememberSaveable { mutableStateOf(startCustomizing) }
                if (customizing) {
                    WidgetSettingsScreen(
                        viewModel = viewModel,
                        onDone = ::saveSettings,
                        onClose = ::finish,
                    )
                } else {
                    StopPickerScreen(
                        viewModel = viewModel,
                        onStopSelected = { stop -> onStopSelected(stop) { customizing = true } },
                        onCustomize = { customizing = true },
                        onClose = ::finish,
                    )
                }
            }
        }
    }

    /** Saves the stop right away, so the widget is placed even if the user skips customizing. */
    private fun onStopSelected(stop: BusStop, thenCustomize: () -> Unit) {
        lifecycleScope.launch {
            viewModel.select(stop)
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
            )
            thenCustomize()
        }
    }

    private fun saveSettings(settings: WidgetSettings) {
        lifecycleScope.launch {
            viewModel.saveSettings(settings)
            finish()
        }
    }

    companion object {
        private const val EXTRA_CUSTOMIZE = "com.example.tmbparada.extra.CUSTOMIZE"

        /** Opens the stop search, or the widget settings when [customize] is true. */
        fun intent(context: Context, widgetId: Int, customize: Boolean = false): Intent =
            Intent(context, StopPickerActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(EXTRA_CUSTOMIZE, customize)
                // A distinct URI per widget keeps the widgets' PendingIntents from colliding.
                .setData(Uri.parse("tmbparada://widget/$widgetId" + if (customize) "/customize" else ""))
    }
}
