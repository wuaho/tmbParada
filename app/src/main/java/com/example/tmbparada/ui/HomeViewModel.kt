package com.example.tmbparada.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tmbparada.domain.Clock
import com.example.tmbparada.domain.WidgetStateStore
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.model.WidgetSettings
import com.example.tmbparada.domain.refresh.LiveRefreshController
import com.example.tmbparada.widget.InstalledWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlacedWidget(
    val widgetId: Int,
    val stop: BusStop?,
    val settings: WidgetSettings,
    val isLive: Boolean,
)

data class HomeUiState(
    val credentialsConfigured: Boolean,
    val canPinWidgets: Boolean,
    val widgets: List<PlacedWidget>,
)

class HomeViewModel(
    store: WidgetStateStore,
    private val installedWidgets: InstalledWidgets,
    private val liveRefresh: LiveRefreshController,
    private val clock: Clock,
    credentialsConfigured: Boolean,
) : ViewModel() {
    private val widgetIds = MutableStateFlow(installedWidgets.ids())

    val uiState: StateFlow<HomeUiState> = combine(widgetIds, store.observeAll()) { ids, states ->
        HomeUiState(
            credentialsConfigured = credentialsConfigured,
            canPinWidgets = installedWidgets.isPinningSupported,
            widgets = ids.map { id ->
                val state = states[id]
                PlacedWidget(
                    widgetId = id,
                    stop = state?.stop,
                    settings = state?.settings ?: WidgetSettings(),
                    isLive = state?.isLive(clock.nowMillis()) == true,
                )
            },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(credentialsConfigured, installedWidgets.isPinningSupported, emptyList()),
    )

    /** Widgets can be added or removed while the app is in the background. */
    fun refreshWidgets() {
        widgetIds.value = installedWidgets.ids()
    }

    fun addWidget() {
        viewModelScope.launch { installedWidgets.requestPin() }
    }

    fun startLive(widgetId: Int) {
        viewModelScope.launch { liveRefresh.startLiveSession(widgetId) }
    }

    fun stopLive(widgetId: Int) {
        viewModelScope.launch { liveRefresh.stopLiveSession(widgetId) }
    }
}
