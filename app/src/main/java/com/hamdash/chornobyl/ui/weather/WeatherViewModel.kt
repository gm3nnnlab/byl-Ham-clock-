package com.hamdash.chornobyl.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.WeatherSnapshot
import com.hamdash.chornobyl.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeatherUiState(
    val weather: WeatherSnapshot = WeatherSnapshot(),
    val kp: Double? = null,
    val settings: AppSettings = AppSettings(),
)

class WeatherViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<WeatherUiState> = combine(
        container.weatherRepository.state,
        container.propagationRepository.state,
        container.settingsRepository.settings,
    ) { weather, propagation, settings ->
        WeatherUiState(weather, propagation.data?.kIndex, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeatherUiState())

    init {
        viewModelScope.launch { runCatching { container.weatherRepository.refreshIfStale() } }
        viewModelScope.launch { runCatching { container.propagationRepository.refreshIfStale() } }
    }

    fun refresh() {
        viewModelScope.launch { runCatching { container.weatherRepository.refresh() } }
        viewModelScope.launch { runCatching { container.propagationRepository.refresh() } }
    }
}
