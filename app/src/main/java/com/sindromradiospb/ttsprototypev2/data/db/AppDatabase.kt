package com.sindromradiospb.ttsprototypev2.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LibraryTextEntity::class,
        LibraryRowEntity::class,
        SentenceNoteEntity::class,
        AudioAssetEntity::class,
        RowAudioEntity::class,
        TextAudioEntity::class,
        ExportHistoryEntity::class,
        ProviderCallLogEntity::class,
    ],
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sentence_notes (
                        note_id TEXT NOT NULL PRIMARY KEY,
                        text_id TEXT NOT NULL,
                        sentence_id TEXT NOT NULL,
                        note TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY(text_id) REFERENCES library_texts(text_id) ON DELETE CASCADE,
                        FOREIGN KEY(sentence_id) REFERENCES library_rows(row_id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentence_notes_sentence_id ON sentence_notes(sentence_id)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_sentence_notes_text_id_sentence_id ON sentence_notes(text_id, sentence_id)",
                )
            }
        }
    }
}
