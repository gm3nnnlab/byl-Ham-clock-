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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.domain.DEFAULT_LATITUDE
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
import com.hamdash.chornobyl.ui.common.NO_DATA
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
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val container = rememberContainer()
    val requestPermission = rememberLocationPermissionRequest(container.locationTracker)
    val zone = remember(state.settings.timeZoneId) { TimeZoneOptions.resolve(state.settings.timeZoneId) }
    val showGps = state.settings.showGps
    val fix = if (showGps) state.location as? LocationState.Fix else null

    ScreenScaffold(
        title = "CHORNOBYL HAMDASH",
        actions = { StatusChip(online = state.status.online) },
    ) { padding ->
        val error = state.error
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            error != null -> ErrorView(error, onRetry = null, modifier = Modifier.padding(padding))
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

                SectionCard(
                    title = "MAP",
                    trailing = { TextButton(onClick = onOpenMap) { Text("Open map") } },
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

    SectionCard("CLOCK") {
        Row(Modifier.fillMaxWidth()) {
            if (showUtc) {
                ClockValue("UTC", TimeFormats.clock(now, utc), Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
            }
            ClockValue("LOCAL  ${zone.id}", TimeFormats.clock(now, zone), Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text(TimeFormats.date(now, zone), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        when (sun) {
            is SunResult.Normal -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueTile("Sunrise", TimeFormats.hourMinute(sun.sunrise, zone), Modifier.weight(1f))
                ValueTile("Sunset", TimeFormats.hourMinute(sun.sunset, zone), Modifier.weight(1f))
                ValueTile("Daylight", TimeFormats.duration(sun.dayLength), Modifier.weight(1f))
            }
            SunResult.AlwaysUp -> Text("The sun does not set today at this location.")
            SunResult.AlwaysDown -> Text("The sun does not rise today at this location.")
        }
        Text(
            if (fix != null) "Sun times for your location" else "Sun times for Chornobyl (default location)",
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
        title = "REPEATERS",
        onClick = onClick,
        trailing = { Text("$total found", style = MaterialTheme.typography.labelMedium) },
    ) {
        if (total == 0) {
            Text("No repeaters in the local database yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            RadioMode.entries.filter { (counts[it] ?: 0) > 0 }.forEach { mode ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    ModeDot(mode)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${mode.label} - ${counts[mode]}",
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
    SectionCard(title = "DMR", onClick = onClick) {
        if (repeater == null) {
            Text("No DMR repeaters in the local database.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile("${repeater.bandName ?: "RX"} MHz", formatMHz(repeater.rxMHz), Modifier.weight(2f))
            ValueTile("Mode", repeater.mode.label, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile("Color code", repeater.colorCode?.let { "CC $it" } ?: NO_DATA, Modifier.weight(1f))
            ValueTile("Slot", repeater.timeslot?.let { "TS $it" } ?: NO_DATA, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Talkgroups: " + (repeater.talkgroupIds.takeIf { it.isNotEmpty() }?.joinToString(", ") { "TG $it" } ?: NO_DATA),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "${repeater.callsign} - ${repeater.source}",
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
        title = "PROPAGATION",
        onClick = onClick,
        trailing = {
            if (state.propagation.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile("Solar flux", data?.solarFlux?.toString() ?: NO_DATA, Modifier.weight(1f))
            ValueTile("K-index", data?.kIndex?.let { String.format(Locale.US, "%.1f", it) } ?: NO_DATA, Modifier.weight(1f))
            ValueTile("A-index", data?.aIndex?.toString() ?: NO_DATA, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text("Geomagnetic: " + (geomagneticState(data?.kIndex)?.label ?: NO_DATA), style = MaterialTheme.typography.bodyMedium)
        Text("Last X-ray flare: " + (data?.flareClass ?: NO_DATA), style = MaterialTheme.typography.bodyMedium)
        if (data != null && data.fetchedAtMillis > 0) {
            Text(
                "Fetched " + TimeFormats.dateTime(Instant.ofEpochMilli(data.fetchedAtMillis), ZoneId.of("UTC")) + " UTC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
