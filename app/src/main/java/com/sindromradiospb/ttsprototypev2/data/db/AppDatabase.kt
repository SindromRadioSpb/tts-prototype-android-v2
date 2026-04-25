package com.sindromradiospb.ttsprototypev2.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

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
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
