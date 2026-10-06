package com.hamdash.chornobyl.domain

import kotlinx.serialization.Serializable

/** Every field is null when the source did not provide it; the UI shows "NO DATA" instead of guessing. */
@Serializable
data class PropagationData(
    val solarFlux: Int? = null,
    val kIndex: Double? = null,
    val aIndex: Int? = null,
    val flareClass: String? = null,
    val flareTime: String? = null,
    val kIndexTime: String? = null,
    val fetchedAtMillis: Long = 0L,
) {
    val hasAnyValue: Boolean
        get() = solarFlux != null || kIndex != null || aIndex != null || flareClass != null
}

enum class GeomagneticState(val label: String) {
    QUIET("Quiet"),
    UNSETTLED("Unsettled"),
    ACTIVE("Active"),
    STORM_G1("Minor storm (G1)"),
    STORM_G2("Moderate storm (G2)"),
    STORM_G3("Strong storm (G3)"),
    STORM_G4("Severe storm (G4)"),
    STORM_G5("Extreme storm (G5)"),
}

fun geomagneticState(kp: Double?): GeomagneticState? = when {
    kp == null || kp < 0 -> null
    kp < 3 -> GeomagneticState.QUIET
    kp < 4 -> GeomagneticState.UNSETTLED
    kp < 5 -> GeomagneticState.ACTIVE
    kp < 6 -> GeomagneticState.STORM_G1
    kp < 7 -> GeomagneticState.STORM_G2
    kp < 8 -> GeomagneticState.STORM_G3
    kp < 9 -> GeomagneticState.STORM_G4
    else -> GeomagneticState.STORM_G5
}

data class PropagationSnapshot(
    val data: PropagationData? = null,
    val loading: Boolean = false,
    val error: String? = null,
)
