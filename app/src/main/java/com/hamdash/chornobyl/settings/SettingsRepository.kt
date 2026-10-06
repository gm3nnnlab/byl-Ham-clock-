package com.hamdash.chornobyl.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.TimeZoneOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(private val store: DataStore<Preferences>) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val TIME_ZONE = stringPreferencesKey("time_zone")
        val SHOW_UTC = booleanPreferencesKey("show_utc")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val SHOW_GPS = booleanPreferencesKey("show_gps")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval_hours")
        val MOBILE_DATA = booleanPreferencesKey("allow_mobile_data")
        val CATALOG_URL = stringPreferencesKey("catalog_url")
        val TILE_URL = stringPreferencesKey("tile_url")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            val defaults = AppSettings()
            AppSettings(
                themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME].orEmpty()) }.getOrDefault(defaults.themeMode),
                timeZoneId = p[Keys.TIME_ZONE] ?: defaults.timeZoneId,
                showUtc = p[Keys.SHOW_UTC] ?: defaults.showUtc,
                distanceUnit = runCatching { DistanceUnit.valueOf(p[Keys.DISTANCE_UNIT].orEmpty()) }
                    .getOrDefault(defaults.distanceUnit),
                showGps = p[Keys.SHOW_GPS] ?: defaults.showGps,
                autoSync = p[Keys.AUTO_SYNC] ?: defaults.autoSync,
                syncIntervalHours = p[Keys.SYNC_INTERVAL] ?: defaults.syncIntervalHours,
                allowMobileData = p[Keys.MOBILE_DATA] ?: defaults.allowMobileData,
                catalogUrl = p[Keys.CATALOG_URL] ?: defaults.catalogUrl,
                tileUrl = p[Keys.TILE_URL] ?: defaults.tileUrl,
            )
        }

    suspend fun current(): AppSettings = settings.first()

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.THEME] = value.name }
    suspend fun setTimeZone(id: String) = edit { it[Keys.TIME_ZONE] = if (id in TimeZoneOptions.ids) id else TimeZoneOptions.SYSTEM }
    suspend fun setShowUtc(value: Boolean) = edit { it[Keys.SHOW_UTC] = value }
    suspend fun setDistanceUnit(value: DistanceUnit) = edit { it[Keys.DISTANCE_UNIT] = value.name }
    suspend fun setShowGps(value: Boolean) = edit { it[Keys.SHOW_GPS] = value }
    suspend fun setAutoSync(value: Boolean) = edit { it[Keys.AUTO_SYNC] = value }
    suspend fun setSyncIntervalHours(value: Int) = edit { it[Keys.SYNC_INTERVAL] = value }
    suspend fun setAllowMobileData(value: Boolean) = edit { it[Keys.MOBILE_DATA] = value }
    suspend fun setCatalogUrl(value: String) = edit { it[Keys.CATALOG_URL] = value.trim() }
    suspend fun setTileUrl(value: String) = edit { it[Keys.TILE_URL] = value.trim() }
}
