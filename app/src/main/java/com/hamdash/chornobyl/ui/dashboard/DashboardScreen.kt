package com.hamdash.chornobyl.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.DEFAULT_LATITUDE
import com.hamdash.chornobyl.domain.FlightConditions
import com.hamdash.chornobyl.domain.flightInputAt
import com.hamdash.chornobyl.domain.DEFAULT_LONGITUDE
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.SunCalculator
import com.hamdash.chornobyl.domain.SunResult
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.domain.formatMHz
import com.hamdash.chornobyl.domain.geomagneticState
import com.hamdash.chornobyl.location.LocationState
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.ErrorView
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.ModeDot
import com.hamdash.chornobyl.ui.common.asString
import com.hamdash.chornobyl.ui.common.displayName
import com.hamdash.chornobyl.ui.common.noData
import com.hamdash.chornobyl.ui.common.talkgroupsText
import com.hamdash.chornobyl.ui.common.RepeaterDetailSheet
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.StatusChip
import com.hamdash.chornobyl.ui.common.ValueTile
import com.hamdash.chornobyl.ui.common.rememberContainer
import com.hamdash.chornobyl.ui.common.syncSummary
import com.hamdash.chornobyl.ui.map.LocationStatusBanner
import com.hamdash.chornobyl.ui.map.OsmMap
import com.hamdash.chornobyl.ui.map.rememberLocationPermissionRequest
import com.hamdash.chornobyl.ui.theme.NumberFont
import com.hamdash.chornobyl.ui.common.VerdictBadge
import com.hamdash.chornobyl.ui.common.RepeaterStatusBadge
import com.hamdash.chornobyl.ui.common.conditionText
import com.hamdash.chornobyl.ui.common.temperatureText
import com.hamdash.chornobyl.ui.common.windText
import com.hamdash.chornobyl.ui.zone.StalkerNote
import com.hamdash.chornobyl.ui.zone.daysSinceDisaster
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun DashboardScreen(
    onOpenMap: () -> Unit,
    onOpenRepeaters: () -> Unit,
    onOpenDmr: () -> Unit,
    onOpenPropagation: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenFpv: () -> Unit,
    onOpenZone: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val container = rememberContainer()
    val requestPermission = rememberLocationPermissionRequest(container.locationTracker)
    val zone = remember(state.settings.timeZoneId) { TimeZoneOptions.resolve(state.settings.timeZoneId) }
    val showGps = state.settings.showGps
    val fix = if (showGps) state.location as? LocationState.Fix else null

    ScreenScaffold(
        title = stringResource(R.string.dash_title),
        actions = { StatusChip(online = state.status.online) },
    ) { padding ->
        val error = state.error
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            error != null -> ErrorView(error.asString(), onRetry = null, modifier = Modifier.padding(padding))
            else -> Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    syncSummary(state.status, zone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ClockCard(zone = zone, showUtc = state.settings.showUtc, fix = fix)
                WeatherCard(state, onOpenWeather = onOpenWeather, onOpenFpv = onOpenFpv)

                SectionCard(
                    title = stringResource(R.string.dash_map),
                    trailing = { TextButton(onClick = onOpenMap) { Text(stringResource(R.string.action_open_map)) } },
                ) {
                    OsmMap(
                        repeaters = state.repeaters,
                        userLocation = fix,
                        tileUrl = state.settings.tileUrl,
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(8.dp)),
                        onRepeaterClick = viewModel::select,
                    )
                    if (showGps && state.location !is LocationState.Fix) {
                        Spacer(Modifier.height(8.dp))
                        LocationStatusBanner(state.location, onRequestPermission = requestPermission)
                    }
                }

                RepeatersCard(state.repeaters.size, state.counts, onClick = onOpenRepeaters)
                DmrCard(state.dmrRepeater, onClick = onOpenDmr)
                PropagationCard(state, onClick = onOpenPropagation)
                ZoneCard(onClick = onOpenZone)
            }
        }
    }

    state.selected?.let { repeater ->
        RepeaterDetailSheet(
            repeater = repeater,
            location = if (showGps) state.location else LocationState.Disabled,
            unit = state.settings.distanceUnit,
            isFavorite = repeater.id in state.favoriteIds,
            onToggleFavorite = { viewModel.toggleFavorite(repeater) },
            onDismiss = { viewModel.select(null) },
        )
    }
}

@Composable
private fun ClockCard(zone: ZoneId, showUtc: Boolean, fix: LocationState.Fix?) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }
    val latitude = fix?.latitude ?: DEFAULT_LATITUDE
    val longitude = fix?.longitude ?: DEFAULT_LONGITUDE
    val localDate = now.atZone(zone).toLocalDate()
    val sun = remember(localDate, latitude, longitude) { SunCalculator.calculate(localDate, latitude, longitude) }
    val utc = ZoneId.of("UTC")

    val locale = LocalConfiguration.current.locales[0]
    val hourSuffix = stringResource(R.string.unit_h)
    val minuteSuffix = stringResource(R.string.unit_min)

    SectionCard(stringResource(R.string.dash_clock)) {
        Row(Modifier.fillMaxWidth()) {
            if (showUtc) {
                ClockValue(stringResource(R.string.dash_utc), TimeFormats.clock(now, utc), Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
            }
            ClockValue(stringResource(R.string.dash_local, zone.id), TimeFormats.clock(now, zone), Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text(TimeFormats.date(now, zone, locale), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        when (sun) {
            is SunResult.Normal -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueTile(stringResource(R.string.dash_sunrise), TimeFormats.hourMinute(sun.sunrise, zone), Modifier.weight(1f))
                ValueTile(stringResource(R.string.dash_sunset), TimeFormats.hourMinute(sun.sunset, zone), Modifier.weight(1f))
                ValueTile(
                    stringResource(R.string.dash_daylight),
                    TimeFormats.duration(sun.dayLength, hourSuffix, minuteSuffix),
                    Modifier.weight(1f),
                )
            }
            SunResult.AlwaysUp -> Text(stringResource(R.string.dash_sun_up_always))
            SunResult.AlwaysDown -> Text(stringResource(R.string.dash_sun_down_always))
        }
        Text(
            stringResource(if (fix != null) R.string.dash_sun_for_you else R.string.dash_sun_default),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ClockValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(
            value,
            fontFamily = NumberFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun RepeatersCard(total: Int, counts: Map<RadioMode, Int>, onClick: () -> Unit) {
    SectionCard(
        title = stringResource(R.string.dash_repeaters),
        onClick = onClick,
        trailing = { Text(stringResource(R.string.dash_found, total), style = MaterialTheme.typography.labelMedium) },
    ) {
        if (total == 0) {
            Text(stringResource(R.string.dash_no_repeaters), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            RadioMode.entries.filter { (counts[it] ?: 0) > 0 }.forEach { mode ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    ModeDot(mode)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${mode.displayName()} - ${counts[mode]}",
                        fontFamily = NumberFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DmrCard(repeater: Repeater?, onClick: () -> Unit) {
    SectionCard(title = stringResource(R.string.dash_dmr), onClick = onClick) {
        if (repeater == null) {
            Text(stringResource(R.string.dash_no_dmr), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        RepeaterStatusBadge(repeater.status, Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile(stringResource(R.string.dash_band_mhz, repeater.bandName ?: "RX"), formatMHz(repeater.rxMHz), Modifier.weight(2f))
            ValueTile(stringResource(R.string.dash_mode), repeater.mode.displayName(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile(
                stringResource(R.string.dash_color_code),
                repeater.colorCode?.let { stringResource(R.string.value_cc, it) } ?: noData(),
                Modifier.weight(1f),
            )
            ValueTile(
                stringResource(R.string.dash_slot),
                repeater.timeslot?.let { stringResource(R.string.value_ts, it) } ?: noData(),
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(
                R.string.dash_talkgroups_line,
                talkgroupsText(repeater.talkgroupIds) ?: noData(),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(R.string.dash_callsign_source, repeater.callsign, repeater.source),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PropagationCard(state: DashboardUiState, onClick: () -> Unit) {
    val data = state.propagation.data
    SectionCard(
        title = stringResource(R.string.dash_propagation),
        onClick = onClick,
        trailing = {
            if (state.propagation.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile(stringResource(R.string.dash_solar_flux), data?.solarFlux?.toString() ?: noData(), Modifier.weight(1f))
            ValueTile(
                stringResource(R.string.dash_k_index),
                data?.kIndex?.let { String.format(Locale.US, "%.1f", it) } ?: noData(),
                Modifier.weight(1f),
            )
            ValueTile(stringResource(R.string.dash_a_index), data?.aIndex?.toString() ?: noData(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.dash_geomagnetic_line, geomagneticState(data?.kIndex)?.displayName() ?: noData()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(stringResource(R.string.dash_flare_line, data?.flareClass ?: noData()), style = MaterialTheme.typography.bodyMedium)
        val kp = data?.kIndex
        if (kp != null && kp >= FlightConditions.KP_CAUTION) {
            StalkerNote(stringResource(R.string.stalker_emission), Modifier.padding(top = 8.dp))
        }
        if (data != null && data.fetchedAtMillis > 0) {
            Text(
                stringResource(
                    R.string.dash_fetched,
                    TimeFormats.dateTime(Instant.ofEpochMilli(data.fetchedAtMillis), ZoneId.of("UTC")),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun WeatherCard(state: DashboardUiState, onOpenWeather: () -> Unit, onOpenFpv: () -> Unit) {
    val forecast = state.weather.forecast
    val nowEpoch = Instant.now().epochSecond
    val current = forecast?.current?.takeIf { nowEpoch - it.timeEpoch < 2 * 3600 }
    val verdict = forecast?.flightInputAt(nowEpoch, state.propagation.data?.kIndex)?.let(FlightConditions::assess)?.overall
    SectionCard(
        title = stringResource(R.string.dash_weather, state.settings.weatherPlace.displayName()),
        onClick = onOpenWeather,
        trailing = {
            if (state.weather.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile(stringResource(R.string.wx_temp), temperatureText(current?.temperatureC), Modifier.weight(1f))
            ValueTile(stringResource(R.string.wx_wind), windText(current?.windSpeedMs), Modifier.weight(1f))
            ValueTile(stringResource(R.string.wx_gusts), windText(current?.windGustsMs), Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text(conditionText(current?.weatherCode), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dash_fpv), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenFpv) { VerdictBadge(verdict) }
        }
    }
}

@Composable
private fun ZoneCard(onClick: () -> Unit) {
    SectionCard(title = stringResource(R.string.dash_zone), onClick = onClick) {
        Text(
            stringResource(R.string.zone_since_days, daysSinceDisaster()),
            fontFamily = NumberFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Text(
            stringResource(R.string.dash_zone_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
