package com.hamdash.chornobyl.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

sealed interface SunResult {
    data class Normal(val sunrise: Instant, val sunset: Instant) : SunResult {
        val dayLength: Duration get() = Duration.between(sunrise, sunset)
    }

    data object AlwaysUp : SunResult
    data object AlwaysDown : SunResult
}

/** Sunrise/sunset from the Almanac for Computers algorithm; accurate to a couple of minutes. */
object SunCalculator {
    private const val OFFICIAL_ZENITH = 90.833

    fun calculate(date: LocalDate, latitude: Double, longitude: Double): SunResult {
        val rise = eventHoursUtc(date, latitude, longitude, rising = true)
        val set = eventHoursUtc(date, latitude, longitude, rising = false)
        if (rise is Event.Never) return if (rise.alwaysUp) SunResult.AlwaysUp else SunResult.AlwaysDown
        if (set is Event.Never) return if (set.alwaysUp) SunResult.AlwaysUp else SunResult.AlwaysDown
        val midnight = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        var sunrise = midnight.plusMillis(((rise as Event.At).utHours * 3_600_000).toLong())
        var sunset = midnight.plusMillis(((set as Event.At).utHours * 3_600_000).toLong())
        if (sunset.isBefore(sunrise)) sunset = sunset.plus(Duration.ofDays(1))
        if (Duration.between(sunrise, sunset) > Duration.ofHours(24)) sunrise = sunrise.plus(Duration.ofDays(1))
        return SunResult.Normal(sunrise, sunset)
    }

    private sealed interface Event {
        data class At(val utHours: Double) : Event
        data class Never(val alwaysUp: Boolean) : Event
    }

    private fun eventHoursUtc(date: LocalDate, lat: Double, lon: Double, rising: Boolean): Event {
        val lngHour = lon / 15.0
        val t = date.dayOfYear + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0
        val meanAnomaly = 0.9856 * t - 3.289
        val trueLongitude = norm(
            meanAnomaly + 1.916 * sinD(meanAnomaly) + 0.020 * sinD(2 * meanAnomaly) + 282.634,
            360.0,
        )
        var rightAscension = norm(Math.toDegrees(atan(0.91764 * tanD(trueLongitude))), 360.0)
        val lQuadrant = floor(trueLongitude / 90.0) * 90.0
        val raQuadrant = floor(rightAscension / 90.0) * 90.0
        rightAscension = (rightAscension + (lQuadrant - raQuadrant)) / 15.0

        val sinDec = 0.39782 * sinD(trueLongitude)
        val cosDec = cos(asin(sinDec))
        val cosH = (cosD(OFFICIAL_ZENITH) - sinDec * sinD(lat)) / (cosDec * cosD(lat))
        if (cosH > 1) return Event.Never(alwaysUp = false)
        if (cosH < -1) return Event.Never(alwaysUp = true)

        val hourAngle = (if (rising) 360.0 - Math.toDegrees(acos(cosH)) else Math.toDegrees(acos(cosH))) / 15.0
        val localMeanTime = hourAngle + rightAscension - 0.06571 * t - 6.622
        return Event.At(norm(localMeanTime - lngHour, 24.0))
    }

    private fun norm(value: Double, range: Double): Double {
        val r = value % range
        return if (r < 0) r + range else r
    }

    private fun sinD(deg: Double) = sin(Math.toRadians(deg))
    private fun cosD(deg: Double) = cos(Math.toRadians(deg))
    private fun tanD(deg: Double) = tan(Math.toRadians(deg))
}
