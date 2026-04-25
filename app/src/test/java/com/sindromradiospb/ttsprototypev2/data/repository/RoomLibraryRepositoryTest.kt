package com.sindromradiospb.ttsprototypev2.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindromradiospb.ttsprototypev2.core.model.TableModelMeta
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomLibraryRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomLibraryRepository
    private var idCounter = 0
    private var clockCounter = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = repositoryFor(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndLoadTextRoundTrip() = runTest {
        val result = repository.saveGeneratedText(sampleRequest(tags = listOf(" hebrew ", "study", "hebrew")))

        assertTrue(result is SaveTextResult.Saved)
        val saved = (result as SaveTextResult.Saved).text
        val loaded = repository.getText(saved.id)

        assertEquals("Greeting", loaded.title)
        assertEquals(listOf("hebrew", "study"), loaded.tags)
        assertEquals("שלום עולם", loaded.sourceText)
        assertEquals(2, loaded.rows.size)
        assertEquals("שלום", loaded.rows[0].hebrewPlain)
        assertEquals("шалом", loaded.rows[0].translitRu)
    }

    @Test
    fun duplicateTextReturnsConflict() = runTest {
        val first = repository.saveGeneratedText(sampleRequest())
        val second = repository.saveGeneratedText(sampleRequest())

        assertTrue(first is SaveTextResult.Saved)
        assertTrue(second is SaveTextResult.Conflict)
        assertEquals((first as SaveTextResult.Saved).text.id, (second as SaveTextResult.Conflict).existingTextId)
    }

    @Test
    fun updatePreservesRowIdsByOrder() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text
        val originalIds = saved.rows.map { it.id }

        val updated = repository.updateGeneratedText(
            textId = saved.id,
            input = sampleRequest(rows = listOf(row("שלום", russian = "привет"), row("עולם", russian = "земля"))),
        ) as SaveTextResult.Saved

        assertEquals(originalIds, updated.text.rows.map { it.id })
        assertEquals("земля", updated.text.rows[1].russian)
    }

    @Test
    fun updatePreservesExistingRowAudioLinks() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text
        val rowId = saved.rows.first().id
        repository.linkDefaultRowAudio(rowId, sampleAudio())

        repository.updateGeneratedText(
            textId = saved.id,
            input = sampleRequest(rows = listOf(row("שלום", russian = "привет"), row("עולם"))),
        )

        assertEquals("asset-1", repository.getDefaultRowAudio(rowId)?.assetKey)
    }

    @Test
    fun getTextIncludesDefaultRowAudioLink() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text
        val rowId = saved.rows.first().id
        repository.linkDefaultRowAudio(rowId, sampleAudio())

        val loaded = repository.getText(saved.id)

        assertEquals("asset-1", loaded.rows.first().audioAssetKey)
    }

    @Test
    fun patchRowStoresOriginalValueAndMarksDefaultAudioStale() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest(rows = listOf(row("שלום")))) as SaveTextResult.Saved).text
        val rowId = saved.rows.single().id
        repository.linkDefaultRowAudio(rowId, sampleAudio())

        val patched = repository.patchRow(
            textId = saved.id,
            rowId = rowId,
            fields = EditableRowFields(mapOf(RowField.HebrewPlain to "שלום חדש")),
        )
        val audio = repository.getDefaultRowAudio(rowId)

        assertEquals("שלום חדש", patched.hebrewPlain)
        assertEquals("שלום", patched.editMeta?.original?.get("hebrew_plain"))
        assertEquals(true, patched.editMeta?.edited?.get("hebrew_plain"))
        assertEquals(true, audio?.isStale)
        assertEquals("row_text_changed", audio?.staleReason)
    }

    @Test
    fun resetRowRestoresSelectedFieldOnly() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest(rows = listOf(row("שלום", russian = "мир")))) as SaveTextResult.Saved).text
        val rowId = saved.rows.single().id
        repository.patchRow(
            textId = saved.id,
            rowId = rowId,
            fields = EditableRowFields(
                mapOf(
                    RowField.HebrewPlain to "שלום חדש",
                    RowField.Russian to "привет",
                ),
            ),
        )

        val reset = repository.resetRowFields(saved.id, rowId, setOf(RowField.HebrewPlain))

        assertEquals("שלום", reset.hebrewPlain)
        assertEquals("привет", reset.russian)
        assertEquals(false, reset.editMeta?.edited?.get("hebrew_plain"))
        assertEquals(true, reset.editMeta?.edited?.get("russian"))
    }

    @Test
    fun reorderRowsValidatesExactRowSetAndPersistsOrder() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text
        val ids = saved.rows.map { it.id }

        val reordered = repository.reorderRows(saved.id, ids.reversed())

        assertEquals(ids.reversed(), reordered.map { it.id })
        assertEquals(listOf(0, 1), reordered.map { it.orderIndex })
        assertFailsWithMessage("Reorder must include exactly the current row IDs") {
            repository.reorderRows(saved.id, listOf(ids.first()))
        }
    }

    @Test
    fun deleteRowCompactsOrder() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text

        repository.deleteRow(saved.id, saved.rows.first().id)
        val loaded = repository.getText(saved.id)

        assertEquals(1, loaded.rows.size)
        assertEquals(0, loaded.rows.single().orderIndex)
        assertEquals("עולם", loaded.rows.single().hebrewPlain)
    }

    @Test
    fun addRowInsertsAfterSelectedRow() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text

        val added = repository.addRow(
            textId = saved.id,
            afterRowId = saved.rows.first().id,
            fields = EditableRowFields(mapOf(RowField.HebrewPlain to "חדש", RowField.Russian to "новый")),
        )
        val loaded = repository.getText(saved.id)

        assertEquals("חדש", added.hebrewPlain)
        assertEquals(listOf("שלום", "חדש", "עולם"), loaded.rows.map { it.hebrewPlain })
        assertEquals(listOf(0, 1, 2), loaded.rows.map { it.orderIndex })
        assertEquals(true, added.editMeta?.added)
    }

    @Test
    fun archiveHidesTextFromDefaultSummaryFlow() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text

        repository.archiveText(saved.id, archived = true)

        assertTrue(repository.observeTexts(includeArchived = false).first().isEmpty())
        assertEquals(1, repository.observeTexts(includeArchived = true).first().size)
        assertTrue(repository.getText(saved.id).isArchived)
    }

    @Test
    fun deleteTextRemovesTextFromSummariesAndLoadPath() = runTest {
        val saved = (repository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text

        repository.deleteText(saved.id)

        assertTrue(repository.observeTexts(includeArchived = true).first().isEmpty())
        assertFailsWithMessage("Library text not found: ${saved.id}") {
            repository.getText(saved.id)
        }
    }

    @Test
    fun savedTextSurvivesDatabaseRestart() = runTest {
        val databaseName = "restart-${System.nanoTime()}.db"
        val firstDb = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        val firstRepository = repositoryFor(firstDb)
        val saved = (firstRepository.saveGeneratedText(sampleRequest()) as SaveTextResult.Saved).text
        firstDb.close()

        val secondDb = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        val secondRepository = repositoryFor(secondDb)
        val loaded = secondRepository.getText(saved.id)
        secondDb.close()
        context.deleteDatabase(databaseName)

        assertEquals(saved.id, loaded.id)
        assertEquals(saved.rows.map { it.hebrewPlain }, loaded.rows.map { it.hebrewPlain })
    }

    private fun repositoryFor(database: AppDatabase): RoomLibraryRepository =
        RoomLibraryRepository(
            database = database,
            clock = { "2026-04-25T00:00:${(clockCounter++).toString().padStart(2, '0')}Z" },
            idFactory = { "id-${idCounter++}" },
        )

    private fun sampleRequest(
        tags: List<String> = emptyList(),
        rows: List<GeneratedLibraryRowInput> = listOf(row("שלום"), row("עולם")),
    ): SaveGeneratedTextRequest =
        SaveGeneratedTextRequest(
            title = "Greeting",
            tags = tags,
            sourceText = "שלום עולם",
            tableModelMeta = TableModelMeta(
                provider = TranslationProviderId.GoogleTranslateFree,
                actualProvider = TranslationProviderId.GoogleTranslateFree,
                generatedAt = "2026-04-25T00:00:00Z",
            ),
            rows = rows,
        )

    private fun row(
        hebrew: String,
        russian: String = "мир",
    ): GeneratedLibraryRowInput =
        GeneratedLibraryRowInput(
            hebrewPlain = hebrew,
            translit = "shalom",
            translitRu = "шалом",
            russian = russian,
        )

    private fun sampleAudio(): AudioAssetInput =
        AudioAssetInput(
            assetKey = "asset-1",
            fileName = "asset-1.mp3",
            relativePath = "audio/rows/id-1/id-2/asset-1.mp3",
            mimeType = "audio/mpeg",
            providerId = "google_online_tts",
            language = "he",
            provenanceJson = """{"requested_provider_id":"google_online_tts"}""",
        )

    private suspend fun assertFailsWithMessage(
        expectedMessage: String,
        block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (error: IllegalArgumentException) {
            assertEquals(expectedMessage, error.message)
            return
        } catch (error: IllegalStateException) {
            assertEquals(expectedMessage, error.message)
            return
        }
        throw AssertionError("Expected IllegalArgumentException or IllegalStateException")
    }
}
