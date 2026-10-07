package com.hamdash.chornobyl.data

import com.hamdash.chornobyl.domain.GameEntry
import com.hamdash.chornobyl.domain.GameFact
import com.hamdash.chornobyl.domain.LocalizedText
import com.hamdash.chornobyl.domain.PlaceCategory
import com.hamdash.chornobyl.domain.ZoneContent
import com.hamdash.chornobyl.domain.ZoneEvent
import com.hamdash.chornobyl.domain.ZonePlace
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class TextDto(val en: String = "", val uk: String = "")

@Serializable
internal data class EventDto(val date: String, val title: TextDto, val text: TextDto, val source: String = "")

@Serializable
internal data class PlaceDto(
    val id: String,
    val category: String,
    val lat: Double,
    val lon: Double,
    val approximate: Boolean = true,
    val name: TextDto,
    val text: TextDto,
    val stalker: TextDto? = null,
    val source: String = "",
)

@Serializable
internal data class GameDto(val year: Int, val title: String, val text: TextDto)

@Serializable
internal data class FactDto(val title: TextDto, val text: TextDto)

@Serializable
internal data class StalkerDto(val games: List<GameDto> = emptyList(), val facts: List<FactDto> = emptyList())

@Serializable
internal data class ZoneDto(
    val schemaVersion: Int = 1,
    val history: List<EventDto> = emptyList(),
    val places: List<PlaceDto> = emptyList(),
    val stalker: StalkerDto = StalkerDto(),
)

/** Parses the bundled zone.json. Entries without an English text or a stated source are dropped. */
object ZoneParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): ZoneContent {
        val dto = json.decodeFromString(ZoneDto.serializer(), text)
        return ZoneContent(
            history = dto.history
                .filter { it.title.en.isNotBlank() && it.source.startsWith("https://") }
                .map { ZoneEvent(it.date, it.title.text(), it.text.text(), it.source) },
            places = dto.places.mapNotNull { p ->
                val category = PlaceCategory.entries.firstOrNull { it.name == p.category } ?: return@mapNotNull null
                if (p.name.en.isBlank() || !p.source.startsWith("https://")) return@mapNotNull null
                if (p.lat !in -90.0..90.0 || p.lon !in -180.0..180.0) return@mapNotNull null
                ZonePlace(p.id, category, p.lat, p.lon, p.approximate, p.name.text(), p.text.text(), p.stalker?.text(), p.source)
            },
            games = dto.stalker.games.map { GameEntry(it.year, it.title, it.text.text()) },
            facts = dto.stalker.facts.map { GameFact(it.title.text(), it.text.text()) },
        )
    }

    private fun TextDto.text() = LocalizedText(en.trim(), uk.trim())
}
