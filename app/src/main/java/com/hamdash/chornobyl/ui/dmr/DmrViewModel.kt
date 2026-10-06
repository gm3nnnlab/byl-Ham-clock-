package com.hamdash.chornobyl.ui.dmr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.Talkgroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class DmrUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val repeaters: List<Repeater> = emptyList(),
    val selected: Repeater? = null,
    val talkgroups: List<Talkgroup> = emptyList(),
    val totalTalkgroups: Int = 0,
    val query: String = "",
)

class DmrViewModel(container: AppContainer) : ViewModel() {
    private val selectedId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<DmrUiState> = combine(
        container.repository.repeaters,
        container.repository.talkgroups,
        selectedId,
        query,
    ) { all, talkgroups, id, q ->
        val dmr = all.filter { it.mode == RadioMode.DMR }
        val needle = q.trim()
        DmrUiState(
            loading = false,
            repeaters = dmr,
            selected = dmr.firstOrNull { it.id == id } ?: dmr.firstOrNull(),
            talkgroups = talkgroups.filter {
                needle.isEmpty() ||
                    it.id.toString().contains(needle) ||
                    it.name.contains(needle, ignoreCase = true) ||
                    it.description.contains(needle, ignoreCase = true)
            },
            totalTalkgroups = talkgroups.size,
            query = q,
        )
    }
        .catch {
            emit(
                DmrUiState(
                    loading = false,
                    error = "The DMR data could not be read from the local database. Restart the app, or clear the cache in Settings.",
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DmrUiState())

    fun select(repeater: Repeater) = selectedId.update { repeater.id }
    fun setQuery(value: String) = query.update { value }
}
