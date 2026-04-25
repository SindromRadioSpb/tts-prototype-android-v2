package com.sindromradiospb.ttsprototypev2.data.audio

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindromradiospb.ttsprototypev2.core.model.TableModelMeta
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderProvenance
import com.sindromradiospb.ttsprototypev2.core.provider.TtsResponse
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.repository.GeneratedLibraryRowInput
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.SaveGeneratedTextRequest
import com.sindromradiospb.ttsprototypev2.data.repository.SaveTextResult
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
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
class AudioStorageRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var libraryRepository: RoomLibraryRepository
    private lateinit var audioStorageRepository: AudioStorageRepository
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        libraryRepository = RoomLibraryRepository(
            database = database,
            clock = { "2026-04-25T05:00:00Z" },
            idFactory = { "audio-id-${nextId++}" },
        )
        audioStorageRepository = AudioStorageRepository(
            database = database,
            appFilesDir = temporaryFolder.newFolder("files"),
            clock = { "2026-04-25T05:00:00Z" },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun adoptRowAudioCopiesFileAndRecordsDefaultMetadata() = runTest {
        val saved = saveText()
        val row = saved.rows.single()
        val source = temporaryFolder.newFile("source.wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }

        val adopted = audioStorageRepository.adoptRowAudio(
            textId = saved.id,
            rowId = row.id,
            ttsResponse = sampleResponse(source),
        )

        val asset = database.libraryDao().getAudioAsset("asset-1")
        val defaultRowAudio = database.libraryDao().getDefaultRowAudio(row.id)
        assertTrue(adopted.absoluteFile.exists())
        assertEquals("audio/rows/${saved.id}/${row.id}/asset-1.wav", adopted.asset.relativePath)
        assertEquals("asset-1", asset?.assetKey)
        assertEquals(false, asset?.isMissing)
        assertEquals(4L, asset?.sizeBytes)
        assertEquals("asset-1", defaultRowAudio?.assetKey)
        assertEquals(false, defaultRowAudio?.isStale)
    }

    @Test
    fun resolvePlayableAudioMarksMissingFile() = runTest {
        val saved = saveText()
        val row = saved.rows.single()
        val source = temporaryFolder.newFile("source.wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val adopted = audioStorageRepository.adoptRowAudio(saved.id, row.id, sampleResponse(source))

        assertTrue(adopted.absoluteFile.delete())
        val result = audioStorageRepository.resolvePlayableAudio("asset-1")

        val asset = database.libraryDao().getAudioAsset("asset-1")
        assertTrue(result is PlayableAudioResult.Missing)
        assertEquals(true, asset?.isMissing)
    }

    @Test
    fun adoptRowAudioRejectsMissingProviderOutput() = runTest {
        val saved = saveText()
        val row = saved.rows.single()
        val missingSource = File(temporaryFolder.root, "missing.wav")

        val error = assertProviderException {
            audioStorageRepository.adoptRowAudio(saved.id, row.id, sampleResponse(missingSource))
        }

        assertEquals(ProviderErrorCategory.MissingAudio, error.category)
    }

    @Test
    fun adoptRowAudioRejectsUnsafeAssetKey() = runTest {
        val saved = saveText()
        val row = saved.rows.single()
        val source = temporaryFolder.newFile("source.wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }

        val error = assertProviderException {
            audioStorageRepository.adoptRowAudio(
                saved.id,
                row.id,
                sampleResponse(source, assetKey = "../asset"),
            )
        }

        assertEquals(ProviderErrorCategory.InvalidResponse, error.category)
    }

    @Test
    fun playbackControllerReturnsFailureForMissingFileWithoutStartingMediaPlayer() {
        val controller = AndroidAudioPlaybackController()

        val result = controller.play(File(temporaryFolder.root, "missing.wav"))

        assertTrue(result is AudioPlaybackResult.Failed)
        controller.release()
    }

    private suspend fun saveText(): com.sindromradiospb.ttsprototypev2.core.model.LibraryText {
        val result = libraryRepository.saveGeneratedText(
            SaveGeneratedTextRequest(
                title = "Audio test",
                sourceText = "שלום",
                tableModelMeta = TableModelMeta(
                    provider = TranslationProviderId.GoogleTranslateFree,
                    actualProvider = TranslationProviderId.GoogleTranslateFree,
                    generatedAt = "2026-04-25T05:00:00Z",
                ),
                rows = listOf(GeneratedLibraryRowInput(hebrewPlain = "שלום", russian = "привет")),
            ),
        )
        return (result as SaveTextResult.Saved).text
    }

    private fun sampleResponse(sourceFile: File, assetKey: String = "asset-1"): TtsResponse =
        TtsResponse(
            audioAssetKey = assetKey,
            localFileName = sourceFile.name,
            localFilePath = sourceFile.absolutePath,
            mimeType = "audio/wav",
            provenance = ProviderProvenance(
                requestedProviderId = "system_or_browser_fallback_low_quality",
                actualProviderId = "system_or_browser_fallback_low_quality",
                model = "test",
                generatedAt = "2026-04-25T05:00:00Z",
            ),
            sizeBytes = sourceFile.takeIf { it.exists() }?.length(),
        )

    private suspend fun assertProviderException(block: suspend () -> Unit): ProviderException {
        try {
            block()
        } catch (error: ProviderException) {
            return error
        }
        throw AssertionError("Expected ProviderException")
    }
}
