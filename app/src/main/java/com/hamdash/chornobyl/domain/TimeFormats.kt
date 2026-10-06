package com.hamdash.chornobyl.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormats {
    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    private val hourMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    fun clock(instant: Instant, zone: ZoneId): String = clockFormatter.withZone(zone).format(instant)

    fun hourMinute(instant: Instant, zone: ZoneId): String = hourMinuteFormatter.withZone(zone).format(instant)

    fun dateTime(instant: Instant, zone: ZoneId): String = dateTimeFormatter.withZone(zone).format(instant)

    fun date(instant: Instant, zone: ZoneId, locale: Locale = Locale.ENGLISH): String =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale).withZone(zone).format(instant)

    fun duration(duration: Duration): String {
        val minutes = duration.toMinutes()
        return "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"
    }
}

object TimeZoneOptions {
    const val SYSTEM = "system"

    val ids: List<String> = listOf(
        SYSTEM,
        "Europe/Kyiv",
        "Europe/Warsaw",
        "Europe/Berlin",
        "Europe/London",
        "America/New_York",
    )

    fun resolve(id: String): ZoneId =
        if (id == SYSTEM) ZoneId.systemDefault() else runCatching { ZoneId.of(id) }.getOrElse { ZoneId.systemDefault() }

    fun label(id: String): String = if (id == SYSTEM) "Device default (${ZoneId.systemDefault().id})" else id
}
