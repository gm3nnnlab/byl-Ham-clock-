package com.hamdash.chornobyl.data

import android.content.Context
import com.hamdash.chornobyl.domain.ZoneContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** History, landmarks and game notes shipped inside the APK (assets/zone.json); works fully offline. */
class ZoneRepository(private val context: Context, scope: CoroutineScope) {
    private val _content = MutableStateFlow<ZoneContent?>(null)
    val content: StateFlow<ZoneContent?> = _content.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            _content.value = runCatching {
                ZoneParser.parse(context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() })
            }.getOrDefault(ZoneContent())
        }
    }

    private companion object {
        const val ASSET_NAME = "zone.json"
    }
}
