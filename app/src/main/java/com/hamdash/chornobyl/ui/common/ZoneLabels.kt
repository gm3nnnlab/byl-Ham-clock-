package com.hamdash.chornobyl.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdash.chornobyl.R
import com.hamdash.chornobyl.domain.CompassPoint
import com.hamdash.chornobyl.domain.FlightFactor
import com.hamdash.chornobyl.domain.FlightVerdict
import com.hamdash.chornobyl.domain.LocalizedText
import com.hamdash.chornobyl.domain.PlaceCategory
import com.hamdash.chornobyl.domain.WeatherCondition
import com.hamdash.chornobyl.domain.WeatherPlace
import java.util.Locale

/** Picks the Ukrainian or English text for the language the UI is currently shown in. */
@Composable
fun LocalizedText.localized(): String = pick(LocalConfiguration.current.locales[0].language)

@Composable
fun WeatherCondition.displayName(): String = stringResource(
    when (this) {
        WeatherCondition.CLEAR -> R.string.wx_clear
        WeatherCondition.MAINLY_CLEAR -> R.string.wx_mainly_clear
        WeatherCondition.PARTLY_CLOUDY -> R.string.wx_partly_cloudy
        WeatherCondition.OVERCAST -> R.string.wx_overcast
        WeatherCondition.FOG -> R.string.wx_fog
        WeatherCondition.DRIZZLE -> R.string.wx_drizzle
        WeatherCondition.FREEZING_DRIZZLE -> R.string.wx_freezing_drizzle
        WeatherCondition.RAIN -> R.string.wx_rain
        WeatherCondition.FREEZING_RAIN -> R.string.wx_freezing_rain
        WeatherCondition.SNOW -> R.string.wx_snow
        WeatherCondition.SNOW_GRAINS -> R.string.wx_snow_grains
        WeatherCondition.RAIN_SHOWERS -> R.string.wx_rain_showers
        WeatherCondition.SNOW_SHOWERS -> R.string.wx_snow_showers
        WeatherCondition.THUNDERSTORM -> R.string.wx_thunderstorm
        WeatherCondition.THUNDERSTORM_HAIL -> R.string.wx_thunderstorm_hail
    },
)

@Composable
fun CompassPoint.displayName(): String = stringResource(
    when (this) {
        CompassPoint.N -> R.string.dir_n
        CompassPoint.NE -> R.string.dir_ne
        CompassPoint.E -> R.string.dir_e
        CompassPoint.SE -> R.string.dir_se
        CompassPoint.S -> R.string.dir_s
        CompassPoint.SW -> R.string.dir_sw
        CompassPoint.W -> R.string.dir_w
        CompassPoint.NW -> R.string.dir_nw
    },
)

@Composable
fun WeatherPlace.displayName(): String = stringResource(
    when (this) {
        WeatherPlace.CHORNOBYL -> R.string.place_chornobyl
        WeatherPlace.PRIPYAT -> R.string.place_pripyat
        WeatherPlace.SLAVUTYCH -> R.string.place_slavutych
        WeatherPlace.KYIV -> R.string.place_kyiv
        WeatherPlace.MY_LOCATION -> R.string.place_my_location
    },
)

@Composable
fun FlightVerdict?.displayName(): String = when (this) {
    FlightVerdict.GOOD -> stringResource(R.string.fpv_good)
    FlightVerdict.CAUTION -> stringResource(R.string.fpv_caution)
    FlightVerdict.NO_GO -> stringResource(R.string.fpv_no_go)
    null -> noData()
}

fun FlightVerdict?.color(): Color = when (this) {
    FlightVerdict.GOOD -> Color(0xFF2EBD6B)
    FlightVerdict.CAUTION -> Color(0xFFE8A317)
    FlightVerdict.NO_GO -> Color(0xFFE5484D)
    null -> Color(0xFF7A8794)
}

@Composable
fun FlightFactor.displayName(): String = stringResource(
    when (this) {
        FlightFactor.WIND -> R.string.fpv_f_wind
        FlightFactor.GUSTS -> R.string.fpv_f_gusts
        FlightFactor.WIND_ALOFT -> R.string.fpv_f_aloft
        FlightFactor.PRECIPITATION -> R.string.fpv_f_precip
        FlightFactor.THUNDERSTORM -> R.string.fpv_f_thunder
        FlightFactor.VISIBILITY -> R.string.fpv_f_visibility
        FlightFactor.TEMPERATURE -> R.string.fpv_f_temperature
        FlightFactor.GEOMAGNETIC -> R.string.fpv_f_geomagnetic
        FlightFactor.DAYLIGHT -> R.string.fpv_f_daylight
    },
)

@Composable
fun PlaceCategory.displayName(): String = stringResource(
    when (this) {
        PlaceCategory.NUCLEAR -> R.string.cat_nuclear
        PlaceCategory.TOWN -> R.string.cat_town
        PlaceCategory.RADIO -> R.string.cat_radio
        PlaceCategory.NATURE -> R.string.cat_nature
        PlaceCategory.MEMORIAL -> R.string.cat_memorial
    },
)

@Composable
fun VerdictBadge(verdict: FlightVerdict?, modifier: Modifier = Modifier) {
    Surface(color = verdict.color(), shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            verdict.displayName(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun oneDecimal(v: Double) = String.format(Locale.US, "%.1f", v)

@Composable
fun temperatureText(c: Double?): String = c?.let { stringResource(R.string.value_celsius, oneDecimal(it)) } ?: noData()

@Composable
fun windText(ms: Double?): String = ms?.let { stringResource(R.string.value_ms, oneDecimal(it)) } ?: noData()

@Composable
fun precipitationText(mm: Double?): String = mm?.let { stringResource(R.string.value_mm, oneDecimal(it)) } ?: noData()

@Composable
fun percentText(p: Int?): String = p?.let { stringResource(R.string.value_percent, it) } ?: noData()

@Composable
fun visibilityText(m: Double?): String = m?.let { stringResource(R.string.value_km, oneDecimal(it / 1000.0)) } ?: noData()

@Composable
fun pressureText(hpa: Double?): String = hpa?.let { stringResource(R.string.value_hpa, String.format(Locale.US, "%.0f", it)) } ?: noData()

@Composable
fun conditionText(code: Int?): String =
    com.hamdash.chornobyl.domain.weatherCondition(code)?.displayName() ?: noData()
