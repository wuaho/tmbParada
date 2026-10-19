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
 *
 * When the widget was added from the app's "Add widget" button, closing this screen goes to
 * the home screen, where the new widget is, instead of back to the app.
 */
class StopPickerActivity : ComponentActivity() {
    private val widgetId: Int by lazy {
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    private val returnHome: Boolean by lazy { intent.getBooleanExtra(EXTRA_RETURN_HOME, false) }

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

    override fun finish() {
        if (returnHome && !isChangingConfigurations) {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        super.finish()
    }

    companion object {
        private const val EXTRA_CUSTOMIZE = "com.example.tmbparada.extra.CUSTOMIZE"
        private const val EXTRA_RETURN_HOME = "com.example.tmbparada.extra.RETURN_HOME"

        /** Opened by the launcher after pinning a widget, which adds EXTRA_APPWIDGET_ID. */
        fun pinnedIntent(context: Context): Intent =
            Intent(context, StopPickerActivity::class.java)
                .putExtra(EXTRA_RETURN_HOME, true)
                .setData(Uri.parse("tmbparada://widget/pinned"))

        /** Opens the stop search, or the widget settings when [customize] is true. */
        fun intent(context: Context, widgetId: Int, customize: Boolean = false): Intent =
            Intent(context, StopPickerActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(EXTRA_CUSTOMIZE, customize)
                // A distinct URI per widget keeps the widgets' PendingIntents from colliding.
                .setData(Uri.parse("tmbparada://widget/$widgetId" + if (customize) "/customize" else ""))
    }
}
