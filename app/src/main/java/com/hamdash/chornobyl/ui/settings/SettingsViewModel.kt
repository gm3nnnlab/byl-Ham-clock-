package com.hamdash.chornobyl.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.AppStatus
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.WeatherPlace
import com.hamdash.chornobyl.settings.AppLanguage
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.settings.ThemeMode
import com.hamdash.chornobyl.ui.common.UiText
import com.hamdash.chornobyl.ui.common.toMessage
import com.hamdash.chornobyl.ui.common.uiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val status: AppStatus = AppStatus(),
    val message: UiText? = null,
    val busy: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.settingsRepository
    private val message = MutableStateFlow<UiText?>(null)
    private val busy = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        repo.settings,
        container.appStatus,
        message,
        busy,
    ) { settings, status, msg, isBusy ->
        SettingsUiState(settings, status, msg, isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setLanguage(value: AppLanguage) = launch { repo.setLanguage(value) }
    fun setTheme(value: ThemeMode) = launch { repo.setThemeMode(value) }
    fun setTimeZone(id: String) = launch { repo.setTimeZone(id) }
    fun setShowUtc(value: Boolean) = launch { repo.setShowUtc(value) }
    fun setDistanceUnit(value: DistanceUnit) = launch { repo.setDistanceUnit(value) }
    fun setShowGps(value: Boolean) = launch { repo.setShowGps(value) }
    fun setAutoSync(value: Boolean) = launch { repo.setAutoSync(value) }
    fun setSyncInterval(hours: Int) = launch { repo.setSyncIntervalHours(hours) }
    fun setAllowMobileData(value: Boolean) = launch { repo.setAllowMobileData(value) }
    fun dismissMessage() = message.update { null }

    fun setWeatherPlace(value: WeatherPlace) = launch {
        repo.setWeatherPlace(value)
        if (value == WeatherPlace.MY_LOCATION) repo.setShowGps(true)
        runCatching { container.weatherRepository.refresh() }
    }

    fun saveCatalogUrl(raw: String) {
        val url = raw.trim()
        if (url.isNotEmpty() && !url.startsWith("https://")) {
            message.value = uiText(R.string.msg_url_not_https)
            return
        }
        launch {
            repo.setCatalogUrl(url)
            message.value = uiText(if (url.isEmpty()) R.string.msg_source_cleared else R.string.msg_source_saved)
        }
    }

    fun saveTileUrl(raw: String) {
        val url = raw.trim()
        val valid = url.startsWith("https://") && "{z}" in url && "{x}" in url && "{y}" in url
        if (url.isNotEmpty() && !valid) {
            message.value = uiText(R.string.msg_tile_invalid)
            return
        }
        launch {
            repo.setTileUrl(url)
            message.value = uiText(if (url.isEmpty()) R.string.msg_tile_default else R.string.msg_tile_saved)
        }
    }

    fun syncNow() {
        launch {
            busy.value = true
            try {
                message.value = container.repository.refresh().toMessage()
            } finally {
                busy.value = false
            }
        }
    }

    fun clearCache() {
        launch {
            busy.value = true
            try {
                container.clearCaches()
                message.value = uiText(R.string.msg_cache_cleared)
            } finally {
                busy.value = false
            }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
