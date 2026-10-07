package com.hamdash.chornobyl

import com.hamdash.chornobyl.domain.WeatherCurrent
import com.hamdash.chornobyl.domain.WeatherDay
import com.hamdash.chornobyl.domain.WeatherForecast
import com.hamdash.chornobyl.domain.WeatherHour
import com.hamdash.chornobyl.domain.flightInputAt
import com.hamdash.chornobyl.domain.flightInputFor
import com.hamdash.chornobyl.domain.isDaylight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlightInputTest {
    private val forecast = WeatherForecast(
        placeId = "CHORNOBYL",
        latitude = 51.3,
        longitude = 30.2,
        current = WeatherCurrent(timeEpoch = 10_000, windSpeedMs = 2.0, windGustsMs = 4.0),
        hourly = listOf(
            WeatherHour(timeEpoch = 7_200, windSpeedMs = 8.0, windGustsMs = 9.0, windSpeed80mMs = 11.0),
            WeatherHour(timeEpoch = 10_800, windSpeedMs = 10.0, windGustsMs = 13.0),
        ),
        daily = listOf(WeatherDay(dateEpoch = 0, sunriseEpoch = 5_000, sunsetEpoch = 40_000)),
    )

    @Test
    fun prefersFreshCurrentValues() {
        val now = forecast.flightInputAt(10_000, 2.0)!!
        assertEquals(2.0, now.windMs!!, 0.0)
        assertEquals(11.0, now.wind80mMs!!, 0.0)
        assertEquals(true, now.daylight)
        assertEquals(2.0, now.kp!!, 0.0)
    }

    @Test
    fun staleDataGivesNoCurrentConditions() {
        assertNull(forecast.flightInputAt(10_000 + 3 * 3600, null))
        val old = forecast.copy(current = forecast.current!!.copy(timeEpoch = 0))
        assertEquals(8.0, old.flightInputAt(9_000, null)!!.windMs!!, 0.0)
    }

    @Test
    fun hourInputUsesHourValues() {
        val input = forecast.flightInputFor(forecast.hourly[1], 6.0)
        assertEquals(13.0, input.gustMs!!, 0.0)
        assertEquals(true, input.daylight)
    }

    @Test
    fun daylightUnknownWithoutSunTimes() {
        assertNull(forecast.copy(daily = emptyList()).isDaylight(10_000))
        assertEquals(false, forecast.isDaylight(45_000))
    }
}
