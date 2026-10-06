package com.hamdash.chornobyl.database

import com.hamdash.chornobyl.domain.Band
import com.hamdash.chornobyl.domain.DigitalMode
import com.hamdash.chornobyl.domain.DocLink
import com.hamdash.chornobyl.domain.RadioMode
import com.hamdash.chornobyl.domain.Repeater
import com.hamdash.chornobyl.domain.Talkgroup

fun Repeater.toEntity() = RepeaterEntity(
    id = id, name = name, callsign = callsign, rxMHz = rxMHz, txMHz = txMHz, mode = mode.name,
    ctcssHz = ctcssHz, colorCode = colorCode, timeslot = timeslot,
    talkgroupIds = talkgroupIds.joinToString(","),
    latitude = latitude, longitude = longitude, elevationM = elevationM,
    source = source, lastUpdated = lastUpdated,
)

fun RepeaterEntity.toDomain() = Repeater(
    id = id, name = name, callsign = callsign, rxMHz = rxMHz, txMHz = txMHz,
    mode = RadioMode.parse(mode), ctcssHz = ctcssHz, colorCode = colorCode, timeslot = timeslot,
    talkgroupIds = talkgroupIds.split(',').mapNotNull { it.trim().toIntOrNull() },
    latitude = latitude, longitude = longitude, elevationM = elevationM,
    source = source, lastUpdated = lastUpdated,
)

fun Talkgroup.toEntity() = TalkgroupEntity(id, name, description, source)
fun TalkgroupEntity.toDomain() = Talkgroup(id, name, description, source)

fun Band.toEntity() = BandEntity(name, startMHz, endMHz, typicalModes, purpose, notes)
fun BandEntity.toDomain() = Band(name, startMHz, endMHz, typicalModes, purpose, notes)

fun DigitalMode.toEntity() = DigitalModeEntity(
    id = id, name = name, summary = summary, principle = principle, uses = uses, equipment = equipment,
    links = links.joinToString("\n") { "${it.title}\t${it.url}" },
)

fun DigitalModeEntity.toDomain() = DigitalMode(
    id = id, name = name, summary = summary, principle = principle, uses = uses, equipment = equipment,
    links = links.lineSequence().mapNotNull { line ->
        val parts = line.split('\t', limit = 2)
        if (parts.size == 2 && parts[1].isNotBlank()) DocLink(parts[0], parts[1]) else null
    }.toList(),
)
