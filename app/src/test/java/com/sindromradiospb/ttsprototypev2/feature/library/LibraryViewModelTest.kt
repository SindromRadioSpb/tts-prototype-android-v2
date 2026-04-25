package com.sindromradiospb.ttsprototypev2.feature.library

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sindromradiospb.ttsprototypev2.MainDispatcherRule
import com.sindromradiospb.ttsprototypev2.core.model.TableModelMeta
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.repository.GeneratedLibraryRowInput
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.SaveGeneratedTextRequest
import com.sindromradiospb.ttsprototypev2.data.repository.SaveTextResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomLibraryRepository
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLibraryRepository(
            database = database,
            clock = { "2026-04-25T07:00:00Z" },
            idFactory = { "id-${nextId++}" },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun openTextLoadsRowsAndMarksLastOpened() = runTest(mainDispatcherRule.testDispatcher) {
        val saved = saveText()
        val viewModel = viewModel()

        viewModel.openText(saved.id)

        val state = viewModel.uiState.first { it.selectedText?.id == saved.id }
        assertEquals(saved.id, state.selectedText?.id)
        assertEquals("2026-04-25T07:30:00Z", state.selectedText?.lastOpenedAt)
        assertEquals("שלום", state.selectedText?.rows?.single()?.hebrewPlain)
        assertEquals("Opened Library test.", state.message)
    }

    @Test
    fun archiveRestoreAndDeleteSelectedTextUpdatesLifecycleState() = runTest(mainDispatcherRule.testDispatcher) {
        val saved = saveText()
        val viewModel = viewModel()

        viewModel.openText(saved.id)
        viewModel.uiState.first { it.selectedText?.id == saved.id }
        viewModel.archiveSelected(archived = true)

        val archived = viewModel.uiState.first { it.selectedText?.isArchived == true && it.summaries.isEmpty() }
        assertTrue(archived.summaries.isEmpty())

        viewModel.setIncludeArchived(true)
        assertEquals(1, viewModel.uiState.first { it.includeArchived && it.summaries.size == 1 }.summaries.size)

        viewModel.archiveSelected(archived = false)
        val restored = viewModel.uiState.first { it.selectedText?.isArchived == false }
        assertFalse(restored.selectedText?.isArchived ?: true)

        viewModel.deleteSelected()

        val deleted = viewModel.uiState.first {
            it.selectedText == null && it.summaries.isEmpty() && it.message == "Deleted Library test."
        }
        assertNull(deleted.selectedText)
        assertTrue(deleted.summaries.isEmpty())
        assertEquals("Deleted Library test.", deleted.message)
    }

    @Test
    fun editResetMoveDeleteAndAddRowsUpdatesSelectedText() = runTest(mainDispatcherRule.testDispatcher) {
        val saved = saveText(
            rows = listOf(
                GeneratedLibraryRowInput(hebrewPlain = "שלום", russian = "привет"),
                GeneratedLibraryRowInput(hebrewPlain = "עולם", russian = "мир"),
            ),
        )
        val viewModel = viewModel()
        viewModel.openText(saved.id)
        val opened = viewModel.uiState.first { it.selectedText?.id == saved.id }.selectedText!!
        val firstRow = opened.rows[0]
        val secondRow = opened.rows[1]

        viewModel.startEditingRow(firstRow.id)
        viewModel.updateRowDraft(
            LibraryRowDraft.from(firstRow).copy(
                hebrewPlain = "שָׁלוֹם",
                russian = "мир вам",
            ),
        )
        viewModel.saveEditingRow()

        val edited = viewModel.uiState.first { it.message == "Saved row 1." }.selectedText!!
        assertEquals("שָׁלוֹם", edited.rows[0].hebrewPlain)
        assertEquals("мир вам", edited.rows[0].russian)
        assertTrue(edited.rows[0].editMeta?.edited?.get("hebrew_plain") == true)

        viewModel.resetRow(edited.rows[0].id)
        val reset = viewModel.uiState.first { it.message == "Reset row 1." }.selectedText!!
        assertEquals("שלום", reset.rows[0].hebrewPlain)
        assertEquals("привет", reset.rows[0].russian)
        assertFalse(reset.rows[0].editMeta?.edited?.get("hebrew_plain") ?: true)

        viewModel.moveRow(secondRow.id, -1)
        val moved = viewModel.uiState.first { it.message == "Moved row 2 to position 1." }.selectedText!!
        assertEquals("עולם", moved.rows[0].hebrewPlain)
        assertEquals("שלום", moved.rows[1].hebrewPlain)

        viewModel.deleteRow(moved.rows[1].id)
        val deleted = viewModel.uiState.first { it.message == "Deleted row 2." }.selectedText!!
        assertEquals(1, deleted.rows.size)
        assertEquals("עולם", deleted.rows.single().hebrewPlain)

        viewModel.startAddingRow(deleted.rows.single().id)
        viewModel.updateRowDraft(LibraryRowDraft(hebrewPlain = "אור", russian = "свет"))
        viewModel.saveNewRow()

        val added = viewModel.uiState.first { it.message == "Added row." }.selectedText!!
        assertEquals(listOf("עולם", "אור"), added.rows.map { it.hebrewPlain })
        assertTrue(added.rows[1].editMeta?.added == true)
    }

    @Test
    fun saveNewRowRequiresAtLeastOneField() = runTest(mainDispatcherRule.testDispatcher) {
        val saved = saveText()
        val viewModel = viewModel()

        viewModel.openText(saved.id)
        viewModel.uiState.first { it.selectedText?.id == saved.id }
        viewModel.startAddingRow(afterRowId = null)
        viewModel.saveNewRow()

        val state = viewModel.uiState.first { it.message == "Enter at least one row field before adding a row." }
        assertEquals(1, state.selectedText?.rows?.size)
    }

    private fun viewModel(): LibraryViewModel =
        LibraryViewModel(
            repository = repository,
            clock = { "2026-04-25T07:30:00Z" },
        )

    private suspend fun saveText(
        rows: List<GeneratedLibraryRowInput> = listOf(
            GeneratedLibraryRowInput(hebrewPlain = "שלום", russian = "привет"),
        ),
    ): com.sindromradiospb.ttsprototypev2.core.model.LibraryText {
        val result = repository.saveGeneratedText(
            SaveGeneratedTextRequest(
                title = "Library test",
                sourceText = "שלום",
                tableModelMeta = TableModelMeta(
                    provider = TranslationProviderId.GoogleTranslateFree,
                    actualProvider = TranslationProviderId.GoogleTranslateFree,
                    generatedAt = "2026-04-25T07:00:00Z",
                ),
                rows = rows,
            ),
        )
        return (result as SaveTextResult.Saved).text
    }
}
