package com.sindromradiospb.ttsprototypev2.feature.classic

import com.sindromradiospb.ttsprototypev2.MainDispatcherRule
import com.sindromradiospb.ttsprototypev2.core.model.LibraryRow
import com.sindromradiospb.ttsprototypev2.core.model.LibraryText
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.TtsProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationProviderRegistry
import com.sindromradiospb.ttsprototypev2.data.provider.tts.FakeTtsProvider
import com.sindromradiospb.ttsprototypev2.data.provider.tts.MissingConfigurationTtsProvider
import com.sindromradiospb.ttsprototypev2.data.provider.translation.FakeTranslationProvider
import com.sindromradiospb.ttsprototypev2.data.provider.translation.MissingConfigurationTranslationProvider
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.data.repository.SaveGeneratedTextRequest
import com.sindromradiospb.ttsprototypev2.data.repository.SaveTextResult
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicTableColumn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClassicModeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun blankSourceDoesNotGenerateRows() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onSourceTextChanged("   ")
        viewModel.generateTable()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.rows.isEmpty())
        assertEquals("Введите Hebrew source text перед генерацией.", state.message)
    }

    @Test
    fun generateUsesTranslationProviderAndPreservesSelectedProviders() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onTranslationProviderChanged(TranslationProviderId.GeminiLegacy)
        viewModel.onTtsProviderChanged(TtsProviderId.SystemFallbackLowQuality)
        viewModel.onSourceTextChanged("שלום עולם\nבדיקה")
        viewModel.generateTable()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertEquals(2, state.rows.size)
        assertEquals("שלום עולם", state.rows.first().hebrewPlain)
        assertEquals("m3-fake-sbl-1", state.rows.first().translit)
        assertEquals(TranslationProviderId.GeminiLegacy, state.translationProvider)
        assertEquals(TtsProviderId.SystemFallbackLowQuality, state.ttsProvider)
        assertEquals("Translation: gemini_legacy", state.generationLabel)
        assertEquals("Translation complete via gemini_legacy.", state.message)
    }

    @Test
    fun selectingSystemFallbackResetsUnsupportedVoiceControls() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onTtsVoiceNameChanged("he-IL-Wavenet-A")
        viewModel.onSpeakingRateChanged(1.6)
        viewModel.onPitchChanged(2.0)
        viewModel.onTtsProviderChanged(TtsProviderId.SystemFallbackLowQuality)

        val state = viewModel.uiState.value
        assertEquals(TtsProviderId.SystemFallbackLowQuality, state.ttsProvider)
        assertNull(state.ttsVoiceName)
        assertEquals(1.0, state.speakingRate, 0.0)
        assertEquals(0.0, state.pitch, 0.0)
    }

    @Test
    fun missingConfiguredProviderShowsVisibleErrorWithoutFallback() = runTest(mainDispatcherRule.testDispatcher) {
        val registry = TranslationProviderRegistry(
            listOf(
                FakeTranslationProvider(id = TranslationProviderId.GoogleTranslateFree),
                MissingConfigurationTranslationProvider(
                    id = TranslationProviderId.GcpTranslate,
                    message = "gcp_translate is not configured.",
                ),
                FakeTranslationProvider(id = TranslationProviderId.GeminiLegacy),
            ),
        )
        val viewModel = viewModel(translationProviders = registry)

        viewModel.onTranslationProviderChanged(TranslationProviderId.GcpTranslate)
        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.rows.isEmpty())
        assertEquals("Translation failed", state.generationLabel)
        assertTrue(state.message.orEmpty().contains("MissingConfiguration"))
    }

    @Test
    fun changingTranslationProviderRegeneratesExistingTable() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()
        assertEquals("Translation: google_translate_free", viewModel.uiState.value.generationLabel)

        viewModel.onTranslationProviderChanged(TranslationProviderId.GeminiLegacy)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TranslationProviderId.GeminiLegacy, state.translationProvider)
        assertEquals("Translation: gemini_legacy", state.generationLabel)
        assertEquals("Translation complete via gemini_legacy.", state.message)
    }

    @Test
    fun saveGeneratedRowsPersistsToLibraryPort() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeLibraryRepository()
        val viewModel = viewModel(repository)

        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()
        viewModel.saveCurrent()
        viewModel.commitSaveMetadata()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(1, repository.savedRequests.size)
        assertEquals("saved-1", state.savedTextId)
        assertEquals("Карточка текста сохранена в Library.", state.message)
        assertEquals(1, state.libraryTexts.size)
    }

    @Test
    fun secondSaveUpdatesCurrentCardWithoutCreatingSecondText() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeLibraryRepository()
        val viewModel = viewModel(repository)

        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()
        viewModel.saveCurrent()
        viewModel.commitSaveMetadata()
        advanceUntilIdle()
        viewModel.saveCurrent()
        viewModel.commitSaveMetadata()
        advanceUntilIdle()

        assertEquals(1, repository.savedRequests.size)
        assertEquals("Карточка текста сохранена в Library.", viewModel.uiState.value.message)
    }

    @Test
    fun openLibraryTextLoadsSavedRowsIntoClassicMode() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeLibraryRepository()
        val viewModel = viewModel(repository)

        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()
        viewModel.saveCurrent()
        viewModel.updateSaveMetadataDraft(
            ClassicSaveMetadataDraft(
                title = "Saved YouTube title",
                source = "https://www.youtube.com/watch?v=demo",
                tagsCsv = "classic-mode",
            ),
        )
        viewModel.commitSaveMetadata()
        advanceUntilIdle()
        viewModel.onSourceTextChanged("אחר")
        viewModel.openLibraryText("saved-1", resume = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("שלום עולם", state.sourceText)
        assertEquals("saved-1", state.savedTextId)
        assertEquals("Saved YouTube title", state.loadedTextTitle)
        assertEquals("https://www.youtube.com/watch?v=demo", state.loadedTextSourceLabel)
        assertEquals("Library: Saved YouTube title", state.generationLabel)
        assertTrue(state.message.orEmpty().contains("Progress metadata is not available yet"))
    }

    @Test
    fun speakSourceUsesSelectedTtsProvider() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(
            ttsProviders = TtsProviderRegistry(
                listOf(
                    FakeTtsProvider(id = TtsProviderId.GoogleOnlineTts),
                    FakeTtsProvider(id = TtsProviderId.SystemFallbackLowQuality),
                ),
            ),
        )

        viewModel.onTtsProviderChanged(TtsProviderId.SystemFallbackLowQuality)
        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.speakSource()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSpeaking)
        assertTrue(state.message.orEmpty().contains("TTS complete via system_or_browser_fallback_low_quality"))
    }

    @Test
    fun speakSourceShowsMissingConfigurationWithoutFallback() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(
            ttsProviders = TtsProviderRegistry(
                listOf(
                    MissingConfigurationTtsProvider(
                        id = TtsProviderId.GoogleOnlineTts,
                        message = "google tts missing",
                    ),
                    FakeTtsProvider(id = TtsProviderId.SystemFallbackLowQuality),
                ),
            ),
        )

        viewModel.onTtsProviderChanged(TtsProviderId.GoogleOnlineTts)
        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.speakSource()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSpeaking)
        assertTrue(state.message.orEmpty().contains("MissingConfiguration"))
        assertTrue(state.message.orEmpty().contains("google tts missing"))
    }

    @Test
    fun rowNoteRequiresSavedCardThenPersistsAndDeletes() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeLibraryRepository()
        val viewModel = viewModel(repository)

        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()
        viewModel.openRowNote(orderIndex = 0)

        assertTrue(viewModel.uiState.value.message.orEmpty().contains("Сначала нажмите"))

        viewModel.saveCurrent()
        viewModel.commitSaveMetadata()
        advanceUntilIdle()
        viewModel.openRowNote(orderIndex = 0)
        viewModel.updateRowNoteDraft("  **важно**\n- повторить  ")
        viewModel.saveRowNote()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals("Заметка сохранена.", state.message)
        assertEquals("**важно**\n- повторить", state.rows.first().note)

        viewModel.openRowNote(orderIndex = 0)
        viewModel.deleteRowNote()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Заметка удалена.", state.message)
        assertNull(state.rows.first().note)
    }

    @Test
    fun tableColumnTogglePreventsHidingEveryColumn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        ClassicTableColumn.entries.dropLast(1).forEach(viewModel::toggleTableColumn)
        viewModel.toggleTableColumn(ClassicTableColumn.Translation)

        val state = viewModel.uiState.value
        assertEquals(setOf(ClassicTableColumn.Translation), state.tableDisplay.visibleColumns)
        assertEquals("Нельзя скрыть все колонки. Минимум одна должна оставаться видимой.", state.message)
    }

    @Test
    fun tableColumnResizeIsClampedAndResetRestoresDefaults() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.adjustTableColumnWidth(ClassicTableColumn.Hebrew, -10_000f)
        assertEquals(64, viewModel.uiState.value.tableDisplay.columnWidthsDp[ClassicTableColumn.Hebrew])

        viewModel.adjustTableColumnWidth(ClassicTableColumn.Hebrew, 10_000f)
        assertEquals(420, viewModel.uiState.value.tableDisplay.columnWidthsDp[ClassicTableColumn.Hebrew])

        viewModel.resetTableDisplay()
        val state = viewModel.uiState.value
        assertEquals(170, state.tableDisplay.columnWidthsDp[ClassicTableColumn.Hebrew])
        assertEquals("Настройки таблицы сброшены.", state.message)
    }

    @Test
    fun autoNextWithoutRowsShowsExplicitMessage() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.toggleAutoNextPlayback()

        val state = viewModel.uiState.value
        assertFalse(state.tableDisplay.isAutoNextActive)
        assertEquals("Нет строк для построчного воспроизведения.", state.message)
    }

    @Test
    fun disclosureToggleCollapsesClassicBlocks() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.toggleDisclosure(ClassicDisclosurePanel.Voice)
        viewModel.toggleDisclosure(ClassicDisclosurePanel.Result)

        val state = viewModel.uiState.value
        assertFalse(state.disclosure.voiceOpen)
        assertFalse(state.disclosure.resultOpen)
        assertTrue(state.disclosure.sourceOpen)
        assertTrue(state.disclosure.translationOpen)
    }

    private fun viewModel(
        repository: LibraryRepository = FakeLibraryRepository(),
        translationProviders: TranslationProviderRegistry = TranslationProviderRegistry(
            listOf(
                FakeTranslationProvider(id = TranslationProviderId.GoogleTranslateFree),
                FakeTranslationProvider(id = TranslationProviderId.GcpTranslate),
                FakeTranslationProvider(id = TranslationProviderId.GeminiLegacy),
            ),
        ),
        ttsProviders: TtsProviderRegistry? = null,
    ): ClassicModeViewModel =
        ClassicModeViewModel(
            repository = repository,
            translationProviders = translationProviders,
            ttsProviders = ttsProviders,
            clock = { "2026-04-25T02:00:00Z" },
        )
}

private class FakeLibraryRepository : LibraryRepository {
    private val summaries = MutableStateFlow<List<LibraryTextSummary>>(emptyList())
    val savedRequests = mutableListOf<SaveGeneratedTextRequest>()

    override fun observeTexts(includeArchived: Boolean): Flow<List<LibraryTextSummary>> = summaries

    override suspend fun getText(textId: String): LibraryText =
        requireNotNull(lastSavedText?.takeIf { it.id == textId }) { "Library text not found: $textId" }

    override suspend fun markOpened(textId: String, openedAt: String) {
        lastSavedText = lastSavedText?.takeIf { it.id == textId }?.copy(lastOpenedAt = openedAt)
    }

    override suspend fun saveGeneratedText(input: SaveGeneratedTextRequest): SaveTextResult {
        if (savedRequests.any { it.sourceText == input.sourceText }) {
            return SaveTextResult.Conflict(existingTextId = "saved-1", textKey = "fake-key")
        }
        savedRequests.add(input)
        val saved = LibraryText(
            id = "saved-1",
            textKey = "fake-key",
            title = input.title,
            sourceLabel = input.sourceLabel,
            level = input.level,
            tags = input.tags,
            topic = input.topic,
            sourceText = input.sourceText,
            sourceMeta = input.sourceMeta,
            ttsProfile = input.ttsProfile,
            tableModelMeta = input.tableModelMeta,
            rows = input.rows.mapIndexed { index, row ->
                LibraryRow(
                    id = "row-$index",
                    textId = "saved-1",
                    orderIndex = index,
                    hebrewPlain = row.hebrewPlain,
                    hebrewNiqqud = row.hebrewNiqqud,
                    translit = row.translit,
                    translitRu = row.translitRu,
                    russian = row.russian,
                )
            },
            createdAt = "2026-04-25T02:00:00Z",
            updatedAt = "2026-04-25T02:00:00Z",
        )
        lastSavedText = saved
        summaries.value = listOf(
            LibraryTextSummary(
                textId = saved.id,
                textKey = saved.textKey,
                title = saved.title,
                level = saved.level,
                tags = saved.tags,
                sourceLabel = saved.sourceLabel,
                topic = saved.topic,
                createdAt = saved.createdAt,
                updatedAt = saved.updatedAt,
                lastOpenedAt = saved.lastOpenedAt,
                isArchived = false,
            ),
        )
        return SaveTextResult.Saved(saved)
    }

    override suspend fun updateGeneratedText(textId: String, input: SaveGeneratedTextRequest): SaveTextResult {
        val current = requireNotNull(lastSavedText?.takeIf { it.id == textId }) { "Library text not found: $textId" }
        val updated = current.copy(
            title = input.title,
            sourceLabel = input.sourceLabel,
            level = input.level,
            tags = input.tags,
            topic = input.topic,
            sourceText = input.sourceText,
            sourceMeta = input.sourceMeta,
            ttsProfile = input.ttsProfile,
            tableModelMeta = input.tableModelMeta,
            rows = input.rows.mapIndexed { index, row ->
                LibraryRow(
                    id = current.rows.getOrNull(index)?.id ?: "row-$index",
                    textId = textId,
                    orderIndex = index,
                    hebrewPlain = row.hebrewPlain,
                    hebrewNiqqud = row.hebrewNiqqud,
                    translit = row.translit,
                    translitRu = row.translitRu,
                    russian = row.russian,
                    note = current.rows.getOrNull(index)?.note,
                )
            },
            updatedAt = "2026-04-25T02:00:00Z",
        )
        lastSavedText = updated
        return SaveTextResult.Saved(updated)
    }

    override suspend fun saveRowNote(textId: String, rowId: String, note: String): LibraryRow {
        val text = requireNotNull(lastSavedText?.takeIf { it.id == textId }) { "Library text not found: $textId" }
        val trimmed = note.trim()
        val updatedRow = requireNotNull(text.rows.firstOrNull { it.id == rowId }) { "Library row not found: $rowId" }
            .copy(note = trimmed.takeIf { it.isNotBlank() })
        lastSavedText = text.copy(rows = text.rows.map { if (it.id == rowId) updatedRow else it })
        return updatedRow
    }

    override suspend fun deleteRowNote(textId: String, rowId: String): LibraryRow =
        saveRowNote(textId, rowId, "")

    private var lastSavedText: LibraryText? = null
}
