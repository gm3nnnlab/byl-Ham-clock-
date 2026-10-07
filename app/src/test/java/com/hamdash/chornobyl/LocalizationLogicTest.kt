package com.hamdash.chornobyl

import com.hamdash.chornobyl.data.CatalogFormatException
import com.hamdash.chornobyl.data.CatalogParser
import com.hamdash.chornobyl.domain.DistanceUnit
import com.hamdash.chornobyl.domain.SyncProblem
import com.hamdash.chornobyl.domain.TimeFormats
import com.hamdash.chornobyl.domain.formatDistance
import com.hamdash.chornobyl.domain.formatOffset
import com.hamdash.chornobyl.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class LocalizationLogicTest {
    @Test fun syncProblemsSurviveStorage() {
        val all = listOf(
            SyncProblem.HttpsRequired, SyncProblem.TooLarge, SyncProblem.NotJson, SyncProblem.NotObject,
            SyncProblem.NewerSchema(7, 1), SyncProblem.NoValidEntries, SyncProblem.HttpStatus(503), SyncProblem.Unreachable,
        )
        for (problem in all) assertEquals(problem, SyncProblem.decode(problem.encode()))
        assertNull(SyncProblem.decode(null))
        assertEquals(SyncProblem.Unreachable, SyncProblem.decode("something old"))
    }

    @Test fun parserReportsTypedProblems() {
        fun problemOf(text: String) = (runCatching { CatalogParser.parse(text) }.exceptionOrNull() as CatalogFormatException).problem
        assertEquals(SyncProblem.NotJson, problemOf("not json"))
        assertEquals(SyncProblem.NotObject, problemOf("[]"))
        assertEquals(SyncProblem.NewerSchema(99, CatalogParser.SUPPORTED_SCHEMA), problemOf("{\"schemaVersion\":99}"))
    }

    @Test fun unitLabelsAreReplaceable() {
        assertEquals("8.0 км", formatDistance(8.0, DistanceUnit.KM, "км"))
        assertEquals("62 mi", formatDistance(100.0, DistanceUnit.MI))
        assertEquals("-7.600 МГц", formatOffset(-7.6, mhzLabel = "МГц"))
        assertEquals("симплекс", formatOffset(0.0, simplexLabel = "симплекс"))
        assertEquals("16г 07хв", TimeFormats.duration(Duration.ofMinutes(16 * 60 + 7), "г", "хв"))
        assertEquals("16h 07m", TimeFormats.duration(Duration.ofMinutes(16 * 60 + 7)))
    }

    @Test fun dateFollowsTheChosenLanguage() {
        val t = Instant.parse("2026-10-07T08:00:00Z")
        val zone = ZoneId.of("Europe/Kyiv")
        val en = TimeFormats.date(t, zone, AppLanguage.ENGLISH.locale)
        val uk = TimeFormats.date(t, zone, AppLanguage.UKRAINIAN.locale)
        assertEquals("Wed, 7 Oct 2026", en)
        assertNotEquals(en, uk)
        assertTrue("got $uk", uk.contains("жовт"))
    }

    @Test fun deviceLanguageSelectsDefault() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"))
            assertEquals(AppLanguage.UKRAINIAN, AppLanguage.fromDevice())
            Locale.setDefault(Locale.forLanguageTag("de-DE"))
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromDevice())
        } finally {
            Locale.setDefault(previous)
        }
        assertEquals(AppLanguage.UKRAINIAN, AppLanguage.fromName("UKRAINIAN"))
        assertNull(AppLanguage.fromName("klingon"))
    }
}
