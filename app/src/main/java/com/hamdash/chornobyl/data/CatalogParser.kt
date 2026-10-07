package com.hamdash.chornobyl.data

import com.hamdash.chornobyl.domain.Band
import com.hamdash.chornobyl.domain.DigitalMode
import com.hamdash.chornobyl.domain.DocLink
import com.hamdash.chornobyl.domain.RadioCatalog
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.RepeaterStatus
import com.hamdash.chornobyl.domain.SyncProblem
import com.hamdash.chornobyl.domain.Talkgroup
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
internal data class RepeaterDto(
    val id: String,
    val name: String = "",
    val callsign: String,
    val rxMHz: Double,
    val txMHz: Double,
    val mode: String = "",
    val ctcssHz: Double? = null,
    val colorCode: Int? = null,
    val timeslot: Int? = null,
    val talkgroups: List<Int> = emptyList(),
    val lat: Double,
    val lon: Double,
    val elevationM: Int? = null,
    val source: String = "",
    val lastUpdated: String = "",
    val status: String = "",
)

@Serializable
internal data class TalkgroupDto(
    val id: Int,
    val name: String,
    val description: String = "",
    val source: String = "",
)

@Serializable
internal data class BandDto(
    val name: String,
    val startMHz: Double,
    val endMHz: Double,
    val typicalModes: String = "",
    val purpose: String = "",
    val notes: String = "",
)

@Serializable
internal data class DocLinkDto(val title: String, val url: String)

@Serializable
internal data class DigitalModeDto(
    val id: String,
    val name: String,
    val summary: String = "",
    val principle: String = "",
    val uses: String = "",
    val equipment: String = "",
    val links: List<DocLinkDto> = emptyList(),
)

class CatalogFormatException(val problem: SyncProblem) : Exception(problem.encode())

data class ParsedCatalog(val catalog: RadioCatalog, val rejected: List<String>)

/**
 * Parses and validates a catalog document (bundled asset or remote response).
 * Invalid items are dropped one by one and reported; a malformed document throws [CatalogFormatException].
 */
object CatalogParser {
    const val SUPPORTED_SCHEMA = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): ParsedCatalog {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw CatalogFormatException(SyncProblem.NotJson)
        } catch (e: IllegalArgumentException) {
            throw CatalogFormatException(SyncProblem.NotJson)
        }
        val obj = root as? JsonObject ?: throw CatalogFormatException(SyncProblem.NotObject)

        val schema = (obj["schemaVersion"] as? JsonPrimitive)?.intOrNull ?: 1
        if (schema > SUPPORTED_SCHEMA) {
            throw CatalogFormatException(SyncProblem.NewerSchema(schema, SUPPORTED_SCHEMA))
        }

        val rejected = mutableListOf<String>()

        fun <D, T> section(
            name: String,
            serializer: KSerializer<D>,
            map: (D) -> T,
            validate: (T) -> String?,
        ): List<T> {
            val array = obj[name] as? JsonArray ?: return emptyList()
            val result = ArrayList<T>(array.size)
            array.forEachIndexed { index, element: JsonElement ->
                val item = try {
                    map(json.decodeFromJsonElement(serializer, element))
                } catch (e: Exception) {
                    rejected += "$name[$index]: unreadable (${e.message?.lineSequence()?.firstOrNull() ?: "invalid"})"
                    return@forEachIndexed
                }
                val problem = validate(item)
                if (problem != null) rejected += "$name[$index]: $problem" else result += item
            }
            return result
        }

        val repeaters = section("repeaters", RepeaterDto.serializer(), ::toRepeater, CatalogValidator::validateRepeater)
            .distinctBy { it.id }
        val talkgroups = section("talkgroups", TalkgroupDto.serializer(), ::toTalkgroup, CatalogValidator::validateTalkgroup)
            .distinctBy { it.id }
        val bands = section("bands", BandDto.serializer(), ::toBand, CatalogValidator::validateBand)
            .distinctBy { it.name }
        val modes = section("digitalModes", DigitalModeDto.serializer(), ::toDigitalMode, CatalogValidator::validateDigitalMode)
            .distinctBy { it.id }

        val updated = (obj["updated"] as? JsonPrimitive)?.contentOrNull
        return ParsedCatalog(RadioCatalog(updated, repeaters, talkgroups, bands, modes), rejected)
    }

    private fun toRepeater(d: RepeaterDto) = Repeater(
        id = d.id.trim(),
        name = d.name.trim().ifEmpty { d.callsign.trim() },
        callsign = d.callsign.trim(),
        rxMHz = d.rxMHz,
        txMHz = d.txMHz,
        mode = RadioMode.parse(d.mode),
        ctcssHz = d.ctcssHz,
        colorCode = d.colorCode,
        timeslot = d.timeslot,
        talkgroupIds = d.talkgroups,
        latitude = d.lat,
        longitude = d.lon,
        elevationM = d.elevationM,
        source = d.source.trim(),
        lastUpdated = d.lastUpdated.trim(),
        status = RepeaterStatus.parse(d.status),
    )

    private fun toTalkgroup(d: TalkgroupDto) =
        Talkgroup(d.id, d.name.trim(), d.description.trim(), d.source.trim())

    private fun toBand(d: BandDto) =
        Band(d.name.trim(), d.startMHz, d.endMHz, d.typicalModes.trim(), d.purpose.trim(), d.notes.trim())

    private fun toDigitalMode(d: DigitalModeDto) = DigitalMode(
        id = d.id.trim(),
        name = d.name.trim(),
        summary = d.summary.trim(),
        principle = d.principle.trim(),
        uses = d.uses.trim(),
        equipment = d.equipment.trim(),
        links = d.links.map { DocLink(it.title.trim(), it.url.trim()) }.filter { it.url.startsWith("https://") },
    )
}

object CatalogValidator {
    fun validateRepeater(r: Repeater): String? = when {
        r.id.isEmpty() -> "missing id"
        r.callsign.isEmpty() -> "missing callsign"
        r.source.isEmpty() -> "unknown data source (entries without a stated origin are not shown)"
        r.rxMHz <= 0.0 || r.txMHz <= 0.0 -> "frequencies must be positive"
        r.latitude !in -90.0..90.0 || r.longitude !in -180.0..180.0 -> "coordinates out of range"
        r.colorCode != null && r.colorCode !in 0..15 -> "color code must be 0-15"
        r.timeslot != null && r.timeslot !in 1..2 -> "timeslot must be 1 or 2"
        r.ctcssHz != null && r.ctcssHz <= 0.0 -> "CTCSS tone must be positive"
        else -> null
    }

    fun validateTalkgroup(t: Talkgroup): String? = when {
        t.id <= 0 -> "talkgroup id must be positive"
        t.name.isEmpty() -> "missing name"
        t.source.isEmpty() -> "unknown data source"
        else -> null
    }

    fun validateBand(b: Band): String? = when {
        b.name.isEmpty() -> "missing name"
        b.startMHz <= 0.0 || b.endMHz <= b.startMHz -> "invalid frequency range"
        else -> null
    }

    fun validateDigitalMode(m: DigitalMode): String? = when {
        m.id.isEmpty() || m.name.isEmpty() -> "missing id or name"
        else -> null
    }
}
