package com.hamdash.chornobyl.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.RepeaterFilter
import com.hamdash.chornobyl.domain.filterBy
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapUiState(
    val filter: RepeaterFilter = RepeaterFilter.ALL,
    val repeaters: UiState<List<Repeater>> = UiState.Loading,
    val total: Int = 0,
    val location: LocationState = LocationState.Searching,
    val settings: AppSettings = AppSettings(),
    val selected: Repeater? = null,
    val favoriteIds: Set<String> = emptySet(),
)

class MapViewModel(private val container: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(RepeaterFilter.ALL)
    private val selectedId = MutableStateFlow<String?>(null)

    private val repeatersState = container.repository.repeaters
    private val base = combine(repeatersState, filter, selectedId) { all, f, id ->
        val filtered = all.filterBy(f)
        val content: UiState<List<Repeater>> =
            if (filtered.isEmpty()) UiState.Empty else UiState.Content(filtered)
        MapUiState(
            filter = f,
            repeaters = content,
            total = all.size,
            selected = all.firstOrNull { it.id == id },
        )
    }

    val uiState: StateFlow<MapUiState> = combine(
        base,
        container.locationTracker.state,
        container.settingsRepository.settings,
        container.repository.favoriteIds,
    ) { state, location, settings, favorites ->
        state.copy(location = location, settings = settings, favoriteIds = favorites)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun setFilter(value: RepeaterFilter) {
        filter.value = value
    }

    fun select(repeater: Repeater?) {
        selectedId.value = repeater?.id
    }

    fun enableGps() {
        viewModelScope.launch { container.settingsRepository.setShowGps(true) }
    }

    fun toggleFavorite(repeater: Repeater) {
        viewModelScope.launch {
            container.repository.setFavorite(repeater.id, repeater.id !in uiState.value.favoriteIds)
        }
    }
}
