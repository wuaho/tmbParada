package com.example.tmbparada.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.AppWidgetId
import androidx.lifecycle.lifecycleScope
import com.example.tmbparada.data.BusLine
import com.example.tmbparada.data.BusStop
import com.example.tmbparada.data.TmbApiClient
import com.example.tmbparada.data.TmbStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        setContent {
            TmbWidgetConfigurationScreen(onSave = ::saveConfiguration)
        }
    }

    private fun saveConfiguration(stop: BusStop) {
        repository.save(
            appWidgetId,
            repository.read(appWidgetId).copy(
                stopId = stop.id,
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

private enum class CatalogStep { LINES, STOPS }

@androidx.compose.runtime.Composable
private fun TmbWidgetConfigurationScreen(onSave: (BusStop) -> Unit) {
    var step by rememberSaveable { mutableStateOf(CatalogStep.LINES) }
    var lines by remember { mutableStateOf<List<BusLine>>(emptyList()) }
    var stops by remember { mutableStateOf<List<BusStop>>(emptyList()) }
    var selectedLine by remember { mutableStateOf<BusLine?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadLines() {
        loading = true
        error = null
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { TmbApiClient().fetchBusLines() } }
                .onSuccess {
                    lines = it
                    query = ""
                }
                .onFailure { error = it.message ?: "Could not load bus lines" }
            loading = false
        }
    }

    fun loadStops(line: BusLine) {
        selectedLine = line
        step = CatalogStep.STOPS
        query = ""
        loading = true
        error = null
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { TmbApiClient().fetchBusStops(line.id) } }
                .onSuccess { stops = it }
                .onFailure { error = it.message ?: "Could not load stops" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadLines()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (step == CatalogStep.LINES) "Choose a bus line" else "Choose a bus stop",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                if (step == CatalogStep.LINES) {
                    "Search for a line first, then choose one of its stops."
                } else {
                    "${selectedLine?.name.orEmpty()} - ${selectedLine?.description.orEmpty()}"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (step == CatalogStep.STOPS) {
                Text(
                    text = "Back to lines",
                    modifier = Modifier.clickable { step = CatalogStep.LINES },
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (step == CatalogStep.LINES) "Search lines" else "Search stops") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            if (loading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = { if (step == CatalogStep.LINES) loadLines() else selectedLine?.let(::loadStops) }) {
                    Text("Retry")
                }
            } else if (step == CatalogStep.LINES) {
                val filteredLines = lines.filter { line ->
                    line.name.contains(query, ignoreCase = true) ||
                        line.description.contains(query, ignoreCase = true) ||
                        line.origin.contains(query, ignoreCase = true) ||
                        line.destination.contains(query, ignoreCase = true)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filteredLines, key = BusLine::id) { line ->
                        CatalogRow(
                            title = line.name,
                            subtitle = "${line.origin} -> ${line.destination}",
                            onClick = { loadStops(line) },
                        )
                    }
                }
            } else {
                val filteredStops = stops.filter { stop ->
                    stop.name.contains(query, ignoreCase = true) || stop.id.contains(query)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filteredStops, key = { "${it.id}-${it.direction}" }) { stop ->
                        CatalogRow(
                            title = stop.name,
                            subtitle = "Stop ${stop.id} - direction ${stop.direction}",
                            onClick = { onSave(stop) },
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CatalogRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
