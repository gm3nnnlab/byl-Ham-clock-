package com.hamdash.chornobyl.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hamdash.chornobyl.domain.PropagationData
import com.hamdash.chornobyl.domain.PropagationSnapshot
import com.hamdash.chornobyl.network.NoaaParsers
import com.hamdash.chornobyl.network.PropagationSources
import com.hamdash.chornobyl.network.RawApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Space-weather values from NOAA SWPC. A field that cannot be fetched or parsed stays null ("NO DATA");
 * if everything fails the last good snapshot is kept and shown together with its fetch time.
 */
class PropagationRepository(
    private val api: RawApi,
    private val store: DataStore<Preferences>,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("propagation_cache")
    private val mutex = Mutex()
    private val _state = MutableStateFlow(PropagationSnapshot())
    val state: StateFlow<PropagationSnapshot> = _state.asStateFlow()

    init {
        scope.launch {
            val cached = runCatching {
                store.data.first()[key]?.let { json.decodeFromString(PropagationData.serializer(), it) }
            }.getOrNull()
            if (cached != null && _state.value.data == null) _state.value = _state.value.copy(data = cached)
        }
    }

    suspend fun refreshIfStale(maxAgeMillis: Long = STALE_AFTER_MS) {
        val fetchedAt = _state.value.data?.fetchedAtMillis ?: 0L
        if (now() - fetchedAt >= maxAgeMillis) refresh()
    }

    suspend fun refresh() {
        if (!mutex.tryLock()) return
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val (flux, k, flare) = coroutineScope {
                val f = async { download(PropagationSources.SOLAR_FLUX)?.let(NoaaParsers::parseSolarFlux) }
                val kp = async { download(PropagationSources.K_INDEX)?.let(NoaaParsers::parseKIndex) }
                val fl = async { download(PropagationSources.FLARES)?.let(NoaaParsers::parseFlare) }
                Triple(f.await(), kp.await(), fl.await())
            }
            val fresh = PropagationData(
                solarFlux = flux,
                kIndex = k?.kp,
                aIndex = k?.aIndex,
                kIndexTime = k?.time,
                flareClass = flare?.flareClass,
                flareTime = flare?.time,
                fetchedAtMillis = now(),
            )
            if (fresh.hasAnyValue) {
                _state.value = PropagationSnapshot(data = fresh)
                runCatching { store.edit { it[key] = json.encodeToString(PropagationData.serializer(), fresh) } }
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "NOAA data could not be loaded. Check the connection and try again.",
                )
            }
        } finally {
            _state.value = _state.value.copy(loading = false)
            mutex.unlock()
        }
    }

    suspend fun clearCache() {
        runCatching { store.edit { it.remove(key) } }
        _state.value = PropagationSnapshot()
    }

    private suspend fun download(url: String): String? = withContext(Dispatchers.IO) {
        try {
            api.fetch(url).string()
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val STALE_AFTER_MS = 30L * 60 * 1000
    }
}
