package com.hamdash.chornobyl.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hamdash.chornobyl.domain.DataOrigin
import com.hamdash.chornobyl.domain.SyncProblem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

data class SyncMeta(
    val lastSuccessMillis: Long = 0L,
    val lastAttemptMillis: Long = 0L,
    val origin: DataOrigin = DataOrigin.BUNDLED,
    val lastError: SyncProblem? = null,
    val dataUpdated: String? = null,
)

class SyncMetaStore(private val store: DataStore<Preferences>) {
    private object Keys {
        val LAST_SUCCESS = longPreferencesKey("sync_last_success")
        val LAST_ATTEMPT = longPreferencesKey("sync_last_attempt")
        val ORIGIN = stringPreferencesKey("sync_origin")
        val ERROR = stringPreferencesKey("sync_error")
        val DATA_UPDATED = stringPreferencesKey("sync_data_updated")
    }

    val meta: Flow<SyncMeta> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            SyncMeta(
                lastSuccessMillis = p[Keys.LAST_SUCCESS] ?: 0L,
                lastAttemptMillis = p[Keys.LAST_ATTEMPT] ?: 0L,
                origin = runCatching { DataOrigin.valueOf(p[Keys.ORIGIN].orEmpty()) }.getOrDefault(DataOrigin.BUNDLED),
                lastError = SyncProblem.decode(p[Keys.ERROR]),
                dataUpdated = p[Keys.DATA_UPDATED],
            )
        }

    suspend fun current(): SyncMeta = meta.first()

    suspend fun recordSeed(dataUpdated: String?) = store.edit {
        it[Keys.ORIGIN] = DataOrigin.BUNDLED.name
        it[Keys.LAST_SUCCESS] = 0L
        it.remove(Keys.ERROR)
        if (dataUpdated != null) it[Keys.DATA_UPDATED] = dataUpdated else it.remove(Keys.DATA_UPDATED)
    }

    suspend fun recordSuccess(now: Long, dataUpdated: String?) = store.edit {
        it[Keys.ORIGIN] = DataOrigin.REMOTE.name
        it[Keys.LAST_SUCCESS] = now
        it[Keys.LAST_ATTEMPT] = now
        it.remove(Keys.ERROR)
        if (dataUpdated != null) it[Keys.DATA_UPDATED] = dataUpdated
    }

    suspend fun recordFailure(now: Long, problem: SyncProblem) = store.edit {
        it[Keys.LAST_ATTEMPT] = now
        it[Keys.ERROR] = problem.encode()
    }

    suspend fun recordAttempt(now: Long) = store.edit { it[Keys.LAST_ATTEMPT] = now }

    suspend fun reset() = store.edit {
        it.remove(Keys.LAST_SUCCESS)
        it.remove(Keys.LAST_ATTEMPT)
        it.remove(Keys.ORIGIN)
        it.remove(Keys.ERROR)
        it.remove(Keys.DATA_UPDATED)
    }
}
