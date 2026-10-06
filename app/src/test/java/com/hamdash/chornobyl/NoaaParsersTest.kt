package com.hamdash.chornobyl

import com.hamdash.chornobyl.network.NoaaParsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NoaaParsersTest {
    @Test fun solarFluxFromSummaryObject() {
        assertEquals(145, NoaaParsers.parseSolarFlux("""{"Flux":"145","TimeStamp":"2026-10-06 20:00:00"}"""))
        assertEquals(158, NoaaParsers.parseSolarFlux("""{"Flux":157.6}"""))
    }

    @Test fun solarFluxRejectsGarbage() {
        assertNull(NoaaParsers.parseSolarFlux("<html>503</html>"))
        assertNull(NoaaParsers.parseSolarFlux("""{"Flux":"n/a"}"""))
        assertNull(NoaaParsers.parseSolarFlux("""{"Flux":"0"}"""))
        assertNull(NoaaParsers.parseSolarFlux("[]"))
    }

    @Test fun kIndexFromHeaderRows() {
        val body = """[["time_tag","Kp","a_running","station_count"],
            ["2026-10-06 00:00:00.000","2.00","7","8"],
            ["2026-10-06 03:00:00.000","3.33","18","8"]]"""
        val k = NoaaParsers.parseKIndex(body)!!
        assertEquals(3.33, k.kp, 0.0001)
        assertEquals(18, k.aIndex)
        assertEquals("2026-10-06 03:00:00.000", k.time)
    }

    @Test fun kIndexFromObjects() {
        val body = """[{"time_tag":"2026-10-06T00:00:00","Kp":1.67,"a_running":6,"station_count":8},
                       {"time_tag":"2026-10-06T03:00:00","Kp":2.33,"a_running":9,"station_count":8}]"""
        val k = NoaaParsers.parseKIndex(body)!!
        assertEquals(2.33, k.kp, 0.0001)
        assertEquals(9, k.aIndex)
    }

    @Test fun kIndexRejectsGarbage() {
        assertNull(NoaaParsers.parseKIndex("{}"))
        assertNull(NoaaParsers.parseKIndex("[]"))
        assertNull(NoaaParsers.parseKIndex("""[["time_tag","other"],["x","y"]]"""))
        assertNull(NoaaParsers.parseKIndex("oops"))
    }

    @Test fun flareLatest() {
        val f = NoaaParsers.parseFlare("""[{"time_tag":"2026-10-05T12:00:00Z","max_class":"M1.2","max_time":"2026-10-05T12:30:00Z"}]""")
        assertNotNull(f)
        assertEquals("M1.2", f!!.flareClass)
        assertEquals("2026-10-05T12:30:00Z", f.time)
        assertNull(NoaaParsers.parseFlare("[]"))
        assertNull(NoaaParsers.parseFlare("""[{"foo":1}]"""))
    }
}
