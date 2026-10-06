package com.hamdash.chornobyl.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.AppStatus
import com.hamdash.chornobyl.domain.PropagationSnapshot
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.countByMode
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val repeaters: List<Repeater> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val location: LocationState = LocationState.Searching,
    val propagation: PropagationSnapshot = PropagationSnapshot(),
    val status: AppStatus = AppStatus(),
    val selected: Repeater? = null,
    val favoriteIds: Set<String> = emptySet(),
) {
    val counts: Map<RadioMode, Int> get() = repeaters.countByMode()
    val dmrRepeater: Repeater? get() = repeaters.firstOrNull { it.mode == RadioMode.DMR }
}

class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        container.repository.repeaters,
        container.settingsRepository.settings,
        container.locationTracker.state,
        container.propagationRepository.state,
        container.appStatus,
    ) { repeaters, settings, location, propagation, status ->
        DashboardUiState(
            loading = false,
            repeaters = repeaters,
            settings = settings,
            location = location,
            propagation = propagation,
            status = status,
        )
    }
        .combine(selectedId) { state, id -> state.copy(selected = state.repeaters.firstOrNull { it.id == id }) }
        .combine(container.repository.favoriteIds) { state, favorites -> state.copy(favoriteIds = favorites) }
        .catch {
            emit(
                DashboardUiState(
                    loading = false,
                    error = "The dashboard data could not be loaded. Restart the app, or clear the cache in Settings.",
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun select(repeater: Repeater?) {
        selectedId.value = repeater?.id
    }

    fun toggleFavorite(repeater: Repeater) {
        viewModelScope.launch {
            container.repository.setFavorite(repeater.id, repeater.id !in uiState.value.favoriteIds)
        }
    }

    fun refreshPropagation() {
        viewModelScope.launch { container.propagationRepository.refresh() }
    }
}
