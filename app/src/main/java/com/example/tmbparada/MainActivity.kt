package com.example.tmbparada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tmbparada.config.StopPickerActivity
import com.example.tmbparada.ui.HomeScreen
import com.example.tmbparada.ui.HomeViewModel
import com.example.tmbparada.ui.theme.TmbTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val container = appContainer
                HomeViewModel(
                    store = container.widgetStateStore,
                    installedWidgets = container.installedWidgets,
                    credentialsConfigured = container.credentials.isConfigured,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TmbTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onChangeStop = { widgetId -> startActivity(StopPickerActivity.intent(this, widgetId)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWidgets()
    }
}
