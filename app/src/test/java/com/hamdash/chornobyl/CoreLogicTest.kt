package com.hamdash.chornobyl

import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.GeomagneticState
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.RepeaterFilter
import com.hamdash.chornobyl.domain.SunCalculator
import com.hamdash.chornobyl.domain.SunResult
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.bandNameFor
import com.hamdash.chornobyl.domain.countByMode
import com.hamdash.chornobyl.domain.filterBy
import com.hamdash.chornobyl.domain.formatDistance
import com.hamdash.chornobyl.domain.formatMHz
import com.hamdash.chornobyl.domain.formatOffset
import com.hamdash.chornobyl.domain.geomagneticState
import com.hamdash.chornobyl.domain.haversineKm
import com.hamdash.chornobyl.data.CatalogParser
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals as jAssertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CoreLogicTest {
    private val catalog = CatalogParser.parse(File("src/main/assets/catalog.json").readText())

    @Test fun formatsFrequencies() {
        jAssertEquals("438.400", formatMHz(438.4))
        jAssertEquals("145.6125", formatMHz(145.6125))
        jAssertEquals("14.000", formatMHz(14.0))
    }

    @Test fun formatsOffsets() {
        jAssertEquals("-7.600 MHz", formatOffset(430.8 - 438.4))
        jAssertEquals("-0.600 MHz", formatOffset(145.0125 - 145.6125))
        jAssertEquals("+0.600 MHz", formatOffset(0.6))
        jAssertEquals("simplex", formatOffset(0.0))
    }

    @Test fun namesBands() {
        jAssertEquals("70 cm", bandNameFor(438.4))
        jAssertEquals("2 m", bandNameFor(145.6))
        assertNull(bandNameFor(100.0))
    }

    @Test fun parsesModes() {
        jAssertEquals(RadioMode.DSTAR, RadioMode.parse("D-STAR"))
        jAssertEquals(RadioMode.C4FM, RadioMode.parse("System Fusion"))
        jAssertEquals(RadioMode.OTHER, RadioMode.parse(null))
        jAssertEquals(RadioMode.OTHER, RadioMode.parse("tetra"))
    }

    @Test fun bundledCatalogIsFullyValid() {
        assertTrue("rejected: ${catalog.rejected}", catalog.rejected.isEmpty())
        val c = catalog.catalog
        jAssertEquals(7, c.repeaters.size)
        jAssertEquals(12, c.bands.size)
        assertTrue(c.digitalModes.size >= 6)
        assertTrue(c.talkgroups.isNotEmpty())
        assertTrue(c.repeaters.all { it.source.isNotBlank() })
    }

    @Test fun countsMatchSpecExample() {
        val counts = catalog.catalog.repeaters.countByMode()
        jAssertEquals(3, counts[RadioMode.DMR])
        jAssertEquals(2, counts[RadioMode.FM])
        jAssertEquals(1, counts[RadioMode.DSTAR])
        jAssertEquals(1, counts[RadioMode.C4FM])
        jAssertEquals(0, counts[RadioMode.OTHER])
    }

    @Test fun filtersAndSearches() {
        val all = catalog.catalog.repeaters
        jAssertEquals(3, all.filterBy(RepeaterFilter.DMR).size)
        jAssertEquals(7, all.filterBy(RepeaterFilter.ALL).size)
        jAssertEquals(1, all.filterBy(RepeaterFilter.ALL, "zhytomyr").size)
        jAssertEquals(1, all.filterBy(RepeaterFilter.DMR, "438.525").size)
        jAssertEquals(0, all.filterBy(RepeaterFilter.OTHER).size)
    }

    @Test fun rejectsRepeatersWithUnknownSourceOrBadValues() {
        val doc = """
        {"schemaVersion":1,"repeaters":[
          {"id":"a","callsign":"A","rxMHz":438.4,"txMHz":430.8,"mode":"DMR","lat":50,"lon":30,"source":"x"},
          {"id":"b","callsign":"B","rxMHz":438.4,"txMHz":430.8,"mode":"DMR","lat":50,"lon":30},
          {"id":"c","callsign":"C","rxMHz":438.4,"txMHz":430.8,"mode":"DMR","lat":95,"lon":30,"source":"x"},
          {"id":"d","callsign":"D","rxMHz":438.4,"txMHz":430.8,"mode":"DMR","colorCode":16,"lat":50,"lon":30,"source":"x"},
          {"callsign":"E"}
        ]}"""
        val parsed = CatalogParser.parse(doc)
        jAssertEquals(listOf("a"), parsed.catalog.repeaters.map { it.id })
        jAssertEquals(4, parsed.rejected.size)
    }

    @Test fun malformedDocumentsAreRefused() {
        for (bad in listOf("not json", "[]", "{\"schemaVersion\":99}")) {
            val thrown = runCatching { CatalogParser.parse(bad) }.exceptionOrNull()
            assertNotNull("expected failure for: $bad", thrown)
        }
    }

    @Test fun sunTimesForKyivMidsummer() {
        val result = SunCalculator.calculate(LocalDate.of(2024, 6, 21), 50.45, 30.52) as SunResult.Normal
        val zone = ZoneId.of("Europe/Kyiv")
        val sunrise = result.sunrise.atZone(zone)
        val sunset = result.sunset.atZone(zone)
        assertNear(4 * 60 + 47, sunrise.hour * 60 + sunrise.minute, 6)
        assertNear(21 * 60 + 8, sunset.hour * 60 + sunset.minute, 6)
        assertTrue(result.dayLength > Duration.ofHours(16) && result.dayLength < Duration.ofHours(16).plusMinutes(40))
    }

    @Test fun sunTimesForKyivMidwinter() {
        val result = SunCalculator.calculate(LocalDate.of(2024, 12, 21), 50.45, 30.52) as SunResult.Normal
        assertTrue(result.dayLength > Duration.ofHours(7).plusMinutes(40) && result.dayLength < Duration.ofHours(8).plusMinutes(10))
    }

    @Test fun polarDayAndNight() {
        jAssertEquals(SunResult.AlwaysUp, SunCalculator.calculate(LocalDate.of(2024, 6, 21), 80.0, 20.0))
        jAssertEquals(SunResult.AlwaysDown, SunCalculator.calculate(LocalDate.of(2024, 12, 21), 80.0, 20.0))
    }

    @Test fun distances() {
        val km = haversineKm(50.4501, 30.5234, 51.4982, 31.2893)
        assertTrue("got $km", km in 125.0..131.0)
        jAssertEquals("8.0 km", formatDistance(8.0, DistanceUnit.KM))
        jAssertEquals("100 km", formatDistance(100.0, DistanceUnit.KM))
        jAssertEquals("62 mi", formatDistance(100.0, DistanceUnit.MI))
    }

    @Test fun geomagneticLabels() {
        assertNull(geomagneticState(null))
        jAssertEquals(GeomagneticState.QUIET, geomagneticState(1.67))
        jAssertEquals(GeomagneticState.ACTIVE, geomagneticState(4.0))
        jAssertEquals(GeomagneticState.STORM_G1, geomagneticState(5.0))
        jAssertEquals(GeomagneticState.STORM_G5, geomagneticState(9.0))
    }

    @Test fun formatsTime() {
        val t = Instant.parse("2026-10-06T08:05:09Z")
        jAssertEquals("08:05:09", TimeFormats.clock(t, ZoneId.of("UTC")))
        jAssertEquals("11:05:09", TimeFormats.clock(t, ZoneId.of("Europe/Kyiv")))
        jAssertEquals("16h 07m", TimeFormats.duration(Duration.ofMinutes(16 * 60 + 7)))
    }

    private fun assertNear(expected: Int, actual: Int, tolerance: Int) =
        assertTrue("expected $expected±$tolerance but was $actual", kotlin.math.abs(expected - actual) <= tolerance)
}
