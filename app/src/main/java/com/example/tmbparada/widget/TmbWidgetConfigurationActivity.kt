package com.example.tmbparada.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.AppWidgetId
import com.example.tmbparada.data.TmbStateRepository
import kotlinx.coroutines.launch

class TmbWidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var repository: TmbStateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = TmbStateRepository(this)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        setResult(Activity.RESULT_CANCELED)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val existingStopId = repository.read(appWidgetId).stopId
        setContent {
            ConfigurationScreen(
                initialStopId = existingStopId,
                onSave = ::saveConfiguration,
            )
        }
    }

    private fun saveConfiguration(stopId: String) {
        val normalizedStopId = stopId.trim()
        if (!isValidStopId(normalizedStopId)) return

        repository.save(
            appWidgetId,
            repository.read(appWidgetId).copy(
                stopId = normalizedStopId,
                lines = emptyList(),
                arrivals = emptyList(),
                lastUpdatedEpochMillis = null,
                activeUntilEpochMillis = null,
                errorMessage = null,
            ),
        )

        lifecycleScope.launch {
            TmbWidget().update(this@TmbWidgetConfigurationActivity, AppWidgetId(appWidgetId))
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

internal fun isValidStopId(stopId: String): Boolean =
    stopId.isNotBlank() && stopId.all(Char::isDigit)

@androidx.compose.runtime.Composable
private fun ConfigurationScreen(
    initialStopId: String,
    onSave: (String) -> Unit,
) {
    var stopId by rememberSaveable { mutableStateOf(initialStopId) }
    val isValid = isValidStopId(stopId)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Configure TMB Stop", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Choose the numeric stop ID for this widget. Each widget can use a different stop.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = stopId,
                onValueChange = { value -> if (value.all(Char::isDigit)) stopId = value },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Stop ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = { onSave(stopId) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save widget")
            }
        }
    }
}
