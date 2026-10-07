package com.hamdash.chornobyl.domain

import kotlinx.serialization.Serializable
import kotlin.math.floor

/** Readings are grouped into cells of this size (about 2 km), so single survey tracks never show as lines. */
const val RADIATION_CELL_DEGREES = 0.02

/** Safecast bGeigie Nano calibration: 334 counts per minute correspond to 1 uSv/h. Other detectors differ. */
const val CPM_PER_MICROSV_H = 334.0

const val RADIATION_WINDOW_DAYS = 365

/** Anything above this is treated as a faulty reading, not a measurement. */
const val RADIATION_MAX_PLAUSIBLE_USVH = 10_000.0

/** One dose-rate reading in uSv/h. [fromCpm] marks values converted from counts per minute (estimates). */
data class RadiationReading(
    val latitude: Double,
    val longitude: Double,
    val microSvPerHour: Double,
    val capturedEpoch: Long,
    val fromCpm: Boolean,
)

@Serializable
data class RadiationCell(
    val latitude: Double,
    val longitude: Double,
    val medianUsvH: Double,
    val maxUsvH: Double,
    val count: Int,
    val latestEpoch: Long,
    val estimated: Boolean,
)

@Serializable
data class RadiationData(
    val cells: List<RadiationCell>,
    val readingsUsed: Int,
    val readingsSkipped: Int,
    val fetchedAtMillis: Long,
) {
    val highest: RadiationCell? get() = cells.maxByOrNull { it.medianUsvH }
    val newestEpoch: Long? get() = cells.maxOfOrNull { it.latestEpoch }
}

enum class RadiationProblem { OFFLINE, METERED_BLOCKED, UNREACHABLE, UNREADABLE, EMPTY, NO_USABLE }

data class RadiationSnapshot(
    val data: RadiationData? = null,
    val loading: Boolean = false,
    val problem: RadiationProblem? = null,
    val received: Int = 0,
    val skipped: Int = 0,
)

/** Value ranges for the map colors. They only group numbers and are not safety limits. */
enum class RadiationBand(val upperUsvH: Double) {
    UP_TO_0_3(0.3),
    UP_TO_1(1.0),
    UP_TO_10(10.0),
    OVER_10(Double.POSITIVE_INFINITY),
}

fun radiationBand(usvH: Double): RadiationBand =
    RadiationBand.entries.first { usvH <= it.upperUsvH }

fun median(values: List<Double>): Double {
    require(values.isNotEmpty())
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
}

/** Groups readings into grid cells; the result is sorted so the highest cells come last (drawn on top). */
fun aggregateRadiation(
    readings: List<RadiationReading>,
    cellDegrees: Double = RADIATION_CELL_DEGREES,
): List<RadiationCell> =
    readings
        .groupBy { Pair(floor(it.latitude / cellDegrees).toLong(), floor(it.longitude / cellDegrees).toLong()) }
        .map { (key, group) ->
            val values = group.map { it.microSvPerHour }
            RadiationCell(
                latitude = round4((key.first + 0.5) * cellDegrees),
                longitude = round4((key.second + 0.5) * cellDegrees),
                medianUsvH = median(values),
                maxUsvH = values.max(),
                count = group.size,
                latestEpoch = group.maxOf { it.capturedEpoch },
                estimated = group.any { it.fromCpm },
            )
        }
        .sortedBy { it.medianUsvH }

private fun round4(v: Double): Double = Math.round(v * 10_000.0) / 10_000.0
