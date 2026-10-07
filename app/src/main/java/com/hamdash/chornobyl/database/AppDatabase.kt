package com.hamdash.chornobyl.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RepeaterEntity::class,
        TalkgroupEntity::class,
        BandEntity::class,
        DigitalModeEntity::class,
        FavoriteEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        /** Adds the on-air status; existing rows become UNKNOWN, favorites are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `repeaters` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "hamdash.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
    }
}
