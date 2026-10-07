package com.hamdash.chornobyl.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.GeomagneticState
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.RepeaterFilter
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.settings.ThemeMode
import java.time.ZoneId

@Composable
fun RadioMode.displayName(): String =
    if (this == RadioMode.OTHER) stringResource(R.string.mode_other) else label

@Composable
fun RepeaterFilter.displayName(): String = when (this) {
    RepeaterFilter.ALL -> stringResource(R.string.filter_all)
    RepeaterFilter.OTHER -> stringResource(R.string.mode_other)
    else -> label
}

@Composable
fun DistanceUnit.displayName(): String = when (this) {
    DistanceUnit.KM -> stringResource(R.string.unit_km_name)
    DistanceUnit.MI -> stringResource(R.string.unit_mi_name)
}

@Composable
fun DistanceUnit.displaySuffix(): String = when (this) {
    DistanceUnit.KM -> stringResource(R.string.suffix_km)
    DistanceUnit.MI -> stringResource(R.string.suffix_mi)
}

@Composable
fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
}

@Composable
fun GeomagneticState.displayName(): String = stringResource(
    when (this) {
        GeomagneticState.QUIET -> R.string.geo_quiet
        GeomagneticState.UNSETTLED -> R.string.geo_unsettled
        GeomagneticState.ACTIVE -> R.string.geo_active
        GeomagneticState.STORM_G1 -> R.string.geo_g1
        GeomagneticState.STORM_G2 -> R.string.geo_g2
        GeomagneticState.STORM_G3 -> R.string.geo_g3
        GeomagneticState.STORM_G4 -> R.string.geo_g4
        GeomagneticState.STORM_G5 -> R.string.geo_g5
    },
)

@Composable
fun timeZoneLabel(id: String): String =
    if (id == TimeZoneOptions.SYSTEM) stringResource(R.string.tz_device_default, ZoneId.systemDefault().id) else id

@Composable
fun intervalLabel(hours: Int): String = when (hours) {
    1 -> stringResource(R.string.interval_hour)
    24 -> stringResource(R.string.interval_day)
    72 -> stringResource(R.string.interval_3days)
    else -> stringResource(R.string.interval_hours, hours)
}

@Composable fun noData(): String = stringResource(R.string.no_data)

@Composable fun mhzText(mhz: String): String = stringResource(R.string.value_mhz, mhz)

@Composable
fun offsetText(offsetMHz: Double): String = com.hamdash.chornobyl.domain.formatOffset(
    offsetMHz,
    mhzLabel = stringResource(R.string.unit_mhz),
    simplexLabel = stringResource(R.string.simplex),
)

@Composable
fun distanceText(km: Double, unit: DistanceUnit): String =
    com.hamdash.chornobyl.domain.formatDistance(km, unit, unit.displaySuffix())
