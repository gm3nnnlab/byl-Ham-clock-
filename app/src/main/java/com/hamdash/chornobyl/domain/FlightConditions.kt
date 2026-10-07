package com.hamdash.chornobyl.domain

enum class FlightVerdict { GOOD, CAUTION, NO_GO }

enum class FlightFactor { WIND, GUSTS, WIND_ALOFT, PRECIPITATION, THUNDERSTORM, VISIBILITY, TEMPERATURE, GEOMAGNETIC, DAYLIGHT }

/** [verdict] is null when the input for this factor is missing ("NO DATA"). */
data class FactorResult(val factor: FlightFactor, val verdict: FlightVerdict?)

/** [overall] is null when wind or gusts are unknown: without them no weather call can be made. */
data class FlightAssessment(val overall: FlightVerdict?, val factors: List<FactorResult>)

data class FlightInput(
    val windMs: Double? = null,
    val gustMs: Double? = null,
    val wind80mMs: Double? = null,
    val precipitationMm: Double? = null,
    val precipitationProbabilityPct: Int? = null,
    val weatherCode: Int? = null,
    val visibilityM: Double? = null,
    val temperatureC: Double? = null,
    val kp: Double? = null,
    val daylight: Boolean? = null,
)

/**
 * Weather suitability for small civilian FPV and camera drones. It rates the weather only and says
 * nothing about whether flying is permitted: that depends on airspace rules and authorizations.
 * Thresholds are conservative guidance for light quads, not manufacturer limits.
 */
object FlightConditions {
    const val WIND_GOOD_MS = 6.0
    const val WIND_LIMIT_MS = 9.0
    const val GUST_GOOD_MS = 9.0
    const val GUST_LIMIT_MS = 12.0
    const val ALOFT_GOOD_MS = 9.0
    const val ALOFT_LIMIT_MS = 13.0
    const val RAIN_LIMIT_MM = 0.5
    const val RAIN_PROBABILITY_CAUTION = 40
    const val VISIBILITY_LIMIT_M = 1_000.0
    const val VISIBILITY_GOOD_M = 5_000.0
    const val COLD_LIMIT_C = -10.0
    const val COLD_CAUTION_C = 5.0
    const val HOT_CAUTION_C = 35.0
    const val HOT_LIMIT_C = 45.0
    const val KP_CAUTION = 5.0

    private val THUNDER_CODES = setOf(95, 96, 99)

    fun assess(input: FlightInput): FlightAssessment {
        val factors = listOf(
            FactorResult(FlightFactor.WIND, band(input.windMs, WIND_GOOD_MS, WIND_LIMIT_MS)),
            FactorResult(FlightFactor.GUSTS, band(input.gustMs, GUST_GOOD_MS, GUST_LIMIT_MS)),
            FactorResult(FlightFactor.WIND_ALOFT, band(input.wind80mMs, ALOFT_GOOD_MS, ALOFT_LIMIT_MS)),
            FactorResult(FlightFactor.PRECIPITATION, precipitation(input.precipitationMm, input.precipitationProbabilityPct)),
            FactorResult(FlightFactor.THUNDERSTORM, input.weatherCode?.let { if (it in THUNDER_CODES) FlightVerdict.NO_GO else FlightVerdict.GOOD }),
            FactorResult(FlightFactor.VISIBILITY, visibility(input.visibilityM)),
            FactorResult(FlightFactor.TEMPERATURE, temperature(input.temperatureC)),
            FactorResult(FlightFactor.GEOMAGNETIC, input.kp?.let { if (it >= KP_CAUTION) FlightVerdict.CAUTION else FlightVerdict.GOOD }),
            FactorResult(FlightFactor.DAYLIGHT, input.daylight?.let { if (it) FlightVerdict.GOOD else FlightVerdict.CAUTION }),
        )
        val overall = if (input.windMs == null || input.gustMs == null) {
            null
        } else {
            factors.mapNotNull { it.verdict }.maxByOrNull { it.ordinal }
        }
        return FlightAssessment(overall, factors)
    }

    private fun band(value: Double?, good: Double, limit: Double): FlightVerdict? = when {
        value == null -> null
        value <= good -> FlightVerdict.GOOD
        value <= limit -> FlightVerdict.CAUTION
        else -> FlightVerdict.NO_GO
    }

    private fun precipitation(mm: Double?, probability: Int?): FlightVerdict? = when {
        mm == null && probability == null -> null
        mm != null && mm >= RAIN_LIMIT_MM -> FlightVerdict.NO_GO
        (mm != null && mm > 0.0) || (probability != null && probability >= RAIN_PROBABILITY_CAUTION) -> FlightVerdict.CAUTION
        else -> FlightVerdict.GOOD
    }

    private fun visibility(m: Double?): FlightVerdict? = when {
        m == null -> null
        m < VISIBILITY_LIMIT_M -> FlightVerdict.NO_GO
        m < VISIBILITY_GOOD_M -> FlightVerdict.CAUTION
        else -> FlightVerdict.GOOD
    }

    private fun temperature(c: Double?): FlightVerdict? = when {
        c == null -> null
        c < COLD_LIMIT_C || c > HOT_LIMIT_C -> FlightVerdict.NO_GO
        c < COLD_CAUTION_C || c > HOT_CAUTION_C -> FlightVerdict.CAUTION
        else -> FlightVerdict.GOOD
    }
}

/** Daylight at [epoch] from the forecast's sunrise and sunset; null when the forecast has no sun times. */
fun WeatherForecast.isDaylight(epoch: Long): Boolean? {
    val days = daily.filter { it.sunriseEpoch != null && it.sunsetEpoch != null }
    if (days.isEmpty()) return null
    return days.any { epoch >= it.sunriseEpoch!! && epoch < it.sunsetEpoch!! }
}

/** Conditions "now": current values if they are fresh, otherwise the forecast hour that contains [nowEpoch]. */
fun WeatherForecast.flightInputAt(nowEpoch: Long, kp: Double?): FlightInput? {
    val c = current?.takeIf { nowEpoch - it.timeEpoch in -CURRENT_MAX_AGE_S..CURRENT_MAX_AGE_S }
    val hour = upcomingHours(nowEpoch, 1).firstOrNull()?.takeIf { it.timeEpoch <= nowEpoch }
    if (c == null && hour == null) return null
    return FlightInput(
        windMs = c?.windSpeedMs ?: hour?.windSpeedMs,
        gustMs = c?.windGustsMs ?: hour?.windGustsMs,
        wind80mMs = hour?.windSpeed80mMs,
        precipitationMm = c?.precipitationMm ?: hour?.precipitationMm,
        precipitationProbabilityPct = hour?.precipitationProbabilityPct,
        weatherCode = c?.weatherCode ?: hour?.weatherCode,
        visibilityM = c?.visibilityM ?: hour?.visibilityM,
        temperatureC = c?.temperatureC ?: hour?.temperatureC,
        kp = kp,
        daylight = isDaylight(nowEpoch),
    )
}

fun WeatherForecast.flightInputFor(hour: WeatherHour, kp: Double?): FlightInput = FlightInput(
    windMs = hour.windSpeedMs,
    gustMs = hour.windGustsMs,
    wind80mMs = hour.windSpeed80mMs,
    precipitationMm = hour.precipitationMm,
    precipitationProbabilityPct = hour.precipitationProbabilityPct,
    weatherCode = hour.weatherCode,
    visibilityM = hour.visibilityM,
    temperatureC = hour.temperatureC,
    kp = kp,
    daylight = isDaylight(hour.timeEpoch + 1800),
)

private const val CURRENT_MAX_AGE_S = 2L * 3600
