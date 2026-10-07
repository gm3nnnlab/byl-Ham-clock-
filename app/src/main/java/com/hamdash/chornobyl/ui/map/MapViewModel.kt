package com.hamdash.chornobyl.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.RadiationCell
import com.hamdash.chornobyl.domain.RadiationSnapshot
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.RepeaterFilter
import com.hamdash.chornobyl.domain.ZonePlace
import com.hamdash.chornobyl.domain.filterBy
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val showRepeaters: Boolean = true,
    val showPlaces: Boolean = true,
    val places: List<ZonePlace> = emptyList(),
    val selectedPlace: ZonePlace? = null,
    val radiation: RadiationSnapshot = RadiationSnapshot(),
    val selectedRadiation: RadiationCell? = null,
)

private data class Layers(val repeaters: Boolean, val places: Boolean, val selectedPlaceId: String?)

class MapViewModel(private val container: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(RepeaterFilter.ALL)
    private val selectedId = MutableStateFlow<String?>(null)
    private val selectedRadiation = MutableStateFlow<RadiationCell?>(null)
    private val layers = MutableStateFlow(Layers(repeaters = true, places = true, selectedPlaceId = null))

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
    }.combine(combine(layers, container.zoneRepository.content) { l, c -> l to c?.places.orEmpty() }) { state, (l, places) ->
        state.copy(
            showRepeaters = l.repeaters,
            showPlaces = l.places,
            places = places,
            selectedPlace = places.firstOrNull { it.id == l.selectedPlaceId },
        )
    }.combine(container.radiationRepository.state) { state, radiation ->
        state.copy(radiation = radiation)
    }.combine(selectedRadiation) { state, cell ->
        state.copy(selectedRadiation = cell)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    init {
        viewModelScope.launch {
            container.settingsRepository.settings
                .map { it.showRadiation }
                .distinctUntilChanged()
                .collect { shown ->
                    if (shown) runCatching { container.radiationRepository.refreshIfStale() }
                }
        }
    }

    fun setFilter(value: RepeaterFilter) {
        filter.value = value
    }

    fun select(repeater: Repeater?) {
        selectedId.value = repeater?.id
    }

    fun toggleRepeaters() {
        layers.value = layers.value.copy(repeaters = !layers.value.repeaters)
    }

    fun togglePlaces() {
        layers.value = layers.value.copy(places = !layers.value.places)
    }

    fun selectPlace(place: ZonePlace?) {
        layers.value = layers.value.copy(selectedPlaceId = place?.id)
    }

    fun toggleRadiation() {
        viewModelScope.launch {
            container.settingsRepository.setShowRadiation(!container.settingsRepository.current().showRadiation)
        }
    }

    fun selectRadiation(cell: RadiationCell?) {
        selectedRadiation.value = cell
    }

    fun refreshRadiation() {
        viewModelScope.launch { runCatching { container.radiationRepository.refresh() } }
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
