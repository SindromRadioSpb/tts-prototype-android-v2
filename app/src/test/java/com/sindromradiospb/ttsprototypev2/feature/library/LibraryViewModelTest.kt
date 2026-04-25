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

    private fun viewModel(): LibraryViewModel =
        LibraryViewModel(
            repository = repository,
            clock = { "2026-04-25T07:30:00Z" },
        )

    private suspend fun saveText(): com.sindromradiospb.ttsprototypev2.core.model.LibraryText {
        val result = repository.saveGeneratedText(
            SaveGeneratedTextRequest(
                title = "Library test",
                sourceText = "שלום",
                tableModelMeta = TableModelMeta(
                    provider = TranslationProviderId.GoogleTranslateFree,
                    actualProvider = TranslationProviderId.GoogleTranslateFree,
                    generatedAt = "2026-04-25T07:00:00Z",
                ),
                rows = listOf(GeneratedLibraryRowInput(hebrewPlain = "שלום", russian = "привет")),
            ),
        )
        return (result as SaveTextResult.Saved).text
    }
}
