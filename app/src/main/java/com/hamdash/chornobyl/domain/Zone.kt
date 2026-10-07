package com.hamdash.chornobyl.domain

/** Text available in English and Ukrainian; falls back to English when a translation is missing. */
data class LocalizedText(val en: String, val uk: String = "") {
    fun pick(languageTag: String): String = if (languageTag == "uk" && uk.isNotBlank()) uk else en
}

data class ZoneEvent(
    val date: String,
    val title: LocalizedText,
    val text: LocalizedText,
    val source: String,
)

enum class PlaceCategory { NUCLEAR, TOWN, RADIO, NATURE, MEMORIAL }

/** Public, widely documented civilian or historical landmark. [approximate] marks area-level coordinates. */
data class ZonePlace(
    val id: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val approximate: Boolean,
    val name: LocalizedText,
    val text: LocalizedText,
    val stalker: LocalizedText?,
    val source: String,
)

data class GameEntry(val year: Int, val title: String, val text: LocalizedText)

data class GameFact(val title: LocalizedText, val text: LocalizedText)

data class ZoneContent(
    val history: List<ZoneEvent> = emptyList(),
    val places: List<ZonePlace> = emptyList(),
    val games: List<GameEntry> = emptyList(),
    val facts: List<GameFact> = emptyList(),
)

/** Reference point for the approximate 30 km exclusion radius: the power plant. */
const val ZONE_CENTER_LAT = 51.3894
const val ZONE_CENTER_LON = 30.0997
const val ZONE_RADIUS_KM = 30.0

fun isInsideZoneRadius(lat: Double, lon: Double): Boolean =
    haversineKm(lat, lon, ZONE_CENTER_LAT, ZONE_CENTER_LON) <= ZONE_RADIUS_KM
