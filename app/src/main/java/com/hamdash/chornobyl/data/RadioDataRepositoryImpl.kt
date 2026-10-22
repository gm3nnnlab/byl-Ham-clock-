package com.hamdash.chornobyl.data

import com.hamdash.chornobyl.domain.DataOrigin
import com.hamdash.chornobyl.domain.RadioDataRepository
import com.hamdash.chornobyl.domain.SyncResult
import com.hamdash.chornobyl.domain.SyncState
import com.hamdash.chornobyl.domain.bundledCatalogIsNewer
import com.hamdash.chornobyl.network.ConnectivityMonitor
import com.hamdash.chornobyl.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RadioDataRepositoryImpl(
    private val local: LocalDataSource,
    private val remote: RemoteDataSource,
    private val bundled: BundledCatalogSource,
    private val meta: SyncMetaStore,
    private val connectivity: ConnectivityMonitor,
    private val settings: SettingsRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : RadioDataRepository {

    private val mutex = Mutex()
    private val syncing = MutableStateFlow(false)

    override val repeaters = local.repeaters
    override val talkgroups = local.talkgroups
    override val bands = local.bands
    override val digitalModes = local.digitalModes
    override val favoriteIds = local.favoriteIds

    override val syncState: Flow<SyncState> = combine(meta.meta, syncing) { m, isSyncing ->
        SyncState(
            lastSyncMillis = m.lastSuccessMillis.takeIf { it > 0 },
            origin = m.origin,
            lastError = m.lastError,
            dataUpdated = m.dataUpdated,
            isSyncing = isSyncing,
        )
    }

    override suspend fun initialize() {
        mutex.withLock { seedFromBundle() }
        val s = settings.current()
        if (s.autoSync && now() - meta.current().lastAttemptMillis >= s.syncIntervalHours * HOUR_MS) {
            refresh()
        }
    }

    override suspend fun refresh(): SyncResult {
        if (!mutex.tryLock()) return SyncResult.Busy
        syncing.value = true
        try {
            seedFromBundle()
            val network = connectivity.status()
            if (!network.online) return SyncResult.Offline
            if (network.metered && !settings.current().allowMobileData) return SyncResult.SkippedMobileData

            return when (val result = remote.fetchCatalog()) {
                RemoteResult.NotConfigured -> {
                    meta.recordAttempt(now())
                    SyncResult.NotConfigured
                }
                is RemoteResult.Failure -> {
                    meta.recordFailure(now(), result.problem)
                    SyncResult.Failed(result.problem)
                }
                is RemoteResult.Success -> {
                    local.replaceCatalog(result.catalog)
                    meta.recordSuccess(now(), result.catalog.updated)
                    SyncResult.Updated(result.rejected.size)
                }
            }
        } finally {
            syncing.value = false
            mutex.unlock()
        }
    }

    override suspend fun setFavorite(repeaterId: String, favorite: Boolean) = local.setFavorite(repeaterId, favorite)

    override suspend fun clearCache() {
        mutex.withLock {
            local.clearCatalog()
            meta.reset()
            seedFromBundle()
        }
    }

    /**
     * Fills an empty database from the catalog in the APK, and replaces data that came from an older bundled
     * catalog after an app update. A catalog downloaded from the user's own source is left alone.
     */
    private suspend fun seedFromBundle() {
        val empty = local.isCatalogEmpty()
        val stored = meta.current()
        if (!empty && stored.origin != DataOrigin.BUNDLED) return
        val catalog = bundled.load()
        if (empty || bundledCatalogIsNewer(stored.origin, stored.dataUpdated, catalog.updated)) {
            local.replaceCatalog(catalog)
            meta.recordSeed(catalog.updated)
        }
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
