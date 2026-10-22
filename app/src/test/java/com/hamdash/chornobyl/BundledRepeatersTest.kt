package com.hamdash.chornobyl

import com.hamdash.chornobyl.data.CatalogParser
import com.hamdash.chornobyl.domain.DataOrigin
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.RepeaterStatus
import com.hamdash.chornobyl.domain.bundledCatalogChanged
import com.hamdash.chornobyl.domain.catalogFingerprint
import com.hamdash.chornobyl.domain.sideBySideAnchors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledRepeatersTest {
    private val repeaters = CatalogParser.parse(File("src/main/assets/catalog.json").readText()).catalog.repeaters

    @Test
    fun bundledRepeatersAreRealPublicListings() {
        assertEquals(
            setOf("UR0UUB", "UR0UUM", "UR0UUJ", "UR0UUO", "EU1A04R", "EU7A02R"),
            repeaters.map { it.callsign }.toSet(),
        )
        assertEquals(6, repeaters.size)
        assertTrue(repeaters.none { "DEMO" in it.source })
        assertTrue(repeaters.all { it.source.startsWith("HearHam.live") || it.source.startsWith("BrandMeister") })
        assertTrue(repeaters.all { "retrieved 2026-10-22" in it.source })
    }

    @Test
    fun listingsDoNotClaimToBeOnAir() {
        assertTrue(repeaters.all { it.status == RepeaterStatus.UNKNOWN })
    }

    @Test
    fun keepsTheValuesFromTheSources() {
        val kyiv = repeaters.first { it.callsign == "UR0UUJ" }
        assertEquals(RadioMode.DMR, kyiv.mode)
        assertEquals(439.025, kyiv.rxMHz, 1e-9)
        assertEquals(431.425, kyiv.txMHz, 1e-9)
        assertEquals(1, kyiv.colorCode)
        assertTrue("repeater ID 255016" in kyiv.source)
        val both = repeaters.first { it.callsign == "EU7A02R" }
        assertTrue(both.source.startsWith("BrandMeister") && "HearHam.live" in both.source)
        assertEquals(438.85, repeaters.first { it.callsign == "UR0UUO" }.rxMHz, 1e-9)
    }

    @Test
    fun changedBundledCatalogReplacesOnlyBundledData() {
        val old = catalogFingerprint("""{"updated":"2026-10-22","repeaters":[]}""")
        val new = catalogFingerprint("""{"updated":"2026-10-22","repeaters":[{}]}""")
        assertTrue(old != new)
        assertEquals(old, catalogFingerprint("""{"updated":"2026-10-22","repeaters":[]}"""))
        assertTrue(bundledCatalogChanged(DataOrigin.BUNDLED, old, new))
        assertTrue(bundledCatalogChanged(DataOrigin.BUNDLED, null, new))
        assertFalse(bundledCatalogChanged(DataOrigin.BUNDLED, new, new))
        assertFalse(bundledCatalogChanged(DataOrigin.REMOTE, old, new))
        assertFalse(bundledCatalogChanged(DataOrigin.REMOTE, null, new))
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
