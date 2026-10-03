package com.example.tmbparada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tmbparada.data.TmbConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TmbParadaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SetupScreen()
                }
            }
        }
    }
}

@Composable
private fun SetupScreen() {
    val credentialsConfigured = TmbConfig.appId.isNotBlank() && TmbConfig.appKey.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("TMB Parada", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Widget configurado para la parada ${TmbConfig.defaultStopId}.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            if (credentialsConfigured) {
                "API de TMB configurada. Añade el widget a la pantalla de inicio."
            } else {
                "Faltan TMB_APP_ID y TMB_APP_KEY en local.properties. Consulta README.md."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TmbParadaTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
