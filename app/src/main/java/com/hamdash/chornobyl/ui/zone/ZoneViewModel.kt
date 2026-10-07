package com.hamdash.chornobyl.ui.zone

import androidx.lifecycle.ViewModel
import com.hamdash.chornobyl.AppContainer
import com.hamdash.chornobyl.domain.ZoneContent
import kotlinx.coroutines.flow.StateFlow

class ZoneViewModel(container: AppContainer) : ViewModel() {
    val content: StateFlow<ZoneContent?> = container.zoneRepository.content
}
