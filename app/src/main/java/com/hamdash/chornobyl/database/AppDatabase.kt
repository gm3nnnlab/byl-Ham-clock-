package com.hamdash.chornobyl.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RepeaterEntity::class,
        TalkgroupEntity::class,
        BandEntity::class,
        DigitalModeEntity::class,
        FavoriteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "hamdash.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
