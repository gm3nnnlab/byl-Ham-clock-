package com.hamdash.chornobyl.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repeaters")
data class RepeaterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val callsign: String,
    val rxMHz: Double,
    val txMHz: Double,
    val mode: String,
    val ctcssHz: Double?,
    val colorCode: Int?,
    val timeslot: Int?,
    val talkgroupIds: String,
    val latitude: Double,
    val longitude: Double,
    val elevationM: Int?,
    val source: String,
    val lastUpdated: String,
    val status: String,
)

@Entity(tableName = "talkgroups")
data class TalkgroupEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val source: String,
)

@Entity(tableName = "bands")
data class BandEntity(
    @PrimaryKey val name: String,
    val startMHz: Double,
    val endMHz: Double,
    val typicalModes: String,
    val purpose: String,
    val notes: String,
)

@Entity(tableName = "digital_modes")
data class DigitalModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val summary: String,
    val principle: String,
    val uses: String,
    val equipment: String,
    val links: String,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val repeaterId: String)
