package com.hamdash.chornobyl.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.AppStatus
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.settings.ThemeMode
import com.hamdash.chornobyl.ui.common.toMessage
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
    val message: String? = null,
    val busy: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.settingsRepository
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        repo.settings,
        container.appStatus,
        message,
        busy,
    ) { settings, status, msg, isBusy ->
        SettingsUiState(settings, status, msg, isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(value: ThemeMode) = launch { repo.setThemeMode(value) }
    fun setTimeZone(id: String) = launch { repo.setTimeZone(id) }
    fun setShowUtc(value: Boolean) = launch { repo.setShowUtc(value) }
    fun setDistanceUnit(value: DistanceUnit) = launch { repo.setDistanceUnit(value) }
    fun setShowGps(value: Boolean) = launch { repo.setShowGps(value) }
    fun setAutoSync(value: Boolean) = launch { repo.setAutoSync(value) }
    fun setSyncInterval(hours: Int) = launch { repo.setSyncIntervalHours(hours) }
    fun setAllowMobileData(value: Boolean) = launch { repo.setAllowMobileData(value) }
    fun dismissMessage() = message.update { null }

    fun saveCatalogUrl(raw: String) {
        val url = raw.trim()
        if (url.isNotEmpty() && !url.startsWith("https://")) {
            message.value = "The data source URL must start with https://. Nothing was saved."
            return
        }
        launch {
            repo.setCatalogUrl(url)
            message.value = if (url.isEmpty()) "Data source cleared. The bundled catalog stays in use." else "Data source saved."
        }
    }

    fun saveTileUrl(raw: String) {
        val url = raw.trim()
        val valid = url.startsWith("https://") && "{z}" in url && "{x}" in url && "{y}" in url
        if (url.isNotEmpty() && !valid) {
            message.value = "The tile URL must start with https:// and contain {z}, {x} and {y}. Nothing was saved."
            return
        }
        launch {
            repo.setTileUrl(url)
            message.value = if (url.isEmpty()) "Using the default OpenStreetMap tiles." else "Tile source saved."
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
                message.value = "Cache cleared. Bundled data was restored; favorites and settings were kept."
            } finally {
                busy.value = false
            }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
