package com.hamdash.chornobyl

import com.hamdash.chornobyl.data.ZoneParser
import com.hamdash.chornobyl.domain.CompassPoint
import com.hamdash.chornobyl.domain.FlightConditions
import com.hamdash.chornobyl.domain.FlightFactor
import com.hamdash.chornobyl.domain.FlightInput
import com.hamdash.chornobyl.domain.FlightVerdict
import com.hamdash.chornobyl.domain.FpvChannels
import com.hamdash.chornobyl.domain.LocalizedText
import com.hamdash.chornobyl.domain.WeatherCondition
import com.hamdash.chornobyl.domain.ZONE_CENTER_LAT
import com.hamdash.chornobyl.domain.ZONE_CENTER_LON
import com.hamdash.chornobyl.domain.compassPoint
import com.hamdash.chornobyl.domain.isInsideZoneRadius
import com.hamdash.chornobyl.domain.roundForPrivacy
import com.hamdash.chornobyl.domain.weatherCondition
import com.hamdash.chornobyl.network.OpenMeteo
import com.hamdash.chornobyl.network.OpenMeteoParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WeatherFpvZoneTest {

    private val calm = FlightInput(
        windMs = 3.0, gustMs = 5.0, wind80mMs = 6.0, precipitationMm = 0.0, precipitationProbabilityPct = 10,
        weatherCode = 1, visibilityM = 20_000.0, temperatureC = 18.0, kp = 2.0, daylight = true,
    )

    @Test
    fun calmDayIsGood() {
        val result = FlightConditions.assess(calm)
        assertEquals(FlightVerdict.GOOD, result.overall)
        assertTrue(result.factors.all { it.verdict == FlightVerdict.GOOD })
    }

    @Test
    fun worstFactorWins() {
        assertEquals(FlightVerdict.CAUTION, FlightConditions.assess(calm.copy(windMs = 8.0)).overall)
        assertEquals(FlightVerdict.NO_GO, FlightConditions.assess(calm.copy(gustMs = 15.0)).overall)
        assertEquals(FlightVerdict.NO_GO, FlightConditions.assess(calm.copy(weatherCode = 95)).overall)
        assertEquals(FlightVerdict.NO_GO, FlightConditions.assess(calm.copy(precipitationMm = 1.2)).overall)
        assertEquals(FlightVerdict.CAUTION, FlightConditions.assess(calm.copy(precipitationProbabilityPct = 60)).overall)
        assertEquals(FlightVerdict.NO_GO, FlightConditions.assess(calm.copy(visibilityM = 500.0)).overall)
        assertEquals(FlightVerdict.CAUTION, FlightConditions.assess(calm.copy(temperatureC = 0.0)).overall)
        assertEquals(FlightVerdict.NO_GO, FlightConditions.assess(calm.copy(temperatureC = -15.0)).overall)
        assertEquals(FlightVerdict.CAUTION, FlightConditions.assess(calm.copy(kp = 6.0)).overall)
        assertEquals(FlightVerdict.CAUTION, FlightConditions.assess(calm.copy(daylight = false)).overall)
    }

    @Test
    fun missingWindMeansNoOverallVerdict() {
        val result = FlightConditions.assess(calm.copy(windMs = null))
        assertNull(result.overall)
        assertNull(result.factors.first { it.factor == FlightFactor.WIND }.verdict)
    }

    @Test
    fun missingOptionalFactorsStayUnknown() {
        val result = FlightConditions.assess(FlightInput(windMs = 2.0, gustMs = 3.0))
        assertEquals(FlightVerdict.GOOD, result.overall)
        assertNull(result.factors.first { it.factor == FlightFactor.VISIBILITY }.verdict)
        assertNull(result.factors.first { it.factor == FlightFactor.GEOMAGNETIC }.verdict)
    }

    @Test
    fun fpvChannelsAreClassified() {
        assertEquals(40, FpvChannels.all.size)
        val r1 = FpvChannels.all.first { it.label == "R1" }
        assertEquals(5658, r1.mhz)
        assertFalse(r1.inSrdBand)
        assertTrue(r1.inAmateurBand)
        val a8 = FpvChannels.all.first { it.label == "A8" }
        assertTrue(a8.inSrdBand && a8.inAmateurBand)
        val e8 = FpvChannels.all.first { it.label == "E8" }
        assertFalse(e8.inSrdBand || e8.inAmateurBand)
    }

    @Test
    fun weatherHelpers() {
        assertEquals(WeatherCondition.CLEAR, weatherCondition(0))
        assertEquals(WeatherCondition.THUNDERSTORM_HAIL, weatherCondition(99))
        assertNull(weatherCondition(42))
        assertNull(weatherCondition(null))
        assertEquals(CompassPoint.N, compassPoint(350))
        assertEquals(CompassPoint.E, compassPoint(90))
        assertEquals(CompassPoint.SW, compassPoint(225))
        assertNull(compassPoint(null))
        assertEquals(51.3, roundForPrivacy(51.2763), 1e-9)
        assertEquals(30.2, roundForPrivacy(30.2219), 1e-9)
    }

    @Test
    fun forecastUrlUsesSiUnitsAndNoKey() {
        val url = OpenMeteo.forecastUrl(51.3, 30.2)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=51.3000&longitude=30.2000"))
        assertTrue(url.contains("wind_speed_unit=ms"))
        assertTrue(url.contains("timeformat=unixtime"))
    }

    @Test
    fun parsesOpenMeteoResponseKeepingNulls() {
        val body = """
            {"latitude":51.28,"longitude":30.22,
             "current":{"time":1760000000,"interval":900,"temperature_2m":12.5,"wind_speed_10m":4.2,
                        "wind_gusts_10m":null,"wind_direction_10m":270,"weather_code":3,"visibility":24000.0},
             "hourly":{"time":[1760000000,1760003600],"temperature_2m":[12.5,null],"wind_speed_10m":[4.2,5.0],
                       "wind_gusts_10m":[7.1,8.0],"precipitation_probability":[10,20],"weather_code":[3,61]},
             "daily":{"time":[1759960800],"temperature_2m_max":[15.0],"temperature_2m_min":[6.0],
                      "sunrise":[1759983600],"sunset":[1760023200]}}
        """.trimIndent()
        val forecast = OpenMeteoParser.parse(body, "CHORNOBYL", 51.28, 30.22, 99L)
        assertNotNull(forecast)
        forecast!!
        assertEquals(12.5, forecast.current?.temperatureC)
        assertNull(forecast.current?.windGustsMs)
        assertNull(forecast.current?.humidityPct)
        assertEquals(270, forecast.current?.windDirectionDeg)
        assertEquals(2, forecast.hourly.size)
        assertNull(forecast.hourly[1].temperatureC)
        assertNull(forecast.hourly[0].windSpeed80mMs)
        assertEquals(61, forecast.hourly[1].weatherCode)
        assertEquals(1759983600L, forecast.daily[0].sunriseEpoch)
        assertEquals(2, forecast.upcomingHours(1760003000, 5).size)
        assertEquals(1, forecast.upcomingHours(1760003700, 5).size)
    }

    @Test
    fun rejectsBrokenOrErrorResponses() {
        assertNull(OpenMeteoParser.parse("not json", "X", 0.0, 0.0, 0))
        assertNull(OpenMeteoParser.parse("""{"error":true,"reason":"bad"}""", "X", 0.0, 0.0, 0))
        assertNull(OpenMeteoParser.parse("{}", "X", 0.0, 0.0, 0))
    }

    @Test
    fun bundledZoneContentIsComplete() {
        val content = ZoneParser.parse(File("src/main/assets/zone.json").readText())
        assertTrue(content.history.size >= 15)
        assertTrue(content.places.size >= 5)
        assertEquals(4, content.games.size)
        assertTrue(content.history.all { it.source.startsWith("https://") && it.title.uk.isNotBlank() })
        assertTrue(content.places.all { it.name.uk.isNotBlank() && it.text.uk.isNotBlank() })
        val ids = content.places.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(content.places.first { it.id == "chnpp" }.let { isInsideZoneRadius(it.latitude, it.longitude) })
        assertFalse(content.places.first { it.id == "slavutych" }.let { isInsideZoneRadius(it.latitude, it.longitude) })
    }

    @Test
    fun zoneParserDropsUnsourcedEntries() {
        val json = """{"history":[{"date":"2000","title":{"en":"A"},"text":{"en":"b"},"source":""}],
            "places":[{"id":"x","category":"TOWN","lat":51.0,"lon":30.0,"name":{"en":"X"},"text":{"en":"y"}},
                      {"id":"y","category":"SECRET","lat":51.0,"lon":30.0,"name":{"en":"Y"},"text":{"en":"y"},"source":"https://a"}]}"""
        val content = ZoneParser.parse(json)
        assertTrue(content.history.isEmpty())
        assertTrue(content.places.isEmpty())
    }

    @Test
    fun localizedTextFallsBackToEnglish() {
        assertEquals("Hi", LocalizedText("Hi", "").pick("uk"))
        assertEquals("Привіт", LocalizedText("Hi", "Привіт").pick("uk"))
        assertEquals("Hi", LocalizedText("Hi", "Привіт").pick("en"))
        assertTrue(isInsideZoneRadius(ZONE_CENTER_LAT, ZONE_CENTER_LON))
    }
}
