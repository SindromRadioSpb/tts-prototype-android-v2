package com.sindromradiospb.ttsprototypev2.feature.classic

import com.sindromradiospb.ttsprototypev2.MainDispatcherRule
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun saveGeneratedRowsPersistsToLibraryPort() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeLibraryRepository()
        val viewModel = viewModel(repository)

        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()
        viewModel.saveCurrent()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(1, repository.savedRequests.size)
        assertEquals("saved-1", state.savedTextId)
        assertEquals("Saved to local Room library.", state.message)
        assertEquals(1, state.libraryTexts.size)
    }

    @Test
    fun duplicateSaveShowsConflictWithoutCreatingSecondText() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeLibraryRepository()
        val viewModel = viewModel(repository)

        viewModel.onSourceTextChanged("שלום עולם")
        viewModel.generateTable()
        advanceUntilIdle()
        viewModel.saveCurrent()
        advanceUntilIdle()
        viewModel.saveCurrent()
        advanceUntilIdle()

        assertEquals(1, repository.savedRequests.size)
        assertEquals("This generated text is already saved in the local library.", viewModel.uiState.value.message)
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

    override suspend fun saveGeneratedText(input: SaveGeneratedTextRequest): SaveTextResult {
        if (savedRequests.any { it.sourceText == input.sourceText }) {
            return SaveTextResult.Conflict(existingTextId = "saved-1", textKey = "fake-key")
        }
        savedRequests.add(input)
        val saved = LibraryText(
            id = "saved-1",
            textKey = "fake-key",
            title = input.title,
            tags = input.tags,
            sourceText = input.sourceText,
            sourceMeta = input.sourceMeta,
            ttsProfile = input.ttsProfile,
            tableModelMeta = input.tableModelMeta,
            rows = emptyList(),
            createdAt = "2026-04-25T02:00:00Z",
            updatedAt = "2026-04-25T02:00:00Z",
        )
        summaries.value = listOf(
            LibraryTextSummary(
                textId = saved.id,
                textKey = saved.textKey,
                title = saved.title,
                level = saved.level,
                updatedAt = saved.updatedAt,
                isArchived = false,
            ),
        )
        return SaveTextResult.Saved(saved)
    }
}
