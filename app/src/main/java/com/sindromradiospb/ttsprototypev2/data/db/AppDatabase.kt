package com.sindromradiospb.ttsprototypev2.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LibraryTextEntity::class,
        LibraryRowEntity::class,
        AudioAssetEntity::class,
        RowAudioEntity::class,
        TextAudioEntity::class,
        ExportHistoryEntity::class,
        ProviderCallLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE library_texts ADD COLUMN source_label TEXT")
                db.execSQL("ALTER TABLE library_texts ADD COLUMN topic TEXT")
            }
        }
    }
}
