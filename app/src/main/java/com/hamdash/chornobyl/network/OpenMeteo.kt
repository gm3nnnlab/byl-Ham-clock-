package com.hamdash.chornobyl.network

import com.hamdash.chornobyl.domain.WeatherCurrent
import com.hamdash.chornobyl.domain.WeatherDay
import com.hamdash.chornobyl.domain.WeatherForecast
import com.hamdash.chornobyl.domain.WeatherHour
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.Locale
import kotlin.math.roundToInt

/** Open-Meteo forecast API: free, no key, data under CC BY 4.0. */
object OpenMeteo {
    const val ATTRIBUTION = "Weather data by Open-Meteo.com (CC BY 4.0)"

    private const val CURRENT = "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code," +
        "cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility"
    private const val HOURLY = "temperature_2m,precipitation_probability,precipitation,weather_code," +
        "wind_speed_10m,wind_gusts_10m,wind_speed_80m,visibility"
    private const val DAILY = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum," +
        "precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,sunrise,sunset"

    fun forecastUrl(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f", latitude, longitude) +
            "&current=$CURRENT&hourly=$HOURLY&daily=$DAILY" +
            "&wind_speed_unit=ms&timeformat=unixtime&timezone=auto&forecast_days=3"
}

object OpenMeteoParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns null when the body is not a usable forecast. Missing values stay null, never guessed. */
    fun parse(body: String, placeId: String, latitude: Double, longitude: Double, fetchedAtMillis: Long): WeatherForecast? {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
        if (root["error"]?.let { (it as? JsonPrimitive)?.content == "true" } == true) return null

        val current = (root["current"] as? JsonObject)?.let { c ->
            val time = c.long("time") ?: return@let null
            WeatherCurrent(
                timeEpoch = time,
                temperatureC = c.double("temperature_2m"),
                apparentC = c.double("apparent_temperature"),
                humidityPct = c.double("relative_humidity_2m")?.roundToInt(),
                precipitationMm = c.double("precipitation"),
                weatherCode = c.double("weather_code")?.roundToInt(),
                cloudCoverPct = c.double("cloud_cover")?.roundToInt(),
                pressureHpa = c.double("pressure_msl"),
                windSpeedMs = c.double("wind_speed_10m"),
                windDirectionDeg = c.double("wind_direction_10m")?.roundToInt(),
                windGustsMs = c.double("wind_gusts_10m"),
                visibilityM = c.double("visibility"),
            )
        }

        val hourly = (root["hourly"] as? JsonObject)?.let { h ->
            val times = h.longs("time")
            val temp = h.doubles("temperature_2m")
            val prob = h.doubles("precipitation_probability")
            val precip = h.doubles("precipitation")
            val code = h.doubles("weather_code")
            val wind = h.doubles("wind_speed_10m")
            val gust = h.doubles("wind_gusts_10m")
            val wind80 = h.doubles("wind_speed_80m")
            val vis = h.doubles("visibility")
            times.mapIndexedNotNull { i, t ->
                t ?: return@mapIndexedNotNull null
                WeatherHour(
                    timeEpoch = t,
                    temperatureC = temp.at(i),
                    precipitationProbabilityPct = prob.at(i)?.roundToInt(),
                    precipitationMm = precip.at(i),
                    weatherCode = code.at(i)?.roundToInt(),
                    windSpeedMs = wind.at(i),
                    windGustsMs = gust.at(i),
                    windSpeed80mMs = wind80.at(i),
                    visibilityM = vis.at(i),
                )
            }
        }.orEmpty()

        val daily = (root["daily"] as? JsonObject)?.let { d ->
            val times = d.longs("time")
            val code = d.doubles("weather_code")
            val max = d.doubles("temperature_2m_max")
            val min = d.doubles("temperature_2m_min")
            val sum = d.doubles("precipitation_sum")
            val prob = d.doubles("precipitation_probability_max")
            val wind = d.doubles("wind_speed_10m_max")
            val gust = d.doubles("wind_gusts_10m_max")
            val rise = d.longs("sunrise")
            val set = d.longs("sunset")
            times.mapIndexedNotNull { i, t ->
                t ?: return@mapIndexedNotNull null
                WeatherDay(
                    dateEpoch = t,
                    weatherCode = code.at(i)?.roundToInt(),
                    temperatureMaxC = max.at(i),
                    temperatureMinC = min.at(i),
                    precipitationSumMm = sum.at(i),
                    precipitationProbabilityMaxPct = prob.at(i)?.roundToInt(),
                    windSpeedMaxMs = wind.at(i),
                    windGustsMaxMs = gust.at(i),
                    sunriseEpoch = rise.getOrNull(i),
                    sunsetEpoch = set.getOrNull(i),
                )
            }
        }.orEmpty()

        val forecast = WeatherForecast(placeId, latitude, longitude, current, hourly, daily, fetchedAtMillis)
        return forecast.takeIf { it.hasAnyValue }
    }

    private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

    private fun JsonObject.doubles(key: String): List<Double?> =
        (this[key] as? JsonArray)?.map { (it as? JsonPrimitive)?.doubleOrNull }.orEmpty()

    private fun JsonObject.longs(key: String): List<Long?> =
        (this[key] as? JsonArray)?.map { e: JsonElement -> (e as? JsonPrimitive)?.longOrNull }.orEmpty()

    private fun List<Double?>.at(i: Int): Double? = getOrNull(i)
}
