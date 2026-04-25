package com.sindromradiospb.ttsprototypev2.feature.classic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindromradiospb.ttsprototypev2.core.model.SourceMeta
import com.sindromradiospb.ttsprototypev2.core.model.TableModelMeta
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TtsProfile
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.data.repository.GeneratedLibraryRowInput
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.SaveGeneratedTextRequest
import com.sindromradiospb.ttsprototypev2.data.repository.SaveTextResult
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
    val savedTextId: String? = null,
    val generatedAt: String? = null,
    val message: String? = null,
)

class ClassicModeViewModel(
    private val repository: LibraryRepository,
    private val generator: ClassicGenerationShell = ClassicGenerationShell(),
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
            val generatedAt = clock()
            val rows = generator.generate(current.sourceText)
            _uiState.update {
                it.copy(
                    rows = rows,
                    isGenerating = false,
                    savedTextId = null,
                    generatedAt = generatedAt,
                    message = "M2 fake generation complete. Real providers are scheduled for M3/M4.",
                )
            }
        }
    }

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
            tags = listOf("classic-mode", "m2-fake-generation"),
            sourceText = sourceText,
            sourceMeta = SourceMeta(origin = "classic_mode_m2_fake_generation", importedAt = now),
            tableModelMeta = TableModelMeta(
                provider = translationProvider,
                actualProvider = translationProvider,
                model = "m2_fake_classic_generator",
                fromCache = false,
                niqqudProvider = "not_implemented_m2",
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ClassicModeViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return ClassicModeViewModel(repository) as T
        }
    }
}

class ClassicGenerationShell {
    fun generate(sourceText: String): List<ClassicGeneratedRowUi> =
        splitSource(sourceText).mapIndexed { index, segment ->
            ClassicGeneratedRowUi(
                orderIndex = index,
                hebrewPlain = segment,
                hebrewNiqqud = "",
                translit = "m2-fake-sbl-${index + 1}",
                translitRu = "м2-фейк-${index + 1}",
                russian = "[M2 fake translation ${index + 1}] ${segment.take(48)}",
            )
        }

    private fun splitSource(sourceText: String): List<String> =
        sourceText
            .split('\n', '.', '!', '?', '׃', '׀')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(sourceText.trim()) }
}
