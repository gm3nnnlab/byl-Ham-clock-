package com.hamdash.chornobyl.network

import com.hamdash.chornobyl.domain.CPM_PER_MICROSV_H
import com.hamdash.chornobyl.domain.RADIATION_MAX_PLAUSIBLE_USVH
import com.hamdash.chornobyl.domain.RadiationReading
import com.hamdash.chornobyl.domain.ZONE_CENTER_LAT
import com.hamdash.chornobyl.domain.ZONE_CENTER_LON
import com.hamdash.chornobyl.domain.ZONE_RADIUS_KM
import com.hamdash.chornobyl.domain.haversineKm
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Locale

/** Safecast open data (safecast.org): volunteer radiation measurements released under CC0. */
object Safecast {
    const val ATTRIBUTION = "Safecast (safecast.org), open data under CC0"
    const val PER_PAGE = 1000
    const val MAX_PAGES = 5

    /** Search radius around the plant: the 30 km Zone plus a small margin. */
    const val SEARCH_RADIUS_METERS = 35_000

    fun measurementsUrl(latitude: Double, longitude: Double, capturedAfter: LocalDate, page: Int): String =
        String.format(
            Locale.US,
            "https://api.safecast.org/measurements.json?latitude=%.4f&longitude=%.4f&distance=%d",
            latitude,
            longitude,
            SEARCH_RADIUS_METERS,
        ) + "&captured_after=$capturedAfter&order=captured_at+desc&per_page=$PER_PAGE&page=$page"
}

/** [total] counts every record in the response; [skipped] those that could not be used. */
data class SafecastPage(val readings: List<RadiationReading>, val total: Int, val skipped: Int)

object SafecastParser {
    private val json = Json { ignoreUnknownKeys = true }
    private const val FUTURE_TOLERANCE_S = 86_400L
    private const val EDGE_MARGIN_KM = 8.0

    /**
     * Returns null when [body] is not a JSON array. Only latitude, longitude, value, unit and capture time are
     * read: nothing about who took a measurement is kept. A record with an unknown unit or implausible
     * value is skipped, never guessed.
     */
    fun parse(body: String, nowEpoch: Long): SafecastPage? {
        val array = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonArray ?: return null
        val readings = ArrayList<RadiationReading>(array.size)
        var skipped = 0
        for (element in array) {
            val reading = (element as? JsonObject)?.let { toReading(it, nowEpoch) }
            if (reading == null) skipped++ else readings += reading
        }
        return SafecastPage(readings, array.size, skipped)
    }

    private fun toReading(o: JsonObject, nowEpoch: Long): RadiationReading? {
        val value = o.double("value") ?: return null
        val lat = o.double("latitude") ?: return null
        val lon = o.double("longitude") ?: return null
        val captured = (o["captured_at"] as? JsonPrimitive)?.contentOrNull?.let(::parseEpoch) ?: return null
        if (captured > nowEpoch + FUTURE_TOLERANCE_S) return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        if (haversineKm(lat, lon, ZONE_CENTER_LAT, ZONE_CENTER_LON) > ZONE_RADIUS_KM + EDGE_MARGIN_KM) return null

        val unit = normalizeUnit((o["unit"] as? JsonPrimitive)?.contentOrNull)
        val (usv, fromCpm) = when (unit) {
            DoseUnit.MICROSIEVERT -> value to false
            DoseUnit.CPM -> (value / CPM_PER_MICROSV_H) to true
            DoseUnit.UNKNOWN -> return null
        }
        if (!usv.isFinite() || usv < 0.0 || usv > RADIATION_MAX_PLAUSIBLE_USVH) return null
        return RadiationReading(lat, lon, usv, captured, fromCpm)
    }

    private enum class DoseUnit { MICROSIEVERT, CPM, UNKNOWN }

    private fun normalizeUnit(raw: String?): DoseUnit =
        when (raw?.lowercase()?.filter { it.isLetterOrDigit() }) {
            "usv", "usvh", "µsv", "µsvh", "μsv", "μsvh", "microsievert", "microsieverts" -> DoseUnit.MICROSIEVERT
            "cpm" -> DoseUnit.CPM
            else -> DoseUnit.UNKNOWN
        }

    private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun parseEpoch(text: String): Long? =
        runCatching { OffsetDateTime.parse(text).toEpochSecond() }.getOrNull()
            ?: runCatching { Instant.parse(text).epochSecond }.getOrNull()
}
