package com.hamdash.chornobyl.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.Band
import com.hamdash.chornobyl.domain.DigitalMode
import com.hamdash.chornobyl.domain.PropagationSnapshot
import com.hamdash.chornobyl.ui.common.UiState
import com.hamdash.chornobyl.ui.common.asUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReferenceViewModel(container: AppContainer) : ViewModel() {
    val bands: StateFlow<UiState<List<Band>>> = container.repository.bands.asUiState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val digitalModes: StateFlow<UiState<List<DigitalMode>>> = container.repository.digitalModes.asUiState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}

class PropagationViewModel(private val container: AppContainer) : ViewModel() {
    val snapshot: StateFlow<PropagationSnapshot> = container.propagationRepository.state

    fun refresh() {
        viewModelScope.launch { container.propagationRepository.refresh() }
    }
}
