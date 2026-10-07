package com.hamdash.chornobyl

import com.hamdash.chornobyl.domain.RadiationBand
import com.hamdash.chornobyl.domain.RadiationReading
import com.hamdash.chornobyl.domain.aggregateRadiation
import com.hamdash.chornobyl.domain.median
import com.hamdash.chornobyl.domain.radiationBand
import com.hamdash.chornobyl.network.Safecast
import com.hamdash.chornobyl.network.SafecastParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RadiationTest {
    // The record layout follows Safecast's published description (captured_at, unit, value, latitude, longitude);
    // it is a test fixture, not a real measurement, and the live response has not been verified from here.
    private val now = 1_790_000_000L
    private val inZone = """"latitude":51.30,"longitude":30.10,"captured_at":"2026-09-01T10:00:00.000Z""""

    private fun record(value: String, unit: String, extra: String = inZone) =
        """{"id":1,"user_id":7,"value":$value,"unit":"$unit",$extra}"""

    @Test
    fun bandsGroupByUpperEdge() {
        assertEquals(RadiationBand.UP_TO_0_3, radiationBand(0.0))
        assertEquals(RadiationBand.UP_TO_0_3, radiationBand(0.3))
        assertEquals(RadiationBand.UP_TO_1, radiationBand(0.31))
        assertEquals(RadiationBand.UP_TO_10, radiationBand(1.0001))
        assertEquals(RadiationBand.OVER_10, radiationBand(10.01))
    }

    @Test
    fun medianHandlesOddAndEven() {
        assertEquals(2.0, median(listOf(3.0, 1.0, 2.0)), 1e-9)
        assertEquals(2.5, median(listOf(4.0, 1.0, 2.0, 3.0)), 1e-9)
    }

    @Test
    fun aggregatesIntoCellsAndKeepsEstimateFlag() {
        val r = { lat: Double, lon: Double, v: Double, t: Long, cpm: Boolean -> RadiationReading(lat, lon, v, t, cpm) }
        val cells = aggregateRadiation(
            listOf(
                r(51.301, 30.101, 0.10, 100, false),
                r(51.305, 30.105, 0.30, 200, true),
                r(51.309, 30.109, 0.20, 150, false),
                r(51.501, 30.301, 5.00, 300, false),
            ),
        )
        assertEquals(2, cells.size)
        assertEquals(5.0, cells.last().medianUsvH, 1e-9)
        val first = cells.first()
        assertEquals(3, first.count)
        assertEquals(0.20, first.medianUsvH, 1e-9)
        assertEquals(0.30, first.maxUsvH, 1e-9)
        assertEquals(200L, first.latestEpoch)
        assertTrue(first.estimated)
        assertFalse(cells.last().estimated)
        assertEquals(51.31, first.latitude, 1e-9)
        assertEquals(30.11, first.longitude, 1e-9)
    }

    @Test
    fun parsesMicrosievertAndConvertsCpm() {
        val body = "[" + record("0.25", "usv") + "," + record("334", "cpm") + "," + record("0.5", "µSv/h") + "]"
        val page = SafecastParser.parse(body, now)!!
        assertEquals(3, page.total)
        assertEquals(0, page.skipped)
        assertEquals(0.25, page.readings[0].microSvPerHour, 1e-9)
        assertFalse(page.readings[0].fromCpm)
        assertEquals(1.0, page.readings[1].microSvPerHour, 1e-9)
        assertTrue(page.readings[1].fromCpm)
        assertEquals(0.5, page.readings[2].microSvPerHour, 1e-9)
    }

    @Test
    fun skipsUnknownUnitsMissingAndImplausibleValues() {
        val body = "[" + listOf(
            record("1", "furlongs"),
            record("-1", "usv"),
            record("99999", "usv"),
            """{"value":1,"unit":"usv","latitude":51.3,"longitude":30.1}""",
            record("1", "usv", """"latitude":48.0,"longitude":30.1,"captured_at":"2026-09-01T10:00:00Z""""),
            record("1", "usv", """"latitude":51.3,"longitude":30.1,"captured_at":"2099-01-01T00:00:00Z""""),
            """{"value":null,"unit":"usv",$inZone}""",
            "42",
            record("0.1", "usv"),
        ).joinToString(",") + "]"
        val page = SafecastParser.parse(body, now)!!
        assertEquals(9, page.total)
        assertEquals(8, page.skipped)
        assertEquals(1, page.readings.size)
    }

    @Test
    fun notAnArrayIsRejected() {
        assertNull(SafecastParser.parse("""{"error":"x"}""", now))
        assertNull(SafecastParser.parse("not json", now))
        assertNotNull(SafecastParser.parse("[]", now))
    }

    @Test
    fun doesNotKeepWhoTookTheMeasurement() {
        val page = SafecastParser.parse("[" + record("0.2", "usv") + "]", now)!!
        assertFalse(page.readings.first().toString().contains("user"))
    }

    @Test
    fun urlUsesZoneCenterNoUserPosition() {
        val url = Safecast.measurementsUrl(51.3894, 30.0997, LocalDate.of(2026, 1, 2), 2)
        assertTrue(url.startsWith("https://api.safecast.org/measurements.json?latitude=51.3894&longitude=30.0997&distance=35000"))
        assertTrue(url.contains("captured_after=2026-01-02"))
        assertTrue(url.endsWith("&per_page=1000&page=2"))
    }
}
