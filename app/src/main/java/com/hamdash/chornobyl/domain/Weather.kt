package com.hamdash.chornobyl.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Current conditions. Every field is null when the source did not provide it; the UI shows "NO DATA". */
@Serializable
data class WeatherCurrent(
    val timeEpoch: Long,
    val temperatureC: Double? = null,
    val apparentC: Double? = null,
    val humidityPct: Int? = null,
    val precipitationMm: Double? = null,
    val weatherCode: Int? = null,
    val cloudCoverPct: Int? = null,
    val pressureHpa: Double? = null,
    val windSpeedMs: Double? = null,
    val windDirectionDeg: Int? = null,
    val windGustsMs: Double? = null,
    val visibilityM: Double? = null,
)

@Serializable
data class WeatherHour(
    val timeEpoch: Long,
    val temperatureC: Double? = null,
    val precipitationProbabilityPct: Int? = null,
    val precipitationMm: Double? = null,
    val weatherCode: Int? = null,
    val windSpeedMs: Double? = null,
    val windGustsMs: Double? = null,
    val windSpeed80mMs: Double? = null,
    val visibilityM: Double? = null,
)

@Serializable
data class WeatherDay(
    val dateEpoch: Long,
    val weatherCode: Int? = null,
    val temperatureMaxC: Double? = null,
    val temperatureMinC: Double? = null,
    val precipitationSumMm: Double? = null,
    val precipitationProbabilityMaxPct: Int? = null,
    val windSpeedMaxMs: Double? = null,
    val windGustsMaxMs: Double? = null,
    val sunriseEpoch: Long? = null,
    val sunsetEpoch: Long? = null,
)

@Serializable
data class WeatherForecast(
    val placeId: String,
    val latitude: Double,
    val longitude: Double,
    val current: WeatherCurrent? = null,
    val hourly: List<WeatherHour> = emptyList(),
    val daily: List<WeatherDay> = emptyList(),
    val fetchedAtMillis: Long = 0L,
) {
    val hasAnyValue: Boolean get() = current != null || hourly.isNotEmpty() || daily.isNotEmpty()

    /** Hours from the one containing [nowEpoch] onwards. */
    fun upcomingHours(nowEpoch: Long, count: Int): List<WeatherHour> =
        hourly.filter { it.timeEpoch + 3600 > nowEpoch }.take(count)
}

data class WeatherSnapshot(
    val forecast: WeatherForecast? = null,
    val loading: Boolean = false,
    val error: Boolean = false,
    /** True when "my location" was chosen but no GPS fix was available, so the default place was used. */
    val usedFallbackPlace: Boolean = false,
)

/**
 * Places the forecast can be requested for. Only public towns; [MY_LOCATION] is opt-in and sends a
 * position rounded to 0.1 degree (about 10 km) to the weather service.
 */
enum class WeatherPlace(val latitude: Double, val longitude: Double) {
    CHORNOBYL(51.2763, 30.2219),
    PRIPYAT(51.4045, 30.0542),
    SLAVUTYCH(51.5222, 30.7569),
    KYIV(50.4501, 30.5234),
    MY_LOCATION(51.2763, 30.2219),
    ;

    companion object {
        val DEFAULT = CHORNOBYL
        fun fromName(name: String?): WeatherPlace? = entries.firstOrNull { it.name == name }
    }
}

/** Rounds a coordinate to one decimal (about 11 km in latitude) before it leaves the device. */
fun roundForPrivacy(value: Double): Double = (value * 10).roundToInt() / 10.0

/** WMO weather interpretation codes as used by Open-Meteo. */
enum class WeatherCondition {
    CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG, DRIZZLE, FREEZING_DRIZZLE, RAIN, FREEZING_RAIN,
    SNOW, SNOW_GRAINS, RAIN_SHOWERS, SNOW_SHOWERS, THUNDERSTORM, THUNDERSTORM_HAIL,
}

fun weatherCondition(code: Int?): WeatherCondition? = when (code) {
    0 -> WeatherCondition.CLEAR
    1 -> WeatherCondition.MAINLY_CLEAR
    2 -> WeatherCondition.PARTLY_CLOUDY
    3 -> WeatherCondition.OVERCAST
    45, 48 -> WeatherCondition.FOG
    51, 53, 55 -> WeatherCondition.DRIZZLE
    56, 57 -> WeatherCondition.FREEZING_DRIZZLE
    61, 63, 65 -> WeatherCondition.RAIN
    66, 67 -> WeatherCondition.FREEZING_RAIN
    71, 73, 75 -> WeatherCondition.SNOW
    77 -> WeatherCondition.SNOW_GRAINS
    80, 81, 82 -> WeatherCondition.RAIN_SHOWERS
    85, 86 -> WeatherCondition.SNOW_SHOWERS
    95 -> WeatherCondition.THUNDERSTORM
    96, 99 -> WeatherCondition.THUNDERSTORM_HAIL
    else -> null
}

enum class CompassPoint { N, NE, E, SE, S, SW, W, NW }

/** Direction the wind blows FROM, as one of 8 compass points. */
fun compassPoint(degrees: Int?): CompassPoint? {
    if (degrees == null) return null
    val normalized = ((degrees % 360) + 360) % 360
    return CompassPoint.entries[((normalized + 22.5) / 45.0).toInt() % 8]
}
