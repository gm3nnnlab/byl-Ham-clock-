package com.hamdash.chornobyl.settings

import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.domain.WeatherPlace

enum class ThemeMode(val label: String) { DARK("Dark"), LIGHT("Light") }

data class AppSettings(
    val language: AppLanguage = AppLanguage.fromDevice(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val timeZoneId: String = TimeZoneOptions.SYSTEM,
    val showUtc: Boolean = true,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val showGps: Boolean = true,
    val autoSync: Boolean = true,
    val syncIntervalHours: Int = 12,
    val allowMobileData: Boolean = true,
    val catalogUrl: String = "",
    val tileUrl: String = "",
    val weatherPlace: WeatherPlace = WeatherPlace.DEFAULT,
) {
    companion object {
        val SYNC_INTERVAL_OPTIONS = listOf(1, 6, 12, 24, 72)
    }
}
