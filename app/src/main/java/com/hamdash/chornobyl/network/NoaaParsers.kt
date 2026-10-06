package com.hamdash.chornobyl.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.roundToInt

/** Defensive parsers for NOAA SWPC JSON products. Anything unexpected yields null, never a guess. */
object NoaaParsers {
    data class KIndex(val kp: Double, val aIndex: Int?, val time: String?)
    data class Flare(val flareClass: String, val time: String?)

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(body: String): JsonElement? =
        try {
            json.parseToJsonElement(body)
        } catch (e: Exception) {
            null
        }

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    /** `{"Flux":"145","TimeStamp":"..."}` from /products/summary/10cm-flux.json */
    fun parseSolarFlux(body: String): Int? {
        val root = parse(body)
        val obj = when (root) {
            is JsonObject -> root
            is JsonArray -> root.lastOrNull() as? JsonObject
            else -> null
        } ?: return null
        val value = (obj["Flux"] ?: obj["flux"]).text()?.toDoubleOrNull() ?: return null
        return if (value > 0) value.roundToInt() else null
    }

    /** /products/noaa-planetary-k-index.json: array of rows (with a header row) or array of objects. */
    fun parseKIndex(body: String): KIndex? {
        val root = parse(body) as? JsonArray ?: return null
        if (root.isEmpty()) return null

        val first = root[0]
        if (first is JsonArray) {
            val header = first.map { it.text()?.lowercase() }
            val kpCol = header.indexOfFirst { it == "kp" || it == "kp_index" }
            if (kpCol < 0) return null
            val aCol = header.indexOf("a_running")
            val timeCol = header.indexOf("time_tag")
            for (i in root.indices.reversed()) {
                if (i == 0) break
                val row = root[i] as? JsonArray ?: continue
                val kp = row.getOrNull(kpCol).text()?.toDoubleOrNull() ?: continue
                return KIndex(
                    kp = kp,
                    aIndex = if (aCol >= 0) row.getOrNull(aCol).text()?.toDoubleOrNull()?.roundToInt() else null,
                    time = if (timeCol >= 0) row.getOrNull(timeCol).text() else null,
                )
            }
            return null
        }

        for (element in root.reversed()) {
            val obj = element as? JsonObject ?: continue
            val kp = (obj["Kp"] ?: obj["kp_index"] ?: obj["kp"]).text()?.toDoubleOrNull() ?: continue
            return KIndex(
                kp = kp,
                aIndex = obj["a_running"].text()?.toDoubleOrNull()?.roundToInt(),
                time = obj["time_tag"].text(),
            )
        }
        return null
    }

    /** /json/goes/primary/xray-flares-latest.json: array of flare records. */
    fun parseFlare(body: String): Flare? {
        val root = parse(body) as? JsonArray ?: return null
        for (element in root.reversed()) {
            val obj = element as? JsonObject ?: continue
            val cls = (obj["max_class"] ?: obj["current_class"]).text() ?: continue
            val time = (obj["max_time"] ?: obj["time_tag"]).text()
            return Flare(cls, time)
        }
        return null
    }
}
