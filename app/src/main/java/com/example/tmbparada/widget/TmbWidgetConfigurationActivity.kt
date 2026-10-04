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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.AppWidgetId
import androidx.lifecycle.lifecycleScope
import com.example.tmbparada.data.StopSearchResult
import com.example.tmbparada.data.TmbStateRepository
import com.example.tmbparada.data.TmbTransitSearchRepository
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

        setContent {
            TmbWidgetConfigurationScreen(onSave = ::saveConfiguration)
        }
    }

    private fun saveConfiguration(stop: StopSearchResult) {
        repository.save(
            appWidgetId,
            repository.read(appWidgetId).copy(
                stopId = stop.stopCode.toString(),
                lines = stop.lineNames.split(",").map(String::trim).filter(String::isNotBlank),
                arrivals = emptyList(),
                lastUpdatedEpochMillis = null,
                activeUntilEpochMillis = null,
                isRefreshing = false,
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

@androidx.compose.runtime.Composable
private fun TmbWidgetConfigurationScreen(onSave: (StopSearchResult) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StopSearchResult>>(emptyList()) }
    val context = LocalContext.current
    val searchRepository = remember(context) { TmbTransitSearchRepository(context) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
        } else {
            searchRepository.search(query).collect { results = it }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Choose a bus stop", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Search by stop name, stop code, or bus line.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search stops") },
                singleLine = true,
            )
            if (query.isBlank()) {
                Text(
                    "Try: Selva de Mar, 2903, or V29",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (results.isEmpty()) {
                Text("No stops found", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(results, key = { "${it.stopCode}-${it.stopName}" }) { stop ->
                        StopSearchRow(stop, onClick = { onSave(stop) })
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StopSearchRow(stop: StopSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text(stop.stopName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Stop ${stop.stopCode} · ${stop.lineNames}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
