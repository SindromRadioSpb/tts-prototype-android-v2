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
import com.sindromradiospb.ttsprototypev2.data.audio.AudioPlaybackController
import com.sindromradiospb.ttsprototypev2.data.audio.AudioPlaybackResult
import com.sindromradiospb.ttsprototypev2.data.audio.AudioStorageRepository
import com.sindromradiospb.ttsprototypev2.data.audio.PlayableAudioResult
import com.sindromradiospb.ttsprototypev2.data.repository.GeneratedLibraryRowInput
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.SaveGeneratedTextRequest
import com.sindromradiospb.ttsprototypev2.data.repository.SaveTextResult
import com.sindromradiospb.ttsprototypev2.data.provider.translation.createAndroidTranslationProviderRegistry
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassicGeneratedRowUi(
    val rowId: String? = null,
    val orderIndex: Int,
    val hebrewPlain: String,
    val hebrewNiqqud: String,
    val translit: String,
    val translitRu: String,
    val russian: String,
    val audioAssetKey: String? = null,
    val note: String? = null,
)

enum class ClassicSourceLanguage(val code: String, val label: String) {
    Hebrew("he-IL", "🇮🇱 Иврит"),
    Russian("ru-RU", "🇷🇺 Русский"),
    English("en-US", "🇺🇸 Английский"),
}

enum class ClassicTranslitProfile(val wireId: String, val label: String) {
    Sbl("sbl", "Транслит: SBL Academic"),
    RuPhonetic("ru-phonetic", "Транслит: Русская фонетика"),
}

enum class ClassicHebrewTableFont(val wireId: String, val label: String) {
    Frank("frank", "Frank Ruhl Libre (рекомендуется)"),
    Assistant("assistant", "Assistant"),
    System("system", "Системный"),
}

data class ClassicSaveMetadataDraft(
    val title: String = "",
    val level: String = "",
    val tagsCsv: String = "classic-mode",
    val source: String = "classic_mode",
    val topic: String = "",
)

data class ClassicNoteEditorState(
    val row: ClassicGeneratedRowUi,
    val draft: String,
    val isSaving: Boolean = false,
)

enum class ClassicTableColumn(val label: String) {
    Action("▶✎"),
    Hebrew("Иврит"),
    Niqqud("Огласовки"),
    Translit("Транслит"),
    Translation("Перевод"),
}

data class ClassicTableDisplayState(
    val isColumnsPanelOpen: Boolean = false,
    val visibleColumns: Set<ClassicTableColumn> = ClassicTableColumn.entries.toSet(),
    val columnWidthsDp: Map<ClassicTableColumn, Int> = mapOf(
        ClassicTableColumn.Action to 92,
        ClassicTableColumn.Hebrew to 170,
        ClassicTableColumn.Niqqud to 190,
        ClassicTableColumn.Translit to 190,
        ClassicTableColumn.Translation to 230,
    ),
    val selectedRowIndex: Int? = null,
    val isAutoNextActive: Boolean = false,
)

enum class ClassicDisclosurePanel {
    Source,
    Voice,
    Translation,
    Result,
}

data class ClassicDisclosureState(
    val sourceOpen: Boolean = true,
    val voiceOpen: Boolean = true,
    val translationOpen: Boolean = true,
    val resultOpen: Boolean = true,
) {
    fun isOpen(panel: ClassicDisclosurePanel): Boolean =
        when (panel) {
            ClassicDisclosurePanel.Source -> sourceOpen
            ClassicDisclosurePanel.Voice -> voiceOpen
            ClassicDisclosurePanel.Translation -> translationOpen
            ClassicDisclosurePanel.Result -> resultOpen
        }

    fun toggled(panel: ClassicDisclosurePanel): ClassicDisclosureState =
        when (panel) {
            ClassicDisclosurePanel.Source -> copy(sourceOpen = !sourceOpen)
            ClassicDisclosurePanel.Voice -> copy(voiceOpen = !voiceOpen)
            ClassicDisclosurePanel.Translation -> copy(translationOpen = !translationOpen)
            ClassicDisclosurePanel.Result -> copy(resultOpen = !resultOpen)
        }
}

data class ClassicModeUiState(
    val sourceText: String = "שלום, זהו אבטיפוס פשוט של המערכת",
    val sourceLanguage: ClassicSourceLanguage = ClassicSourceLanguage.Hebrew,
    val translationProvider: TranslationProviderId = TranslationProviderId.GoogleTranslateFree,
    val ttsProvider: TtsProviderId = TtsProviderId.GoogleOnlineTts,
    val ttsVoiceName: String? = null,
    val speakingRate: Double = 1.0,
    val pitch: Double = 0.0,
    val translitProfile: ClassicTranslitProfile = ClassicTranslitProfile.Sbl,
    val hebrewTableFont: ClassicHebrewTableFont = ClassicHebrewTableFont.Frank,
    val rows: List<ClassicGeneratedRowUi> = emptyList(),
    val libraryTexts: List<LibraryTextSummary> = emptyList(),
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val isSpeaking: Boolean = false,
    val playingRowIndex: Int? = null,
    val tableDisplay: ClassicTableDisplayState = ClassicTableDisplayState(),
    val disclosure: ClassicDisclosureState = ClassicDisclosureState(),
    val savedTextId: String? = null,
    val loadedTextTitle: String? = null,
    val loadedTextSourceLabel: String? = null,
    val saveMetadataDraft: ClassicSaveMetadataDraft? = null,
    val noteEditor: ClassicNoteEditorState? = null,
    val generatedAt: String? = null,
    val generationLabel: String = "Generation: not started",
    val provenance: ProviderProvenance? = null,
    val message: String? = null,
)

class ClassicModeViewModel(
    private val repository: LibraryRepository,
    private val translationProviders: TranslationProviderRegistry = createAndroidTranslationProviderRegistry(),
    private val ttsProviders: TtsProviderRegistry? = null,
    private val audioStorageRepository: AudioStorageRepository? = null,
    private val audioPlaybackController: AudioPlaybackController? = null,
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
                loadedTextTitle = null,
                loadedTextSourceLabel = null,
                message = null,
            )
        }
    }

    fun onTranslationProviderChanged(providerId: TranslationProviderId) {
        val current = uiState.value
        if (current.translationProvider == providerId) return
        val shouldRegenerate = current.rows.isNotEmpty() &&
            current.sourceText.isNotBlank() &&
            !current.isGenerating &&
            !current.isSaving
        _uiState.update {
            it.copy(
                translationProvider = providerId,
                savedTextId = null,
                loadedTextTitle = null,
                loadedTextSourceLabel = null,
                saveMetadataDraft = null,
                message = if (shouldRegenerate) "Переводчик изменён. Пересобираю таблицу..." else null,
            )
        }
        if (shouldRegenerate) {
            generateTable()
        }
    }

    fun onTtsProviderChanged(providerId: TtsProviderId) {
        val isSystemFallback = providerId == TtsProviderId.SystemFallbackLowQuality
        _uiState.update {
            it.copy(
                ttsProvider = providerId,
                ttsVoiceName = if (isSystemFallback) null else it.ttsVoiceName,
                speakingRate = if (isSystemFallback) 1.0 else it.speakingRate,
                pitch = if (isSystemFallback) 0.0 else it.pitch,
                savedTextId = null,
                loadedTextTitle = null,
                loadedTextSourceLabel = null,
                message = null,
            )
        }
    }

    fun onSourceLanguageChanged(language: ClassicSourceLanguage) {
        _uiState.update { it.copy(sourceLanguage = language, savedTextId = null, loadedTextTitle = null, loadedTextSourceLabel = null, message = null) }
    }

    fun onTtsVoiceNameChanged(voiceName: String?) {
        _uiState.update { it.copy(ttsVoiceName = voiceName?.takeIf { value -> value.isNotBlank() }, savedTextId = null, loadedTextTitle = null, loadedTextSourceLabel = null, message = null) }
    }

    fun onSpeakingRateChanged(value: Double) {
        _uiState.update { it.copy(speakingRate = value.coerceIn(0.5, 2.0), savedTextId = null, loadedTextTitle = null, loadedTextSourceLabel = null, message = null) }
    }

    fun onPitchChanged(value: Double) {
        _uiState.update { it.copy(pitch = value.coerceIn(-5.0, 5.0), savedTextId = null, loadedTextTitle = null, loadedTextSourceLabel = null, message = null) }
    }

    fun onTranslitProfileChanged(profile: ClassicTranslitProfile) {
        _uiState.update { it.copy(translitProfile = profile, message = null) }
    }

    fun onHebrewTableFontChanged(font: ClassicHebrewTableFont) {
        _uiState.update { it.copy(hebrewTableFont = font, message = null) }
    }

    fun toggleTableColumnsPanel() {
        _uiState.update {
            it.copy(
                tableDisplay = it.tableDisplay.copy(isColumnsPanelOpen = !it.tableDisplay.isColumnsPanelOpen),
                message = null,
            )
        }
    }

    fun toggleTableColumn(column: ClassicTableColumn) {
        _uiState.update { current ->
            val visible = current.tableDisplay.visibleColumns
            val next = if (column in visible) visible - column else visible + column
            if (next.isEmpty()) {
                current.copy(message = "Нельзя скрыть все колонки. Минимум одна должна оставаться видимой.")
            } else {
                current.copy(
                    tableDisplay = current.tableDisplay.copy(visibleColumns = next),
                    message = null,
                )
            }
        }
    }

    fun resetTableDisplay() {
        _uiState.update {
            it.copy(
                tableDisplay = ClassicTableDisplayState(isColumnsPanelOpen = it.tableDisplay.isColumnsPanelOpen),
                message = "Настройки таблицы сброшены.",
            )
        }
    }

    fun toggleDisclosure(panel: ClassicDisclosurePanel) {
        _uiState.update {
            it.copy(
                disclosure = it.disclosure.toggled(panel),
                message = null,
            )
        }
    }

    fun adjustTableColumnWidth(column: ClassicTableColumn, deltaDp: Float) {
        if (deltaDp == 0f) return
        _uiState.update { current ->
            val widths = current.tableDisplay.columnWidthsDp.toMutableMap()
            val currentWidth = widths[column] ?: 120
            widths[column] = (currentWidth + deltaDp.toInt()).coerceIn(64, 420)
            current.copy(tableDisplay = current.tableDisplay.copy(columnWidthsDp = widths))
        }
    }

    fun selectRow(orderIndex: Int) {
        _uiState.update {
            it.copy(
                tableDisplay = it.tableDisplay.copy(selectedRowIndex = orderIndex),
                message = null,
            )
        }
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
                    sourceLanguage = current.sourceLanguage.code,
                    providerId = current.translationProvider,
                ),
            )
            result.fold(
                onSuccess = { response ->
                    val rows = response.rows.map {
                        ClassicGeneratedRowUi(
                            rowId = null,
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
                            loadedTextTitle = null,
                            loadedTextSourceLabel = null,
                            playingRowIndex = null,
                            tableDisplay = it.tableDisplay.copy(
                                selectedRowIndex = null,
                                isAutoNextActive = false,
                            ),
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
        _uiState.update {
            it.copy(
                saveMetadataDraft = current.saveMetadataDraft ?: current.defaultMetadataDraft(),
                message = null,
            )
        }
    }

    fun updateSaveMetadataDraft(draft: ClassicSaveMetadataDraft) {
        _uiState.update { it.copy(saveMetadataDraft = draft, message = null) }
    }

    fun cancelSaveMetadata() {
        _uiState.update { it.copy(saveMetadataDraft = null, message = null) }
    }

    fun commitSaveMetadata() {
        val current = uiState.value
        val draft = current.saveMetadataDraft ?: current.defaultMetadataDraft()
        if (current.isSaving || current.isGenerating) {
            return
        }
        if (current.rows.isEmpty()) {
            _uiState.update { it.copy(message = "Сначала сгенерируйте строки.") }
            return
        }
        if (draft.title.isBlank()) {
            _uiState.update { it.copy(message = "TITLE обязателен для карточки текста.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            val request = current.toSaveRequest(clock(), draft)
            val result = if (current.savedTextId == null) {
                repository.saveGeneratedText(request)
            } else {
                repository.updateGeneratedText(current.savedTextId, request)
            }
            when (result) {
                is SaveTextResult.Saved -> {
                    val saved = result.text
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            rows = saved.rows.sortedBy { row -> row.orderIndex }.map { row ->
                                ClassicGeneratedRowUi(
                                    rowId = row.id,
                                    orderIndex = row.orderIndex,
                                    hebrewPlain = row.hebrewPlain,
                                    hebrewNiqqud = row.hebrewNiqqud,
                                    translit = row.translit,
                                    translitRu = row.translitRu,
                                    russian = row.russian,
                                    audioAssetKey = row.audioAssetKey,
                                    note = row.note,
                                )
                            },
                            savedTextId = saved.id,
                            loadedTextTitle = saved.title,
                            loadedTextSourceLabel = saved.sourceLabel,
                            saveMetadataDraft = null,
                            message = "Карточка текста сохранена в Library.",
                        )
                    }
                }
                is SaveTextResult.Conflict -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            savedTextId = result.existingTextId,
                            loadedTextTitle = null,
                            loadedTextSourceLabel = null,
                            saveMetadataDraft = null,
                            message = "Такая карточка уже есть в локальной библиотеке.",
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
                            sourceLanguage = ClassicSourceLanguage.entries.firstOrNull { lang -> lang.code == text.ttsProfile?.language }
                                ?: it.sourceLanguage,
                            ttsVoiceName = text.ttsProfile?.voiceName,
                            speakingRate = text.ttsProfile?.speakingRate ?: it.speakingRate,
                            pitch = text.ttsProfile?.pitch ?: it.pitch,
                            rows = text.rows.sortedBy { row -> row.orderIndex }.map { row ->
                                ClassicGeneratedRowUi(
                                    rowId = row.id,
                                    orderIndex = row.orderIndex,
                                    hebrewPlain = row.hebrewPlain,
                                    hebrewNiqqud = row.hebrewNiqqud,
                                    translit = row.translit,
                                    translitRu = row.translitRu,
                                    russian = row.russian,
                                    audioAssetKey = row.audioAssetKey,
                                    note = row.note,
                                )
                            },
                            isGenerating = false,
                            savedTextId = text.id,
                            loadedTextTitle = text.title,
                            loadedTextSourceLabel = text.sourceLabel,
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
            val profile = current.toTtsProfile()
            val provider = registry.get(current.ttsProvider)
            val result = provider.synthesize(
                TtsRequest(
                    text = current.sourceText,
                    profile = profile,
                ),
            )
            result.fold(
                onSuccess = { response ->
                    val playbackMessage = response.localFilePath
                        ?.let { playFile(File(it)).toUserMessage() }
                        ?.let { " Playback: $it" }
                        .orEmpty()
                    _uiState.update {
                        it.copy(
                            isSpeaking = false,
                            message = "TTS complete via ${response.provenance.actualProviderId}: ${response.localFileName.orEmpty()}.$playbackMessage",
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

    fun playRow(orderIndex: Int) {
        startRowPlayback(orderIndex, autoAdvance = false)
    }

    fun toggleAutoNextPlayback() {
        val current = uiState.value
        if (current.tableDisplay.isAutoNextActive) {
            audioPlaybackController?.stop()
            _uiState.update {
                it.copy(
                    playingRowIndex = null,
                    tableDisplay = it.tableDisplay.copy(isAutoNextActive = false),
                    message = "Построчное воспроизведение остановлено.",
                )
            }
            return
        }
        if (current.rows.isEmpty()) {
            _uiState.update { it.copy(message = "Нет строк для построчного воспроизведения.") }
            return
        }
        val startIndex = current.tableDisplay.selectedRowIndex
            ?.takeIf { it in current.rows.indices }
            ?: 0
        _uiState.update {
            it.copy(
                tableDisplay = it.tableDisplay.copy(
                    selectedRowIndex = startIndex,
                    isAutoNextActive = true,
                ),
                message = null,
            )
        }
        startRowPlayback(startIndex, autoAdvance = true)
    }

    private fun startRowPlayback(orderIndex: Int, autoAdvance: Boolean) {
        val current = uiState.value
        val row = current.rows.firstOrNull { it.orderIndex == orderIndex }
        if (row == null) {
            _uiState.update {
                it.copy(
                    playingRowIndex = null,
                    tableDisplay = it.tableDisplay.copy(isAutoNextActive = false),
                    message = "Строка не найдена.",
                )
            }
            return
        }
        val registry = ttsProviders
        if (registry == null) {
            _uiState.update {
                it.copy(
                    playingRowIndex = null,
                    tableDisplay = it.tableDisplay.copy(isAutoNextActive = false),
                    message = "TTS registry is not available in this build path.",
                )
            }
            return
        }
        val textForTts = row.hebrewNiqqud.ifBlank { row.hebrewPlain }.trim()
        if (textForTts.isBlank()) {
            _uiState.update {
                it.copy(
                    playingRowIndex = null,
                    tableDisplay = it.tableDisplay.copy(isAutoNextActive = false),
                    message = "В строке нет текста для озвучки.",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    playingRowIndex = orderIndex,
                    tableDisplay = it.tableDisplay.copy(selectedRowIndex = orderIndex),
                    message = null,
                )
            }

            val profile = current.toTtsProfile()
            val ttsRequest = TtsRequest(
                text = textForTts,
                profile = profile,
                libraryTextId = current.savedTextId,
                libraryRowId = row.rowId,
            )
            // Try linked audio first (works for both locally-synthesized and web-imported audio).
            // We skip the expected-key equality check so web-imported assets (different hash
            // algorithm) are played from the library cache instead of re-synthesized.
            val linkedAssetKey = row.audioAssetKey?.takeIf { it.isNotBlank() }
            val storage = audioStorageRepository
            if (storage != null && linkedAssetKey != null) {
                when (val cached = storage.resolvePlayableAudio(linkedAssetKey)) {
                    is PlayableAudioResult.Available -> {
                        val playback = playFile(cached.absoluteFile) {
                            onRowPlaybackCompleted(orderIndex, autoAdvance)
                        }
                        _uiState.update {
                            it.copy(
                                playingRowIndex = if (playback == AudioPlaybackResult.Started) orderIndex else null,
                                tableDisplay = it.tableDisplay.copy(
                                    isAutoNextActive = it.tableDisplay.isAutoNextActive && playback == AudioPlaybackResult.Started,
                                ),
                                message = "Row audio from cache: ${playback.toUserMessage()}",
                            )
                        }
                        return@launch
                    }
                    is PlayableAudioResult.Missing -> Unit
                }
            }

            val result = registry.get(current.ttsProvider).synthesize(ttsRequest)

            result.fold(
                onSuccess = { response ->
                    val adoptedFile = if (
                        storage != null &&
                        current.savedTextId != null &&
                        row.rowId != null &&
                        response.localFilePath != null &&
                        response.audioAssetKey != null
                    ) {
                        runCatching {
                            storage.adoptRowAudio(current.savedTextId, row.rowId, response).absoluteFile
                        }.getOrNull()
                    } else {
                        response.localFilePath?.let(::File)
                    }
                    val playback = adoptedFile?.let {
                        playFile(it) {
                            onRowPlaybackCompleted(orderIndex, autoAdvance)
                        }
                    } ?: AudioPlaybackResult.Failed("audio file unavailable")
                    val refreshedRows = if (current.savedTextId != null) {
                        runCatching { repository.getText(current.savedTextId) }
                            .getOrNull()
                            ?.rows
                            ?.sortedBy { libraryRow -> libraryRow.orderIndex }
                            ?.map { libraryRow ->
                                ClassicGeneratedRowUi(
                                    rowId = libraryRow.id,
                                    orderIndex = libraryRow.orderIndex,
                                    hebrewPlain = libraryRow.hebrewPlain,
                                    hebrewNiqqud = libraryRow.hebrewNiqqud,
                                    translit = libraryRow.translit,
                                    translitRu = libraryRow.translitRu,
                                    russian = libraryRow.russian,
                                    audioAssetKey = libraryRow.audioAssetKey,
                                    note = libraryRow.note,
                                )
                            }
                    } else {
                        null
                    }
                    _uiState.update {
                        it.copy(
                            rows = refreshedRows ?: it.rows,
                            playingRowIndex = if (playback == AudioPlaybackResult.Started) orderIndex else null,
                            tableDisplay = it.tableDisplay.copy(
                                isAutoNextActive = it.tableDisplay.isAutoNextActive && playback == AudioPlaybackResult.Started,
                            ),
                            message = "Row TTS via ${response.provenance.actualProviderId}: ${playback.toUserMessage()}",
                        )
                    }
                },
                onFailure = { error ->
                    val providerError = error as? ProviderException
                    val message = if (providerError != null) {
                        "Row TTS failed (${providerError.category}): ${providerError.userMessage}"
                    } else {
                        "Row TTS failed: ${error.message.orEmpty().ifBlank { "Unknown provider error." }}"
                    }
                    _uiState.update {
                        it.copy(
                            playingRowIndex = null,
                            tableDisplay = it.tableDisplay.copy(isAutoNextActive = false),
                            message = message,
                        )
                    }
                },
            )
        }
    }

    private fun onRowPlaybackCompleted(orderIndex: Int, autoAdvance: Boolean) {
        viewModelScope.launch {
            val current = uiState.value
            if (autoAdvance && current.tableDisplay.isAutoNextActive) {
                val nextIndex = orderIndex + 1
                if (nextIndex < current.rows.size) {
                    startRowPlayback(nextIndex, autoAdvance = true)
                } else {
                    _uiState.update {
                        it.copy(
                            playingRowIndex = null,
                            tableDisplay = it.tableDisplay.copy(
                                selectedRowIndex = orderIndex,
                                isAutoNextActive = false,
                            ),
                            message = "Построчное воспроизведение завершено.",
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        playingRowIndex = null,
                        tableDisplay = it.tableDisplay.copy(selectedRowIndex = orderIndex),
                    )
                }
            }
        }
    }

    fun openRowNote(orderIndex: Int) {
        val current = uiState.value
        val row = current.rows.firstOrNull { it.orderIndex == orderIndex }
        if (row == null) {
            _uiState.update { it.copy(message = "Строка не найдена.") }
            return
        }
        if (current.savedTextId == null || row.rowId == null) {
            _uiState.update { it.copy(message = "Сначала нажмите «Обновить» и сохраните карточку текста, затем добавьте заметку к строке.") }
            return
        }
        _uiState.update {
            it.copy(
                noteEditor = ClassicNoteEditorState(row = row, draft = row.note.orEmpty()),
                message = null,
            )
        }
    }

    fun updateRowNoteDraft(value: String) {
        _uiState.update { current ->
            current.copy(noteEditor = current.noteEditor?.copy(draft = value.take(16_000)))
        }
    }

    fun closeRowNote() {
        _uiState.update { it.copy(noteEditor = null, message = null) }
    }

    fun saveRowNote() {
        val current = uiState.value
        val editor = current.noteEditor ?: return
        val textId = current.savedTextId
        val rowId = editor.row.rowId
        if (textId == null || rowId == null) {
            _uiState.update { it.copy(message = "Сначала сохраните карточку текста.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(noteEditor = editor.copy(isSaving = true), message = null) }
            runCatching {
                repository.saveRowNote(textId, rowId, editor.draft)
                repository.getText(textId)
            }.fold(
                onSuccess = { text ->
                    _uiState.update {
                        it.copy(
                            rows = text.rows.sortedBy { row -> row.orderIndex }.map { row ->
                                ClassicGeneratedRowUi(
                                    rowId = row.id,
                                    orderIndex = row.orderIndex,
                                    hebrewPlain = row.hebrewPlain,
                                    hebrewNiqqud = row.hebrewNiqqud,
                                    translit = row.translit,
                                    translitRu = row.translitRu,
                                    russian = row.russian,
                                    audioAssetKey = row.audioAssetKey,
                                    note = row.note,
                                )
                            },
                            noteEditor = null,
                            message = if (editor.draft.isBlank()) "Заметка удалена." else "Заметка сохранена.",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            noteEditor = editor.copy(isSaving = false),
                            message = "Не удалось сохранить заметку: ${error.message.orEmpty()}",
                        )
                    }
                },
            )
        }
    }

    fun deleteRowNote() {
        val editor = uiState.value.noteEditor ?: return
        _uiState.update { it.copy(noteEditor = editor.copy(draft = "")) }
        saveRowNote()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun ClassicModeUiState.toSaveRequest(now: String, draft: ClassicSaveMetadataDraft): SaveGeneratedTextRequest =
        SaveGeneratedTextRequest(
            title = draft.title.trim(),
            level = draft.level.trim().takeIf { it.isNotEmpty() },
            tags = draft.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            sourceLabel = draft.source.trim().takeIf { it.isNotEmpty() },
            topic = draft.topic.trim().takeIf { it.isNotEmpty() },
            sourceText = sourceText,
            sourceMeta = SourceMeta(origin = "classic_mode_v4_mobile", importedAt = now),
            tableModelMeta = TableModelMeta(
                provider = translationProvider,
                actualProvider = providerIdFromWireId(provenance?.actualProviderId) ?: translationProvider,
                model = provenance?.model,
                fromCache = false,
                niqqudProvider = "not_implemented_v4",
                niqqudDegraded = true,
                generatedAt = generatedAt ?: now,
            ),
            ttsProfile = toTtsProfile(),
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

    private fun ClassicModeUiState.defaultMetadataDraft(): ClassicSaveMetadataDraft =
        ClassicSaveMetadataDraft(
            title = sourceText.trim().lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
                ?.take(80)
                ?: "Classic Mode text",
            level = "",
            tagsCsv = "classic-mode",
            source = "classic_mode",
            topic = "",
        )

    private fun ClassicModeUiState.toTtsProfile(): TtsProfile =
        TtsProfile(
            providerId = ttsProvider,
            language = sourceLanguage.code,
            voiceName = ttsVoiceName,
            speakingRate = speakingRate,
            pitch = pitch,
        )

    private fun playFile(file: File, onCompletion: (() -> Unit)? = null): AudioPlaybackResult =
        audioPlaybackController?.play(file, onCompletion)
            ?: AudioPlaybackResult.Failed("playback controller unavailable")

    private fun AudioPlaybackResult.toUserMessage(): String =
        when (this) {
            AudioPlaybackResult.Started -> "started"
            is AudioPlaybackResult.Failed -> message
        }

    class Factory(
        private val repository: LibraryRepository,
        private val translationProviders: TranslationProviderRegistry = createAndroidTranslationProviderRegistry(),
        private val ttsProviders: TtsProviderRegistry? = null,
        private val audioStorageRepository: AudioStorageRepository? = null,
        private val audioPlaybackController: AudioPlaybackController? = null,
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
                audioStorageRepository = audioStorageRepository,
                audioPlaybackController = audioPlaybackController,
            ) as T
        }
    }
}
