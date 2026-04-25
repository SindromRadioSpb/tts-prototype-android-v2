package com.sindromradiospb.ttsprototypev2.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseMigrationTest {
    @Test
    fun migration1To2AddsLibraryV3MetadataColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DB)
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )

        val db = openHelper.writableDatabase
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `library_texts` (
                    `text_id` TEXT NOT NULL,
                    `text_key` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `level` TEXT,
                    `tags_json` TEXT NOT NULL,
                    `source_text` TEXT NOT NULL,
                    `source_meta_json` TEXT,
                    `table_model_meta_json` TEXT,
                    `tts_profile_json` TEXT,
                    `is_archived` INTEGER NOT NULL,
                    `created_at` TEXT NOT NULL,
                    `updated_at` TEXT NOT NULL,
                    `last_opened_at` TEXT,
                    `schema_version` INTEGER NOT NULL,
                    PRIMARY KEY(`text_id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO library_texts (
                    text_id, text_key, title, level, tags_json, source_text,
                    source_meta_json, table_model_meta_json, tts_profile_json,
                    is_archived, created_at, updated_at, last_opened_at, schema_version
                ) VALUES (
                    'txt-1', 'key-1', 'Greeting', NULL, '[]', 'שלום',
                    NULL, NULL, NULL, 0, '2026-04-25T00:00:00Z',
                    '2026-04-25T00:00:00Z', NULL, 1
                )
                """.trimIndent(),
            )

            AppDatabase.MIGRATION_1_2.migrate(db)

            db.query("SELECT title, source_label, topic FROM library_texts WHERE text_id = 'txt-1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Greeting", cursor.getString(0))
                assertNull(cursor.getString(1))
                assertNull(cursor.getString(2))
            }
        } finally {
            db.close()
            openHelper.close()
            context.deleteDatabase(TEST_DB)
        }
    }

    @Test
    fun migration2To3AddsSentenceNotesTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DB_2_3)
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB_2_3)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )

        val db = openHelper.writableDatabase
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `library_texts` (
                    `text_id` TEXT NOT NULL,
                    `text_key` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `level` TEXT,
                    `tags_json` TEXT NOT NULL,
                    `source_label` TEXT,
                    `topic` TEXT,
                    `source_text` TEXT NOT NULL,
                    `source_meta_json` TEXT,
                    `table_model_meta_json` TEXT,
                    `tts_profile_json` TEXT,
                    `is_archived` INTEGER NOT NULL,
                    `created_at` TEXT NOT NULL,
                    `updated_at` TEXT NOT NULL,
                    `last_opened_at` TEXT,
                    `schema_version` INTEGER NOT NULL,
                    PRIMARY KEY(`text_id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `library_rows` (
                    `row_id` TEXT NOT NULL,
                    `text_id` TEXT NOT NULL,
                    `order_index` INTEGER NOT NULL,
                    `hebrew_plain` TEXT NOT NULL,
                    `hebrew_niqqud` TEXT NOT NULL,
                    `translit` TEXT NOT NULL,
                    `translit_ru` TEXT NOT NULL,
                    `russian` TEXT NOT NULL,
                    `row_hash` TEXT,
                    `edit_meta_json` TEXT,
                    `source_meta_json` TEXT,
                    `created_at` TEXT NOT NULL,
                    `updated_at` TEXT NOT NULL,
                    PRIMARY KEY(`row_id`),
                    FOREIGN KEY(`text_id`) REFERENCES `library_texts`(`text_id`) ON DELETE CASCADE
                )
                """.trimIndent(),
            )

            AppDatabase.MIGRATION_2_3.migrate(db)

            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='sentence_notes'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("sentence_notes", cursor.getString(0))
            }
            db.query("PRAGMA index_list('sentence_notes')").use { cursor ->
                var hasUnique = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == "index_sentence_notes_text_id_sentence_id" && cursor.getInt(2) == 1) {
                        hasUnique = true
                    }
                }
                assertTrue(hasUnique)
            }
        } finally {
            db.close()
            openHelper.close()
            context.deleteDatabase(TEST_DB_2_3)
        }
    }

    private companion object {
        const val TEST_DB = "migration-1-2"
        const val TEST_DB_2_3 = "migration-2-3"
    }
}
