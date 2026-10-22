package com.hamdash.chornobyl.domain

import kotlin.math.roundToLong

/**
 * Horizontal marker anchors (as a fraction of the icon width) that place markers sharing the same coordinates
 * side by side, so none of them hides another. The coordinates themselves stay exact; markers alone sit at 0.5.
 */
fun sideBySideAnchors(points: List<Pair<Double, Double>>, step: Float = 1.15f): List<Float> {
    val groups = points.indices.groupBy { i -> key(points[i]) }
    val anchors = FloatArray(points.size) { 0.5f }
    groups.values.forEach { members ->
        val middle = (members.size - 1) / 2f
        members.forEachIndexed { slot, index -> anchors[index] = 0.5f - (slot - middle) * step }
    }
    return anchors.toList()
}

// About one metre: listings that repeat a city centre point land in the same group.
private fun key(p: Pair<Double, Double>) = (p.first * 1e5).roundToLong() to (p.second * 1e5).roundToLong()
