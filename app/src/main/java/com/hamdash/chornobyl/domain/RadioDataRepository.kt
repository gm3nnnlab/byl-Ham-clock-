package com.hamdash.chornobyl.domain

import kotlinx.coroutines.flow.Flow

enum class DataOrigin { BUNDLED, REMOTE }

data class SyncState(
    val lastSyncMillis: Long? = null,
    val origin: DataOrigin = DataOrigin.BUNDLED,
    val lastError: SyncProblem? = null,
    val dataUpdated: String? = null,
    val isSyncing: Boolean = false,
)

data class AppStatus(
    val online: Boolean = false,
    val sync: SyncState = SyncState(),
)

sealed interface SyncResult {
    data class Updated(val rejectedItems: Int) : SyncResult
    data object Offline : SyncResult
    data object SkippedMobileData : SyncResult
    data object NotConfigured : SyncResult
    data object Busy : SyncResult
    data class Failed(val problem: SyncProblem) : SyncResult
}

/** Single entry point for radio reference data; UI code never talks to Room or the network directly. */
interface RadioDataRepository {
    val repeaters: Flow<List<Repeater>>
    val talkgroups: Flow<List<Talkgroup>>
    val bands: Flow<List<Band>>
    val digitalModes: Flow<List<DigitalMode>>
    val favoriteIds: Flow<Set<String>>
    val syncState: Flow<SyncState>

    /** Seeds the local database from the bundled catalog if empty, then syncs when due. */
    suspend fun initialize()

    /** Fetches fresh data, validates it, stores valid sections, falls back to local data on any problem. */
    suspend fun refresh(): SyncResult

    suspend fun setFavorite(repeaterId: String, favorite: Boolean)

    /** Drops downloaded data and restores the bundled catalog. Favorites are kept. */
    suspend fun clearCache()
}
