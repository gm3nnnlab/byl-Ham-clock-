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

class RealRepeatersTest {
    private val repeaters = CatalogParser.parse(File("src/main/assets/catalog.json").readText()).catalog.repeaters

    @Test
    fun bundledRepeatersAreTheHearHamEntries() {
        assertEquals(
            setOf("hearham-4515", "hearham-4520", "hearham-4532", "hearham-12698"),
            repeaters.map { it.id }.toSet(),
        )
        assertTrue(repeaters.all { it.source.startsWith("HearHam.live repeater list") })
        assertTrue(repeaters.none { it.source.contains("DEMO", ignoreCase = true) })
        assertTrue(repeaters.all { it.lastUpdated == "2026-09-28" })
    }

    @Test
    fun hearHamListingDoesNotProveARepeaterIsOnAir() {
        assertTrue(repeaters.all { it.status == RepeaterStatus.UNKNOWN })
    }

    @Test
    fun keepsTheListedFrequencies() {
        val r = repeaters.first { it.callsign == "UR0UUO" }
        assertEquals(RadioMode.DMR, r.mode)
        assertEquals(438.85, r.rxMHz, 1e-9)
        assertEquals(431.25, r.txMHz, 1e-9)
        assertEquals(1, r.colorCode)
        assertEquals(null, r.timeslot)
        assertTrue(r.talkgroupIds.isEmpty())
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

    @Test
    fun bundledKyivRepeatersDoNotHideEachOther() {
        val anchors = sideBySideAnchors(repeaters.map { it.latitude to it.longitude })
        val atKyivCentre = repeaters.indices.filter { repeaters[it].callsign in setOf("UR0UUM", "UR0UUB") }
        assertEquals(2, atKyivCentre.size)
        assertTrue(anchors[atKyivCentre[0]] != anchors[atKyivCentre[1]])
    }
}
