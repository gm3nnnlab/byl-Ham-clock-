package com.hamdash.chornobyl.ui.repeaters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.AppStatus
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.RepeaterFilter
import com.hamdash.chornobyl.domain.filterBy
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.settings.AppSettings
import com.hamdash.chornobyl.ui.common.UiState
import com.hamdash.chornobyl.ui.common.UiText
import com.hamdash.chornobyl.ui.common.uiText
import com.hamdash.chornobyl.ui.common.toMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RepeatersUiState(
    val filter: RepeaterFilter = RepeaterFilter.ALL,
    val query: String = "",
    val content: UiState<List<Repeater>> = UiState.Loading,
    val total: Int = 0,
    val location: LocationState = LocationState.Searching,
    val settings: AppSettings = AppSettings(),
    val status: AppStatus = AppStatus(),
    val selected: Repeater? = null,
    val favoriteIds: Set<String> = emptySet(),
    val message: UiText? = null,
)

class RepeatersViewModel(private val container: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(RepeaterFilter.ALL)
    private val query = MutableStateFlow("")
    private val selectedId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<UiText?>(null)

    private val core = combine(container.repository.repeaters, filter, query, selectedId) { all, f, q, id ->
        val filtered = all.filterBy(f, q)
        val content: UiState<List<Repeater>> =
            if (filtered.isEmpty()) UiState.Empty else UiState.Content(filtered)
        RepeatersUiState(
            filter = f,
            query = q,
            content = content,
            total = all.size,
            selected = all.firstOrNull { it.id == id },
        )
    }

    private val environment = combine(
        container.locationTracker.state,
        container.settingsRepository.settings,
        container.appStatus,
        container.repository.favoriteIds,
    ) { location, settings, status, favorites ->
        RepeatersUiState(location = location, settings = settings, status = status, favoriteIds = favorites)
    }

    val uiState: StateFlow<RepeatersUiState> = combine(core, environment, message) { c, e, m ->
        c.copy(
            location = e.location,
            settings = e.settings,
            status = e.status,
            favoriteIds = e.favoriteIds,
            message = m,
        )
    }
        .catch {
            emit(
                RepeatersUiState(
                    content = UiState.Error(uiText(R.string.err_repeaters_read)),
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RepeatersUiState())

    fun setFilter(value: RepeaterFilter) = filter.update { value }
    fun setQuery(value: String) = query.update { value }
    fun select(repeater: Repeater?) = selectedId.update { repeater?.id }
    fun dismissMessage() = message.update { null }

    fun toggleFavorite(repeater: Repeater) {
        viewModelScope.launch {
            container.repository.setFavorite(repeater.id, repeater.id !in uiState.value.favoriteIds)
        }
    }

    fun syncNow() {
        viewModelScope.launch { message.value = container.repository.refresh().toMessage() }
    }
}
