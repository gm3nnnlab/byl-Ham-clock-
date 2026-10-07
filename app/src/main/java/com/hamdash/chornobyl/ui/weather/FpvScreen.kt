package com.hamdash.chornobyl.ui.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.FactorResult
import com.hamdash.chornobyl.domain.FlightConditions
import com.hamdash.chornobyl.domain.FlightFactor
import com.hamdash.chornobyl.domain.FlightInput
import com.hamdash.chornobyl.domain.FpvChannel
import com.hamdash.chornobyl.domain.FpvChannels
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.TimeZoneOptions
import com.hamdash.chornobyl.domain.flightInputAt
import com.hamdash.chornobyl.domain.flightInputFor
import com.hamdash.chornobyl.ui.AppViewModelProvider
import com.hamdash.chornobyl.ui.common.LabeledRow
import com.hamdash.chornobyl.ui.common.ScreenScaffold
import com.hamdash.chornobyl.ui.common.SectionCard
import com.hamdash.chornobyl.ui.common.VerdictBadge
import com.hamdash.chornobyl.ui.common.conditionText
import com.hamdash.chornobyl.ui.common.displayName
import com.hamdash.chornobyl.ui.common.noData
import com.hamdash.chornobyl.ui.common.percentText
import com.hamdash.chornobyl.ui.common.precipitationText
import com.hamdash.chornobyl.ui.common.temperatureText
import com.hamdash.chornobyl.ui.common.visibilityText
import com.hamdash.chornobyl.ui.common.windText
import com.hamdash.chornobyl.ui.theme.NumberFont
import java.time.Instant
import java.util.Locale

private val SrdColor = Color(0xFF2EBD6B)
private val HamColor = Color(0xFF3D8BFD)
private val OutsideColor = Color(0xFF6B7682)

@Composable
fun FpvScreen(onBack: () -> Unit, viewModel: WeatherViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val forecast = state.weather.forecast
    val zone = remember(state.settings.timeZoneId) { TimeZoneOptions.resolve(state.settings.timeZoneId) }
    val nowEpoch = Instant.now().epochSecond
    val input = forecast?.flightInputAt(nowEpoch, state.kp)
    val assessment = input?.let(FlightConditions::assess)

    ScreenScaffold(
        title = stringResource(R.string.fpv_title),
        onBack = onBack,
        actions = { RefreshAction(state.weather.loading, viewModel::refresh) },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LegalCard()
            PlaceLine(state)

            SectionCard(
                title = stringResource(R.string.fpv_now),
                trailing = { VerdictBadge(assessment?.overall) },
            ) {
                if (assessment == null || input == null) {
                    Text(stringResource(R.string.fpv_now_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    assessment.factors.forEach { FactorRow(it, input) }
                }
                Text(
                    stringResource(R.string.fpv_weather_only),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            SectionCard(stringResource(R.string.fpv_next_hours)) {
                val hours = forecast?.upcomingHours(nowEpoch, 12).orEmpty()
                if (forecast == null || hours.isEmpty()) {
                    Text(noData(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    hours.forEach { hour ->
                        val hourInput = forecast.flightInputFor(hour, state.kp)
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                TimeFormats.hourMinute(Instant.ofEpochSecond(hour.timeEpoch), zone),
                                fontFamily = NumberFont,
                                modifier = Modifier.width(56.dp),
                            )
                            VerdictBadge(FlightConditions.assess(hourInput).overall, Modifier.widthIn(min = 72.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.fpv_hour_line, windText(hour.windSpeedMs), windText(hour.windGustsMs), percentText(hour.precipitationProbabilityPct)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            SectionCard(stringResource(R.string.fpv_thresholds)) {
                Text(
                    stringResource(
                        R.string.fpv_thresholds_text,
                        fmt(FlightConditions.WIND_GOOD_MS), fmt(FlightConditions.WIND_LIMIT_MS),
                        fmt(FlightConditions.GUST_GOOD_MS), fmt(FlightConditions.GUST_LIMIT_MS),
                        fmt(FlightConditions.ALOFT_GOOD_MS), fmt(FlightConditions.ALOFT_LIMIT_MS),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            ChannelsCard()

            SectionCard(stringResource(R.string.fpv_links_title)) {
                Text(stringResource(R.string.fpv_links_text), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(stringResource(R.string.fpv_gnss_title)) {
                LabeledRow(
                    stringResource(R.string.dash_k_index),
                    state.kp?.let { String.format(Locale.US, "%.1f", it) } ?: noData(),
                )
                Text(stringResource(R.string.fpv_gnss_text), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(stringResource(R.string.fpv_checklist_title)) {
                Text(stringResource(R.string.fpv_checklist_text), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(stringResource(R.string.fpv_battery_title)) {
                Text(stringResource(R.string.fpv_battery_text), style = MaterialTheme.typography.bodySmall)
            }
            if (forecast != null) FooterCard(forecast, state.weather.error, zone)
        }
    }
}

private fun fmt(v: Double) = String.format(Locale.US, "%.0f", v)

@Composable
private fun LegalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.fpv_legal_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.fpv_legal_text),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun FactorRow(result: FactorResult, input: FlightInput) {
    val value = when (result.factor) {
        FlightFactor.WIND -> windText(input.windMs)
        FlightFactor.GUSTS -> windText(input.gustMs)
        FlightFactor.WIND_ALOFT -> windText(input.wind80mMs)
        FlightFactor.PRECIPITATION -> "${precipitationText(input.precipitationMm)}, ${percentText(input.precipitationProbabilityPct)}"
        FlightFactor.THUNDERSTORM -> conditionText(input.weatherCode)
        FlightFactor.VISIBILITY -> visibilityText(input.visibilityM)
        FlightFactor.TEMPERATURE -> temperatureText(input.temperatureC)
        FlightFactor.GEOMAGNETIC -> input.kp?.let { "Kp ${String.format(Locale.US, "%.1f", it)}" } ?: noData()
        FlightFactor.DAYLIGHT -> when (input.daylight) {
            true -> stringResource(R.string.fpv_day)
            false -> stringResource(R.string.fpv_night)
            null -> noData()
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(result.factor.displayName(), style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VerdictBadge(result.verdict)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelsCard() {
    SectionCard(stringResource(R.string.fpv_channels_title)) {
        Text(stringResource(R.string.fpv_channels_text), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Legend(SrdColor, stringResource(R.string.fpv_legend_srd))
            Legend(HamColor, stringResource(R.string.fpv_legend_ham))
            Legend(OutsideColor, stringResource(R.string.fpv_legend_outside))
        }
        FpvChannels.bandNames.forEach { band ->
            Text(
                stringResource(R.string.fpv_band, band),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FpvChannels.band(band).forEach { ChannelChip(it) }
            }
        }
    }
}

@Composable
private fun ChannelChip(channel: FpvChannel) {
    val color = when {
        channel.inSrdBand -> SrdColor
        channel.inAmateurBand -> HamColor
        else -> OutsideColor
    }
    Surface(color = color, shape = RoundedCornerShape(6.dp)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(channel.label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(channel.mhz.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontFamily = NumberFont)
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = RoundedCornerShape(3.dp), modifier = Modifier.padding(end = 4.dp)) {
            Spacer(Modifier.width(10.dp).padding(5.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
