package com.hamdash.chornobyl.ui.weather

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.FlightConditions
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.domain.WeatherCurrent
import com.hamdash.chornobyl.domain.WeatherDay
import com.hamdash.chornobyl.domain.WeatherForecast
import com.hamdash.chornobyl.domain.WeatherHour
import com.hamdash.chornobyl.domain.compassPoint
import com.hamdash.chornobyl.domain.flightInputAt
import com.hamdash.chornobyl.domain.flightInputFor
import com.hamdash.chornobyl.network.OpenMeteo
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.EmptyView
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.LoadingView
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.ValueTile
import com.hamdash.chornobyl.ui.common.VerdictBadge
import com.hamdash.chornobyl.ui.common.conditionText
import com.hamdash.chornobyl.ui.common.displayName
import com.hamdash.chornobyl.ui.common.noData
import com.hamdash.chornobyl.ui.common.percentText
import com.hamdash.chornobyl.ui.common.precipitationText
import com.hamdash.chornobyl.ui.common.pressureText
import com.hamdash.chornobyl.ui.common.temperatureText
import com.hamdash.chornobyl.ui.common.visibilityText
import com.hamdash.chornobyl.ui.common.windText
import com.hamdash.chornobyl.ui.theme.NumberFont
import java.time.Instant
import java.time.ZoneId

@Composable
fun WeatherScreen(
    onBack: () -> Unit,
    onOpenFpv: () -> Unit,
    viewModel: WeatherViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val forecast = state.weather.forecast
    val zone = remember(state.settings.timeZoneId) { TimeZoneOptions.resolve(state.settings.timeZoneId) }

    ScreenScaffold(
        title = stringResource(R.string.wx_title),
        onBack = onBack,
        actions = { RefreshAction(state.weather.loading, viewModel::refresh) },
    ) { padding ->
        when {
            forecast == null && state.weather.loading -> LoadingView(Modifier.padding(padding))
            forecast == null -> EmptyView(
                stringResource(R.string.no_data),
                stringResource(R.string.wx_none),
                Modifier.padding(padding),
            )
            else -> Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaceLine(state)
                val nowEpoch = Instant.now().epochSecond
                CurrentCard(forecast.current?.takeIf { nowEpoch - it.timeEpoch < 2 * 3600 })
                FpvSummaryCard(forecast, state.kp, nowEpoch, onOpenFpv)
                HoursCard(forecast, forecast.upcomingHours(nowEpoch, 12), state.kp, zone)
                DaysCard(forecast.daily, zone)
                FooterCard(forecast, state.weather.error, zone)
            }
        }
    }
}

@Composable
internal fun RefreshAction(loading: Boolean, onRefresh: () -> Unit) {
    if (loading) {
        CircularProgressIndicator(Modifier.padding(end = 16.dp).size(20.dp), strokeWidth = 2.dp)
    } else {
        IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh)) }
    }
}

@Composable
internal fun PlaceLine(state: WeatherUiState) {
    Column {
        Text(
            stringResource(R.string.wx_place, state.settings.weatherPlace.displayName()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        if (state.weather.usedFallbackPlace) {
            Text(
                stringResource(R.string.wx_fallback),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CurrentCard(current: WeatherCurrent?) {
    SectionCard(stringResource(R.string.wx_now)) {
        if (current == null) {
            Text(stringResource(R.string.wx_now_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                temperatureText(current.temperatureC),
                fontFamily = NumberFont,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(conditionText(current.weatherCode), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.wx_feels_like, temperatureText(current.apparentC)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueTile(stringResource(R.string.wx_wind), windText(current.windSpeedMs), Modifier.weight(1f))
            ValueTile(stringResource(R.string.wx_gusts), windText(current.windGustsMs), Modifier.weight(1f))
            ValueTile(
                stringResource(R.string.wx_direction),
                compassPoint(current.windDirectionDeg)?.displayName() ?: noData(),
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        LabeledRow(stringResource(R.string.wx_humidity), percentText(current.humidityPct))
        LabeledRow(stringResource(R.string.wx_precip), precipitationText(current.precipitationMm))
        LabeledRow(stringResource(R.string.wx_clouds), percentText(current.cloudCoverPct))
        LabeledRow(stringResource(R.string.wx_pressure), pressureText(current.pressureHpa))
        LabeledRow(stringResource(R.string.wx_visibility), visibilityText(current.visibilityM))
    }
}

@Composable
private fun FpvSummaryCard(forecast: WeatherForecast, kp: Double?, nowEpoch: Long, onOpenFpv: () -> Unit) {
    val assessment = forecast.flightInputAt(nowEpoch, kp)?.let(FlightConditions::assess)
    SectionCard(
        title = stringResource(R.string.wx_fpv_title),
        onClick = onOpenFpv,
        trailing = { TextButton(onClick = onOpenFpv) { Text(stringResource(R.string.action_open)) } },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerdictBadge(assessment?.overall)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.fpv_weather_only), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HoursCard(forecast: WeatherForecast, hours: List<WeatherHour>, kp: Double?, zone: ZoneId) {
    SectionCard(stringResource(R.string.wx_hours)) {
        if (hours.isEmpty()) {
            Text(stringResource(R.string.wx_hours_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            HeaderCell(stringResource(R.string.wx_col_time), 1f)
            HeaderCell(stringResource(R.string.wx_col_temp), 1.2f)
            HeaderCell(stringResource(R.string.wx_col_wind), 1.8f)
            HeaderCell(stringResource(R.string.wx_col_rain), 1f)
            HeaderCell(stringResource(R.string.wx_col_fpv), 1.6f)
        }
        hours.forEachIndexed { index, hour ->
            val verdict = FlightConditions.assess(forecast.flightInputFor(hour, kp)).overall
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Cell(TimeFormats.hourMinute(Instant.ofEpochSecond(hour.timeEpoch), zone), 1f)
                Cell(temperatureText(hour.temperatureC), 1.2f)
                Cell(
                    if (hour.windSpeedMs == null) noData() else "${windText(hour.windSpeedMs)} / ${windText(hour.windGustsMs)}",
                    1.8f,
                )
                Cell(percentText(hour.precipitationProbabilityPct), 1f)
                Column(Modifier.weight(1.6f)) { VerdictBadge(verdict) }
            }
            if (index < hours.lastIndex) HorizontalDivider()
        }
        Text(
            stringResource(R.string.wx_wind_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun DaysCard(days: List<WeatherDay>, zone: ZoneId) {
    val locale = LocalConfiguration.current.locales[0]
    SectionCard(stringResource(R.string.wx_days)) {
        if (days.isEmpty()) {
            Text(noData(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        days.forEachIndexed { index, day ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(
                    TimeFormats.date(Instant.ofEpochSecond(day.dateEpoch + 12 * 3600), zone, locale),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(conditionText(day.weatherCode), style = MaterialTheme.typography.bodyMedium)
                LabeledRow(
                    stringResource(R.string.wx_min_max),
                    "${temperatureText(day.temperatureMinC)} / ${temperatureText(day.temperatureMaxC)}",
                )
                LabeledRow(
                    stringResource(R.string.wx_precip),
                    "${precipitationText(day.precipitationSumMm)} (${percentText(day.precipitationProbabilityMaxPct)})",
                )
                LabeledRow(
                    stringResource(R.string.wx_wind_max),
                    "${windText(day.windSpeedMaxMs)} / ${windText(day.windGustsMaxMs)}",
                )
                LabeledRow(
                    stringResource(R.string.wx_sun),
                    if (day.sunriseEpoch != null && day.sunsetEpoch != null) {
                        "${TimeFormats.hourMinute(Instant.ofEpochSecond(day.sunriseEpoch), zone)} - " +
                            TimeFormats.hourMinute(Instant.ofEpochSecond(day.sunsetEpoch), zone)
                    } else {
                        noData()
                    },
                )
            }
            if (index < days.lastIndex) HorizontalDivider()
        }
    }
}

@Composable
internal fun FooterCard(forecast: WeatherForecast, error: Boolean, zone: ZoneId) {
    SectionCard(stringResource(R.string.wx_source_title)) {
        if (error) {
            Text(stringResource(R.string.wx_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        LabeledRow(
            stringResource(R.string.prop_fetched),
            if (forecast.fetchedAtMillis > 0) TimeFormats.dateTime(Instant.ofEpochMilli(forecast.fetchedAtMillis), zone) else noData(),
        )
        Text(
            stringResource(R.string.wx_source_text, OpenMeteo.ATTRIBUTION),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(text: String, weight: Float) {
    Text(text, modifier = Modifier.weight(weight), style = MaterialTheme.typography.bodySmall, fontFamily = NumberFont)
}
