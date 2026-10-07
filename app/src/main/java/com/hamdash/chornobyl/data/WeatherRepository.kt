package com.hamdash.chornobyl.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hamdash.chornobyl.domain.WeatherForecast
import com.hamdash.chornobyl.domain.WeatherPlace
import com.hamdash.chornobyl.domain.WeatherSnapshot
import com.hamdash.chornobyl.domain.roundForPrivacy
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.location.LocationTracker
import com.hamdash.chornobyl.network.ConnectivityMonitor
import com.hamdash.chornobyl.network.OpenMeteo
import com.hamdash.chornobyl.network.OpenMeteoParser
import com.hamdash.chornobyl.network.RawApi
import com.hamdash.chornobyl.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Forecast from Open-Meteo for the place chosen in Settings. The last good forecast is cached and shown
 * offline with its fetch time; nothing is invented when a value is missing.
 * The user's own position is sent only when they pick "My location", and then rounded to about 10 km.
 */
class WeatherRepository(
    private val api: RawApi,
    private val store: DataStore<Preferences>,
    private val settings: SettingsRepository,
    private val location: LocationTracker,
    private val connectivity: ConnectivityMonitor,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("weather_cache")
    private val mutex = Mutex()
    private val _state = MutableStateFlow(WeatherSnapshot())
    val state: StateFlow<WeatherSnapshot> = _state.asStateFlow()

    init {
        scope.launch {
            val cached = runCatching {
                store.data.first()[key]?.let { json.decodeFromString(WeatherForecast.serializer(), it) }
            }.getOrNull()
            if (cached != null && _state.value.forecast == null) _state.value = _state.value.copy(forecast = cached)
        }
    }

    private data class Target(val placeId: String, val latitude: Double, val longitude: Double, val fallback: Boolean)

    private suspend fun target(): Target {
        val place = settings.current().weatherPlace
        if (place != WeatherPlace.MY_LOCATION) return Target(place.name, place.latitude, place.longitude, false)
        val fix = location.state.value as? LocationState.Fix
            ?: return WeatherPlace.DEFAULT.let { Target(it.name, it.latitude, it.longitude, true) }
        return Target(place.name, roundForPrivacy(fix.latitude), roundForPrivacy(fix.longitude), false)
    }

    suspend fun refreshIfStale(maxAgeMillis: Long = STALE_AFTER_MS) {
        val t = target()
        val cached = _state.value.forecast
        val samePlace = cached != null && cached.placeId == t.placeId &&
            cached.latitude == t.latitude && cached.longitude == t.longitude
        if (!samePlace || now() - cached!!.fetchedAtMillis >= maxAgeMillis) refresh()
    }

    suspend fun refresh() {
        if (!mutex.tryLock()) return
        try {
            val t = target()
            _state.value = _state.value.copy(loading = true, error = false, usedFallbackPlace = t.fallback)
            val network = connectivity.status()
            if (!network.online) {
                _state.value = _state.value.copy(loading = false)
                return
            }
            if (network.metered && !settings.current().allowMobileData) {
                _state.value = _state.value.copy(loading = false)
                return
            }
            val body = withContext(Dispatchers.IO) {
                runCatching { api.fetch(OpenMeteo.forecastUrl(t.latitude, t.longitude)).string() }.getOrNull()
            }
            val fresh = body?.let { OpenMeteoParser.parse(it, t.placeId, t.latitude, t.longitude, now()) }
            if (fresh != null) {
                _state.value = WeatherSnapshot(forecast = fresh, usedFallbackPlace = t.fallback)
                runCatching { store.edit { it[key] = json.encodeToString(WeatherForecast.serializer(), fresh) } }
            } else {
                _state.value = _state.value.copy(loading = false, error = true)
            }
        } finally {
            _state.value = _state.value.copy(loading = false)
            mutex.unlock()
        }
    }

    suspend fun clearCache() {
        runCatching { store.edit { it.remove(key) } }
        _state.value = WeatherSnapshot()
    }

    private companion object {
        const val STALE_AFTER_MS = 30L * 60 * 1000
    }
}
