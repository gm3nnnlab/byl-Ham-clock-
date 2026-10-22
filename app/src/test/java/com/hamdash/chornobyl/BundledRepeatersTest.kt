package com.hamdash.chornobyl

import com.hamdash.chornobyl.data.CatalogParser
import com.hamdash.chornobyl.domain.DataOrigin
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.RepeaterStatus
import com.hamdash.chornobyl.domain.bundledCatalogIsNewer
import com.hamdash.chornobyl.domain.sideBySideAnchors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledRepeatersTest {
    private val repeaters = CatalogParser.parse(File("src/main/assets/catalog.json").readText()).catalog.repeaters

    @Test
    fun bundledRepeatersAreTheSevenFromTheEarlierApp() {
        assertEquals(
            setOf("UR5EZ", "UT5EZ", "UR5EZC", "UR3EZS", "UT1EZ", "UR4EZ", "UR7EZ-10"),
            repeaters.map { it.callsign }.toSet(),
        )
        assertEquals(7, repeaters.size)
    }

    @Test
    fun fictionalEntriesAreLabeledAsDemoData() {
        assertTrue(repeaters.all { it.source.startsWith("DEMO DATA (fictional") })
        assertTrue(repeaters.all { it.status == RepeaterStatus.UNKNOWN })
    }

    @Test
    fun keepsTheValuesFromTheEarlierApp() {
        val fm = repeaters.first { it.callsign == "UR5EZ" }
        assertEquals("Chornobyl Exclusion Zone North", fm.name)
        assertEquals(RadioMode.FM, fm.mode)
        assertEquals(145.6875, fm.rxMHz, 1e-9)
        assertEquals(145.0875, fm.txMHz, 1e-9)
        assertEquals(88.5, fm.ctcssHz!!, 1e-9)
        val dmr = repeaters.first { it.callsign == "UT5EZ" }
        assertEquals(1, dmr.colorCode)
        assertEquals(2, dmr.timeslot)
        assertEquals(listOf(91), dmr.talkgroupIds)
        assertEquals(RadioMode.OTHER, repeaters.first { it.callsign == "UR7EZ-10" }.mode)
    }

    @Test
    fun newerBundledCatalogReplacesOnlyBundledData() {
        assertTrue(bundledCatalogIsNewer(DataOrigin.BUNDLED, "2026-10-06", "2026-10-22"))
        assertTrue(bundledCatalogIsNewer(DataOrigin.BUNDLED, null, "2026-10-22"))
        assertFalse(bundledCatalogIsNewer(DataOrigin.BUNDLED, "2026-10-22", "2026-10-22"))
        assertFalse(bundledCatalogIsNewer(DataOrigin.BUNDLED, "2026-11-01", "2026-10-22"))
        assertFalse(bundledCatalogIsNewer(DataOrigin.REMOTE, "2026-10-06", "2026-10-22"))
        assertFalse(bundledCatalogIsNewer(DataOrigin.BUNDLED, "2026-10-06", null))
        assertFalse(bundledCatalogIsNewer(DataOrigin.BUNDLED, "2026-10-06", "not a date"))
    }

    @Test
    fun repeatersAtTheSameSpotSitSideBySide() {
        val anchors = sideBySideAnchors(listOf(50.45 to 30.52, 50.40 to 30.51, 50.45 to 30.52))
        assertEquals(0.5f, anchors[1], 1e-6f)
        assertTrue(anchors[0] != anchors[2])
        assertEquals(0.5f, (anchors[0] + anchors[2]) / 2, 1e-6f)
        assertEquals(listOf(0.5f), sideBySideAnchors(listOf(51.0 to 30.0)))
        assertTrue(sideBySideAnchors(emptyList()).isEmpty())
    }
}
