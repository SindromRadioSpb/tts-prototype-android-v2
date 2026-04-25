package com.sindromradiospb.ttsprototypev2.feature.classic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindromradiospb.ttsprototypev2.core.model.SourceMeta
import com.sindromradiospb.ttsprototypev2.core.model.TableModelMeta
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TtsProfile
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderProvenance
import com.sindromradiospb.ttsprototypev2.core.provider.TtsProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TtsRequest
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationRequest
import com.sindromradiospb.ttsprototypev2.data.repository.GeneratedLibraryRowInput
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.SaveGeneratedTextRequest
import com.sindromradiospb.ttsprototypev2.data.repository.SaveTextResult
import com.sindromradiospb.ttsprototypev2.data.provider.translation.createAndroidTranslationProviderRegistry
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassicGeneratedRowUi(
    val orderIndex: Int,
    val hebrewPlain: String,
    val hebrewNiqqud: String,
    val translit: String,
    val translitRu: String,
    val russian: String,
)

data class ClassicModeUiState(
    val sourceText: String = "שלום, זהו אבטיפוס פשוט של המערכת",
    val translationProvider: TranslationProviderId = TranslationProviderId.GoogleTranslateFree,
    val ttsProvider: TtsProviderId = TtsProviderId.GoogleOnlineTts,
    val rows: List<ClassicGeneratedRowUi> = emptyList(),
    val libraryTexts: List<LibraryTextSummary> = emptyList(),
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val isSpeaking: Boolean = false,
    val savedTextId: String? = null,
    val generatedAt: String? = null,
    val generationLabel: String = "Generation: not started",
    val provenance: ProviderProvenance? = null,
    val message: String? = null,
)

class ClassicModeViewModel(
    private val repository: LibraryRepository,
    private val translationProviders: TranslationProviderRegistry = createAndroidTranslationProviderRegistry(),
    private val ttsProviders: TtsProviderRegistry? = null,
    private val clock: () -> String = { Instant.now().toString() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClassicModeUiState())
    val uiState: StateFlow<ClassicModeUiState> = _uiState.asStateFlow()

    init {
        repository.observeTexts(includeArchived = false)
            .onEach { summaries ->
                _uiState.update { it.copy(libraryTexts = summaries) }
            }
            .launchIn(viewModelScope)
    }

    fun onSourceTextChanged(value: String) {
        _uiState.update {
            it.copy(
                sourceText = value,
                savedTextId = null,
                message = null,
            )
        }
    }

    fun onTranslationProviderChanged(providerId: TranslationProviderId) {
        _uiState.update { it.copy(translationProvider = providerId, savedTextId = null, message = null) }
    }

    fun onTtsProviderChanged(providerId: TtsProviderId) {
        _uiState.update { it.copy(ttsProvider = providerId, savedTextId = null, message = null) }
    }

    fun generateTable() {
        val current = uiState.value
        if (current.sourceText.isBlank()) {
            _uiState.update { it.copy(message = "Введите Hebrew source text перед генерацией.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = null) }
            val provider = translationProviders.get(current.translationProvider)
            val result = provider.translate(
                TranslationRequest(
                    sourceText = current.sourceText,
                    providerId = current.translationProvider,
                ),
            )
            result.fold(
                onSuccess = { response ->
                    val rows = response.rows.map {
                        ClassicGeneratedRowUi(
                            orderIndex = it.segmentIndex,
                            hebrewPlain = it.hebrewPlain,
                            hebrewNiqqud = it.hebrewNiqqud,
                            translit = it.translit,
                            translitRu = it.translitRu,
                            russian = it.russian,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            rows = rows,
                            isGenerating = false,
                            savedTextId = null,
                            generatedAt = response.provenance.generatedAt,
                            generationLabel = "Translation: ${response.provenance.actualProviderId}",
                            provenance = response.provenance,
                            message = "Translation complete via ${response.provenance.actualProviderId}.",
                        )
                    }
                },
                onFailure = { error ->
                    val providerError = error as? ProviderException
                    val message = if (providerError != null) {
                        "Translation failed (${providerError.category}): ${providerError.userMessage}"
                    } else {
                        "Translation failed: ${error.message.orEmpty().ifBlank { "Unknown provider error." }}"
                    }
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generationLabel = "Translation failed",
                            message = message,
                        )
                    }
                },
            )
        }
    }

    private fun providerIdFromWireId(wireId: String?): TranslationProviderId? =
        TranslationProviderId.entries.firstOrNull { it.wireId == wireId }

    fun saveCurrent() {
        val current = uiState.value
        if (current.isSaving || current.isGenerating) {
            return
        }
        if (current.rows.isEmpty()) {
            _uiState.update { it.copy(message = "Сначала сгенерируйте строки.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            val request = current.toSaveRequest(clock())
            when (val result = repository.saveGeneratedText(request)) {
                is SaveTextResult.Saved -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            savedTextId = result.text.id,
                            message = "Saved to local Room library.",
                        )
                    }
                }
                is SaveTextResult.Conflict -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            savedTextId = result.existingTextId,
                            message = "This generated text is already saved in the local library.",
                        )
                    }
                }
            }
        }
    }

    fun openLibraryText(textId: String, resume: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = null) }
            runCatching {
                val openedAt = clock()
                repository.markOpened(textId, openedAt)
                repository.getText(textId)
            }.fold(
                onSuccess = { text ->
                    val provider = text.tableModelMeta?.provider ?: _uiState.value.translationProvider
                    _uiState.update {
                        it.copy(
                            sourceText = text.sourceText,
                            translationProvider = provider,
                            ttsProvider = text.ttsProfile?.providerId ?: it.ttsProvider,
                            rows = text.rows.sortedBy { row -> row.orderIndex }.map { row ->
                                ClassicGeneratedRowUi(
                                    orderIndex = row.orderIndex,
                                    hebrewPlain = row.hebrewPlain,
                                    hebrewNiqqud = row.hebrewNiqqud,
                                    translit = row.translit,
                                    translitRu = row.translitRu,
                                    russian = row.russian,
                                )
                            },
                            isGenerating = false,
                            savedTextId = text.id,
                            generatedAt = text.tableModelMeta?.generatedAt ?: text.updatedAt,
                            generationLabel = "Library: ${text.title}",
                            provenance = null,
                            message = if (resume) {
                                "Opened ${text.title}. Progress metadata is not available yet; resumed at the loaded table."
                            } else {
                                "Opened ${text.title} from local library."
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            message = "Could not open library text: ${error.message.orEmpty()}",
                        )
                    }
                },
            )
        }
    }

    fun speakSource() {
        val current = uiState.value
        if (current.sourceText.isBlank()) {
            _uiState.update { it.copy(message = "Введите Hebrew source text перед TTS.") }
            return
        }
        val registry = ttsProviders
        if (registry == null) {
            _uiState.update { it.copy(message = "TTS registry is not available in this build path.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true, message = null) }
            val profile = TtsProfile(
                providerId = current.ttsProvider,
                language = "he-IL",
                voiceName = null,
            )
            val provider = registry.get(current.ttsProvider)
            val result = provider.synthesize(
                TtsRequest(
                    text = current.sourceText,
                    profile = profile,
                ),
            )
            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            isSpeaking = false,
                            message = "TTS complete via ${response.provenance.actualProviderId}: ${response.localFileName.orEmpty()}",
                        )
                    }
                },
                onFailure = { error ->
                    val providerError = error as? ProviderException
                    val message = if (providerError != null) {
                        "TTS failed (${providerError.category}): ${providerError.userMessage}"
                    } else {
                        "TTS failed: ${error.message.orEmpty().ifBlank { "Unknown provider error." }}"
                    }
                    _uiState.update { it.copy(isSpeaking = false, message = message) }
                },
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun ClassicModeUiState.toSaveRequest(now: String): SaveGeneratedTextRequest =
        SaveGeneratedTextRequest(
            title = sourceText.trim().lineSequence().firstOrNull()
                ?.take(48)
                ?.ifBlank { "Classic Mode text" }
                ?: "Classic Mode text",
            level = null,
            tags = listOf("classic-mode", "m3-translation-provider"),
            sourceLabel = "classic_mode",
            topic = null,
            sourceText = sourceText,
            sourceMeta = SourceMeta(origin = "classic_mode_m3_translation_provider", importedAt = now),
            tableModelMeta = TableModelMeta(
                provider = translationProvider,
                actualProvider = providerIdFromWireId(provenance?.actualProviderId) ?: translationProvider,
                model = provenance?.model,
                fromCache = false,
                niqqudProvider = "not_implemented_m3",
                niqqudDegraded = true,
                generatedAt = generatedAt ?: now,
            ),
            ttsProfile = TtsProfile(
                providerId = ttsProvider,
                language = "he",
                voiceName = null,
            ),
            rows = rows.map {
                GeneratedLibraryRowInput(
                    hebrewPlain = it.hebrewPlain,
                    hebrewNiqqud = it.hebrewNiqqud,
                    translit = it.translit,
                    translitRu = it.translitRu,
                    russian = it.russian,
                )
            },
        )

    class Factory(
        private val repository: LibraryRepository,
        private val translationProviders: TranslationProviderRegistry = createAndroidTranslationProviderRegistry(),
        private val ttsProviders: TtsProviderRegistry? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ClassicModeViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return ClassicModeViewModel(
                repository = repository,
                translationProviders = translationProviders,
                ttsProviders = ttsProviders,
            ) as T
        }
    }
}
