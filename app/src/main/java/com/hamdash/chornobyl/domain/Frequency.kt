package com.hamdash.chornobyl.domain

import java.util.Locale
import kotlin.math.abs

private data class BandRange(val name: String, val startMHz: Double, val endMHz: Double)

// Used only to label a frequency with a familiar band name; it says nothing about what is allowed.
private val KNOWN_BANDS = listOf(
    BandRange("160 m", 1.8, 2.0),
    BandRange("80 m", 3.5, 4.0),
    BandRange("40 m", 7.0, 7.3),
    BandRange("30 m", 10.1, 10.15),
    BandRange("20 m", 14.0, 14.35),
    BandRange("17 m", 18.068, 18.168),
    BandRange("15 m", 21.0, 21.45),
    BandRange("12 m", 24.89, 24.99),
    BandRange("10 m", 28.0, 29.7),
    BandRange("6 m", 50.0, 54.0),
    BandRange("2 m", 144.0, 148.0),
    BandRange("70 cm", 420.0, 450.0),
    BandRange("23 cm", 1240.0, 1300.0),
)

fun bandNameFor(mhz: Double): String? = KNOWN_BANDS.firstOrNull { mhz in it.startMHz..it.endMHz }?.name

/** 438.4 -> "438.400", 145.6125 -> "145.6125" (at least 3, at most 5 decimals). */
fun formatMHz(mhz: Double): String {
    val raw = String.format(Locale.US, "%.5f", mhz).trimEnd('0')
    val decimals = raw.length - raw.indexOf('.') - 1
    return if (decimals >= 3) raw else raw + "0".repeat(3 - decimals)
}

fun formatOffset(offsetMHz: Double, mhzLabel: String = "MHz", simplexLabel: String = "simplex"): String {
    if (abs(offsetMHz) < 0.0005) return simplexLabel
    val sign = if (offsetMHz > 0) "+" else "-"
    return "$sign${formatMHz(abs(offsetMHz))} $mhzLabel"
}
