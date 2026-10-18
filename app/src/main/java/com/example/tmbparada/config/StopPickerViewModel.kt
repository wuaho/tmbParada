package com.example.tmbparada.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tmbparada.domain.StopCatalog
import com.example.tmbparada.domain.WidgetStateStore
import com.example.tmbparada.domain.model.BusStop
import com.example.tmbparada.domain.refresh.LiveRefreshController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SearchResults {
    data object Idle : SearchResults
    data class Loaded(val stops: List<BusStop>) : SearchResults
}

class StopPickerViewModel(
    private val widgetId: Int,
    private val catalog: StopCatalog,
    store: WidgetStateStore,
    private val liveRefresh: LiveRefreshController,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** The stop this widget already shows, when it is being reconfigured. */
    val currentStop: StateFlow<BusStop?> = store.observe(widgetId)
        .map { it.stop }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<SearchResults> = _query
        .debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchResults.Idle)
            } else {
                catalog.search(query).map(SearchResults::Loaded)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SearchResults.Idle)

    fun onQueryChange(query: String) {
        _query.value = query
    }

    suspend fun select(stop: BusStop) {
        liveRefresh.configure(widgetId, stop)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 150L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
