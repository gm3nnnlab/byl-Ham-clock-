package com.hamdash.chornobyl.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM repeaters ORDER BY callsign COLLATE NOCASE")
    fun observeRepeaters(): Flow<List<RepeaterEntity>>

    @Query("SELECT * FROM talkgroups ORDER BY id")
    fun observeTalkgroups(): Flow<List<TalkgroupEntity>>

    @Query("SELECT * FROM bands ORDER BY startMHz")
    fun observeBands(): Flow<List<BandEntity>>

    @Query("SELECT * FROM digital_modes ORDER BY rowid")
    fun observeDigitalModes(): Flow<List<DigitalModeEntity>>

    @Query(
        "SELECT (SELECT COUNT(*) FROM repeaters) + (SELECT COUNT(*) FROM talkgroups) + " +
            "(SELECT COUNT(*) FROM bands) + (SELECT COUNT(*) FROM digital_modes)",
    )
    suspend fun totalRows(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepeaters(items: List<RepeaterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTalkgroups(items: List<TalkgroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBands(items: List<BandEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDigitalModes(items: List<DigitalModeEntity>)

    @Query("DELETE FROM repeaters")
    suspend fun deleteRepeaters()

    @Query("DELETE FROM talkgroups")
    suspend fun deleteTalkgroups()

    @Query("DELETE FROM bands")
    suspend fun deleteBands()

    @Query("DELETE FROM digital_modes")
    suspend fun deleteDigitalModes()
}

@Dao
interface FavoriteDao {
    @Query("SELECT repeaterId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE repeaterId = :repeaterId")
    suspend fun remove(repeaterId: String)
}
