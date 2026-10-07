package com.hamdash.chornobyl.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hamdash.chornobyl.domain.RADIATION_WINDOW_DAYS
import com.hamdash.chornobyl.domain.RadiationData
import com.hamdash.chornobyl.domain.RadiationProblem
import com.hamdash.chornobyl.domain.RadiationReading
import com.hamdash.chornobyl.domain.RadiationSnapshot
import com.hamdash.chornobyl.domain.ZONE_CENTER_LAT
import com.hamdash.chornobyl.domain.ZONE_CENTER_LON
import com.hamdash.chornobyl.domain.aggregateRadiation
import com.hamdash.chornobyl.network.ConnectivityMonitor
import com.hamdash.chornobyl.network.RawApi
import com.hamdash.chornobyl.network.Safecast
import com.hamdash.chornobyl.network.SafecastParser
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
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset

/**
 * Dose-rate readings near the Exclusion Zone from Safecast open data. The request always uses the fixed
 * coordinates of the plant, never the user's position. The last good result is cached and shown with its dates;
 * when nothing usable arrives the screen says so instead of showing guessed values.
 */
class RadiationRepository(
    private val api: RawApi,
    private val store: DataStore<Preferences>,
    private val settings: SettingsRepository,
    private val connectivity: ConnectivityMonitor,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("radiation_cache")
    private val mutex = Mutex()
    private val _state = MutableStateFlow(RadiationSnapshot())
    val state: StateFlow<RadiationSnapshot> = _state.asStateFlow()

    init {
        scope.launch {
            val cached = runCatching {
                store.data.first()[key]?.let { json.decodeFromString(RadiationData.serializer(), it) }
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
        try {
            _state.value = _state.value.copy(loading = true, problem = null, received = 0, skipped = 0)
            val network = connectivity.status()
            when {
                !network.online -> return fail(RadiationProblem.OFFLINE)
                network.metered && !settings.current().allowMobileData -> return fail(RadiationProblem.METERED_BLOCKED)
            }

            val nowMillis = now()
            val after = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC).toLocalDate()
                .minusDays(RADIATION_WINDOW_DAYS.toLong())
            val readings = ArrayList<RadiationReading>()
            var received = 0
            var skipped = 0
            for (page in 1..Safecast.MAX_PAGES) {
                val body = withContext(Dispatchers.IO) {
                    try {
                        val response = api.fetch(Safecast.measurementsUrl(ZONE_CENTER_LAT, ZONE_CENTER_LON, after, page))
                        if (response.contentLength() > MAX_BYTES) null else response.string()
                    } catch (e: IOException) {
                        null
                    } catch (e: retrofit2.HttpException) {
                        null
                    }
                } ?: return fail(RadiationProblem.UNREACHABLE, received, skipped)
                val parsed = SafecastParser.parse(body, nowMillis / 1000)
                    ?: return fail(RadiationProblem.UNREADABLE, received, skipped)
                if (parsed.total == 0) break
                received += parsed.total
                skipped += parsed.skipped
                readings += parsed.readings
            }

            when {
                received == 0 -> fail(RadiationProblem.EMPTY)
                readings.isEmpty() -> fail(RadiationProblem.NO_USABLE, received, skipped)
                else -> {
                    val fresh = RadiationData(aggregateRadiation(readings), readings.size, skipped, nowMillis)
                    _state.value = RadiationSnapshot(data = fresh, received = received, skipped = skipped)
                    runCatching { store.edit { it[key] = json.encodeToString(RadiationData.serializer(), fresh) } }
                }
            }
        } finally {
            _state.value = _state.value.copy(loading = false)
            mutex.unlock()
        }
    }

    private fun fail(problem: RadiationProblem, received: Int = 0, skipped: Int = 0) {
        _state.value = _state.value.copy(loading = false, problem = problem, received = received, skipped = skipped)
    }

    suspend fun clearCache() {
        runCatching { store.edit { it.remove(key) } }
        _state.value = RadiationSnapshot()
    }

    private companion object {
        const val STALE_AFTER_MS = 6L * 60 * 60 * 1000
        const val MAX_BYTES = 5L * 1024 * 1024
    }
}
