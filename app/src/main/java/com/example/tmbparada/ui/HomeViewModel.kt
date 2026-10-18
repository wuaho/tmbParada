package com.example.tmbparada.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tmbparada.domain.WidgetStateStore
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.widget.InstalledWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlacedWidget(val widgetId: Int, val stop: BusStop?)

data class HomeUiState(
    val credentialsConfigured: Boolean,
    val canPinWidgets: Boolean,
    val widgets: List<PlacedWidget>,
)

class HomeViewModel(
    store: WidgetStateStore,
    private val installedWidgets: InstalledWidgets,
    credentialsConfigured: Boolean,
) : ViewModel() {
    private val widgetIds = MutableStateFlow(installedWidgets.ids())

    val uiState: StateFlow<HomeUiState> = combine(widgetIds, store.observeAll()) { ids, states ->
        HomeUiState(
            credentialsConfigured = credentialsConfigured,
            canPinWidgets = installedWidgets.isPinningSupported,
            widgets = ids.map { PlacedWidget(it, states[it]?.stop) },
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
}
