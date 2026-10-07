package com.hamdash.chornobyl

import com.hamdash.chornobyl.data.CatalogParser
import com.hamdash.chornobyl.domain.RepeaterStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class RepeaterStatusTest {

    @Test
    fun parsesKnownSpellings() {
        assertEquals(RepeaterStatus.ACTIVE, RepeaterStatus.parse("active"))
        assertEquals(RepeaterStatus.ACTIVE, RepeaterStatus.parse("On air"))
        assertEquals(RepeaterStatus.OFF_AIR, RepeaterStatus.parse("OFF_AIR"))
        assertEquals(RepeaterStatus.OFF_AIR, RepeaterStatus.parse("off-air"))
        assertEquals(RepeaterStatus.OFF_AIR, RepeaterStatus.parse("suspended"))
    }

    @Test
    fun anythingElseIsUnknown() {
        assertEquals(RepeaterStatus.UNKNOWN, RepeaterStatus.parse(null))
        assertEquals(RepeaterStatus.UNKNOWN, RepeaterStatus.parse(""))
        assertEquals(RepeaterStatus.UNKNOWN, RepeaterStatus.parse("maybe"))
    }

    @Test
    fun catalogReadsStatusAndDefaultsToUnknown() {
        val json = """{"repeaters":[
            {"id":"a","callsign":"UR0AAA","rxMHz":438.4,"txMHz":430.8,"mode":"DMR","lat":50.4,"lon":30.5,"source":"test","status":"OFF_AIR"},
            {"id":"b","callsign":"UR0BBB","rxMHz":145.6,"txMHz":145.0,"mode":"FM","lat":50.4,"lon":30.5,"source":"test"}]}"""
        val repeaters = CatalogParser.parse(json).catalog.repeaters.associateBy { it.id }
        assertEquals(RepeaterStatus.OFF_AIR, repeaters.getValue("a").status)
        assertEquals(RepeaterStatus.UNKNOWN, repeaters.getValue("b").status)
    }

    @Test
    fun bundledDemoEntriesDoNotClaimToBeOnAir() {
        val catalog = CatalogParser.parse(File("src/main/assets/catalog.json").readText()).catalog
        assert(catalog.repeaters.none { it.status == RepeaterStatus.ACTIVE })
    }
}
