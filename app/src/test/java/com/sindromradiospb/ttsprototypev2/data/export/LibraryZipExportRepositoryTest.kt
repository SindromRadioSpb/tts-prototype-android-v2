package com.sindromradiospb.ttsprototypev2.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindromradiospb.ttsprototypev2.core.model.TableModelMeta
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderProvenance
import com.sindromradiospb.ttsprototypev2.core.provider.TtsResponse
import com.sindromradiospb.ttsprototypev2.data.audio.AudioStorageRepository
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.db.AudioAssetEntity
import com.sindromradiospb.ttsprototypev2.data.db.RowAudioEntity
import com.sindromradiospb.ttsprototypev2.data.repository.GeneratedLibraryRowInput
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.SaveGeneratedTextRequest
import com.sindromradiospb.ttsprototypev2.data.repository.SaveTextResult
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class LibraryZipExportRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var libraryRepository: RoomLibraryRepository
    private lateinit var audioStorageRepository: AudioStorageRepository
    private lateinit var exportRepository: LibraryZipExportRepository
    private lateinit var appFilesDir: File
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appFilesDir = temporaryFolder.newFolder("files")
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        libraryRepository = RoomLibraryRepository(
            database = database,
            clock = { "2026-04-25T06:00:00Z" },
            idFactory = { "id-${nextId++}" },
        )
        audioStorageRepository = AudioStorageRepository(
            database = database,
            appFilesDir = appFilesDir,
            clock = { "2026-04-25T06:00:00Z" },
        )
        exportRepository = LibraryZipExportRepository(
            database = database,
            appFilesDir = appFilesDir,
            clock = { "2026-04-25T06:30:00Z" },
            idFactory = { "export-1" },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportZipContainsManifestLibraryAndAvailableAudio() = runTest {
        val saved = saveText()
        val row = saved.rows.single()
        val sourceAudio = temporaryFolder.newFile("row.wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        audioStorageRepository.adoptRowAudio(saved.id, row.id, sampleResponse(sourceAudio))

        val result = exportRepository.exportToZip(temporaryFolder.newFile("library.zip"))

        val entries = unzip(result.zipFile)
        val manifest = Json.parseToJsonElement(entries.string("manifest.json")).jsonObject
        val library = Json.parseToJsonElement(entries.string("library/library.json")).jsonObject
        val missingAudio = Json.parseToJsonElement(entries.string("metadata/missing_audio.json")).jsonObject
        val audioPath = "audio/rows/${saved.id}/${row.id}/asset-1.wav"

        assertFalse(manifest["partial_backup"]!!.jsonPrimitive.boolean)
        assertFalse(manifest["contains_secrets"]!!.jsonPrimitive.boolean)
        assertEquals(1, manifest["text_count"]!!.jsonPrimitive.int)
        assertEquals(1, manifest["row_count"]!!.jsonPrimitive.int)
        assertEquals(1, manifest["audio_count"]!!.jsonPrimitive.int)
        assertEquals("library/library.json", manifest["library_json_path"]!!.jsonPrimitive.content)
        assertEquals("שלום", library["texts"]!!.jsonArray[0].jsonObject["rows"]!!.jsonArray[0].jsonObject["hebrew_plain"]!!.jsonPrimitive.content)
        assertEquals("asset-1", library["audio_assets"]!!.jsonArray[0].jsonObject["asset_key"]!!.jsonPrimitive.content)
        assertEquals(0, missingAudio["missing_audio"]!!.jsonArray.size)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), entries[audioPath])
        assertEquals("success", database.libraryDao().getExportHistory().single().status)
    }

    @Test
    fun missingAudioCreatesPartialBackupInsteadOfFailingExport() = runTest {
        val saved = saveText()
        val row = saved.rows.single()
        val sourceAudio = temporaryFolder.newFile("row.wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val adopted = audioStorageRepository.adoptRowAudio(saved.id, row.id, sampleResponse(sourceAudio))
        assertTrue(adopted.absoluteFile.delete())

        val result = exportRepository.exportToZip(temporaryFolder.newFile("partial.zip"))

        val entries = unzip(result.zipFile)
        val manifest = Json.parseToJsonElement(entries.string("manifest.json")).jsonObject
        val missingAudio = Json.parseToJsonElement(entries.string("metadata/missing_audio.json")).jsonObject

        assertTrue(result.partialBackup)
        assertTrue(manifest["partial_backup"]!!.jsonPrimitive.boolean)
        assertEquals(0, manifest["audio_count"]!!.jsonPrimitive.int)
        assertEquals(1, manifest["missing_audio_count"]!!.jsonPrimitive.int)
        assertEquals("file_missing_in_app_storage", missingAudio["missing_audio"]!!.jsonArray[0].jsonObject["reason"]!!.jsonPrimitive.content)
        assertEquals(true, database.libraryDao().getExportHistory().single().partialBackup)
    }

    @Test
    fun unsafeAudioPathIsReportedAsMissingAndNotWrittenToZip() = runTest {
        val saved = saveText()
        val row = saved.rows.single()
        database.libraryDao().insertAudioAsset(
            AudioAssetEntity(
                assetKey = "asset-unsafe",
                fileName = "asset-unsafe.wav",
                relativePath = "../asset-unsafe.wav",
                mimeType = "audio/wav",
                providerId = "google_online_tts",
                voiceName = null,
                language = "he",
                durationMs = null,
                sizeBytes = null,
                contentHash = null,
                createdAt = "2026-04-25T06:00:00Z",
                provenanceJson = "{}",
                isMissing = false,
            ),
        )
        database.libraryDao().insertRowAudio(
            RowAudioEntity(
                rowId = row.id,
                assetKey = "asset-unsafe",
                isDefault = true,
                isStale = false,
                staleReason = null,
                createdAt = "2026-04-25T06:00:00Z",
            ),
        )

        val result = exportRepository.exportToZip(temporaryFolder.newFile("unsafe.zip"))

        val entries = unzip(result.zipFile)
        val missingAudio = Json.parseToJsonElement(entries.string("metadata/missing_audio.json")).jsonObject
        assertFalse(entries.containsKey("../asset-unsafe.wav"))
        assertEquals("unsafe_export_path", missingAudio["missing_audio"]!!.jsonArray[0].jsonObject["reason"]!!.jsonPrimitive.content)
    }

    private suspend fun saveText(): com.sindromradiospb.ttsprototypev2.core.model.LibraryText {
        val result = libraryRepository.saveGeneratedText(
            SaveGeneratedTextRequest(
                title = "Export test",
                sourceText = "שלום",
                tableModelMeta = TableModelMeta(
                    provider = TranslationProviderId.GoogleTranslateFree,
                    actualProvider = TranslationProviderId.GoogleTranslateFree,
                    generatedAt = "2026-04-25T06:00:00Z",
                ),
                rows = listOf(GeneratedLibraryRowInput(hebrewPlain = "שלום", russian = "привет")),
            ),
        )
        return (result as SaveTextResult.Saved).text
    }

    private fun sampleResponse(sourceFile: File): TtsResponse =
        TtsResponse(
            audioAssetKey = "asset-1",
            localFileName = sourceFile.name,
            localFilePath = sourceFile.absolutePath,
            mimeType = "audio/wav",
            provenance = ProviderProvenance(
                requestedProviderId = "system_or_browser_fallback_low_quality",
                actualProviderId = "system_or_browser_fallback_low_quality",
                generatedAt = "2026-04-25T06:00:00Z",
            ),
            sizeBytes = sourceFile.length(),
        )

    private fun unzip(file: File): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(file.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun Map<String, ByteArray>.string(path: String): String =
        String(requireNotNull(get(path)) { "Missing ZIP entry: $path" }, Charsets.UTF_8)
}
