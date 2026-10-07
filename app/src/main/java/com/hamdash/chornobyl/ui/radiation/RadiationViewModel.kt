package com.hamdash.chornobyl.ui.radiation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.RadiationSnapshot
import com.hamdash.chornobyl.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RadiationUiState(
    val radiation: RadiationSnapshot = RadiationSnapshot(),
    val settings: AppSettings = AppSettings(),
)

class RadiationViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<RadiationUiState> = combine(
        container.radiationRepository.state,
        container.settingsRepository.settings,
    ) { radiation, settings -> RadiationUiState(radiation, settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadiationUiState())

    init {
        viewModelScope.launch { runCatching { container.radiationRepository.refreshIfStale() } }
    }

    fun refresh() {
        viewModelScope.launch { runCatching { container.radiationRepository.refresh() } }
    }

    fun showOnMap() {
        viewModelScope.launch { container.settingsRepository.setShowRadiation(true) }
    }
}
