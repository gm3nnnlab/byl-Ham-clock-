package com.hamdash.chornobyl.domain

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class DistanceUnit(val label: String, val suffix: String, val perKm: Double) {
    KM("Kilometers", "km", 1.0),
    MI("Miles", "mi", 0.621371),
}

/** Fallback point for sunrise/sunset when the user's position is not available: Chornobyl town. */
const val DEFAULT_LATITUDE = 51.2763
const val DEFAULT_LONGITUDE = 30.2219

fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0088
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * earthRadiusKm * asin(sqrt(a))
}

fun formatDistance(km: Double, unit: DistanceUnit, suffix: String = unit.suffix): String {
    val value = km * unit.perKm
    val text = if (value < 10) String.format(Locale.US, "%.1f", value) else String.format(Locale.US, "%.0f", value)
    return "$text $suffix"
}
