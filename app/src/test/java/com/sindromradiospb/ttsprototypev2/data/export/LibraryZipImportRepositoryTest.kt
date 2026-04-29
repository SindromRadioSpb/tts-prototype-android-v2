package com.sindromradiospb.ttsprototypev2.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryZipImportRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomLibraryRepository
    private lateinit var importRepository: LibraryZipImportRepository
    private lateinit var appFilesDir: File
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appFilesDir = temporaryFolder.newFolder("files")
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLibraryRepository(
            database = database,
            clock = { "2026-04-29T10:00:00Z" },
            idFactory = { "id-${nextId++}" },
        )
        importRepository = LibraryZipImportRepository(
            database = database,
            appFilesDir = appFilesDir,
            clock = { "2026-04-29T10:00:00Z" },
            idFactory = { "id-${nextId++}" },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importZipRestoresBundledAudioProfileAndVisibleAudioCounts() = runTest {
        val assetKey = "a".repeat(64)

        val result = importRepository.importFromZip(ByteArrayInputStream(zipBundle(assetKey)))
        val summary = repository.observeTexts(includeArchived = true).first().single()
        val text = repository.getText(summary.textId)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.importedAudio)
        assertEquals(1, result.linkedAudio)
        assertEquals(1, summary.rowCount)
        assertEquals(1, summary.linkedAudioCount)
        assertTrue(File(appFilesDir, "audio/$assetKey.mp3").exists())
        assertEquals(assetKey, text.rows.single().audioAssetKey)
        assertEquals(TtsProviderId.GoogleOnlineTts, text.ttsProfile?.providerId)
        assertEquals(TtsProviderId.GoogleOnlineTts, text.rows.single().audioTtsProfile?.providerId)
        assertEquals("he-IL-Standard-A", text.ttsProfile?.voiceName)
        assertEquals(0.9, text.ttsProfile?.speakingRate ?: 0.0, 0.001)
        assertEquals(2.5, text.ttsProfile?.pitch ?: 0.0, 0.001)
        assertTrue(text.tableModelMetaLabel.orEmpty().contains("model=gemini-flash-latest"))
    }

    private fun zipBundle(assetKey: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("library/library.json"))
            zip.write(libraryJson(assetKey).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("audio/$assetKey.mp3"))
            zip.write(byteArrayOf(1, 2, 3, 4))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun libraryJson(assetKey: String): String =
        """
        {
          "schema_version": 1,
          "texts": [
            {
              "text_id": "web-text-1",
              "text_key": "web-key-1",
              "title": "Imported top100",
              "level": "alef",
              "tags": ["mako"],
              "source_label": "https://example.com/source",
              "topic": "article",
              "source_text": "שלום",
              "source_meta": null,
              "table_model_meta": {
                "promptId": "he-ru-table-v1",
                "model": "gemini-flash-latest",
                "generatedAt": "2026-01-07T00:44:08.820Z"
              },
              "text_audio_asset_key": null,
              "created_at": "2026-04-25T10:00:00Z",
              "updated_at": "2026-04-25T10:10:00Z",
              "is_archived": false,
              "rows": [
                {
                  "row_id": "web-row-1",
                  "order_index": 0,
                  "hebrew_plain": "שלום",
                  "hebrew_niqqud": "שָׁלוֹם",
                  "translit": "shalom",
                  "translit_ru": "шалом",
                  "russian": "привет",
                  "edit_meta": null,
                  "audio_asset_key": "$assetKey"
                }
              ]
            }
          ],
          "audio_assets": [
            {
              "asset_key": "$assetKey",
              "relative_export_path": "audio/$assetKey.mp3",
              "mime_type": "audio/mpeg",
              "provider_id": "unknown",
              "voice_name": "he-IL-Standard-A",
              "language": "he-IL",
              "duration_ms": 1200,
              "size_bytes": 4,
              "content_hash": "$assetKey",
              "provenance": {
                "ttsProfile": {
                  "language": "he-IL",
                  "voiceName": "he-IL-Standard-A",
                  "speakingRate": 0.9,
                  "pitch": 2.5
                }
              }
            }
          ]
        }
        """.trimIndent()
}
