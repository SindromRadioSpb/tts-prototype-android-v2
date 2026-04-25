package com.sindromradiospb.ttsprototypev2.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindromradiospb.ttsprototypev2.core.model.LibraryRow
import com.sindromradiospb.ttsprototypev2.core.model.LibraryText
import com.sindromradiospb.ttsprototypev2.data.repository.EditableRowFields
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository
import com.sindromradiospb.ttsprototypev2.data.repository.RowField
import com.sindromradiospb.ttsprototypev2.data.repository.TextMetadataUpdate
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val includeArchived: Boolean = false,
    val summaries: List<LibraryTextSummary> = emptyList(),
    val selectedText: LibraryText? = null,
    val editingRowId: String? = null,
    val isAddingRow: Boolean = false,
    val addingAfterRowId: String? = null,
    val rowDraft: LibraryRowDraft = LibraryRowDraft(),
    val metadataDraft: LibraryTextMetadataDraft? = null,
    val pendingDeleteTextId: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
)

data class LibraryTextMetadataDraft(
    val textId: String,
    val title: String,
    val level: String,
    val tagsCsv: String,
    val source: String,
    val topic: String,
    val validationMessage: String? = null,
)

data class LibraryRowDraft(
    val hebrewPlain: String = "",
    val hebrewNiqqud: String = "",
    val translit: String = "",
    val translitRu: String = "",
    val russian: String = "",
) {
    fun nonBlankFields(): EditableRowFields =
        EditableRowFields(
            buildMap {
                if (hebrewPlain.isNotBlank()) put(RowField.HebrewPlain, hebrewPlain)
                if (hebrewNiqqud.isNotBlank()) put(RowField.HebrewNiqqud, hebrewNiqqud)
                if (translit.isNotBlank()) put(RowField.Translit, translit)
                if (translitRu.isNotBlank()) put(RowField.TranslitRu, translitRu)
                if (russian.isNotBlank()) put(RowField.Russian, russian)
            },
        )

    fun changedFields(row: LibraryRow): EditableRowFields =
        EditableRowFields(
            buildMap {
                if (hebrewPlain != row.hebrewPlain) put(RowField.HebrewPlain, hebrewPlain)
                if (hebrewNiqqud != row.hebrewNiqqud) put(RowField.HebrewNiqqud, hebrewNiqqud)
                if (translit != row.translit) put(RowField.Translit, translit)
                if (translitRu != row.translitRu) put(RowField.TranslitRu, translitRu)
                if (russian != row.russian) put(RowField.Russian, russian)
            },
        )

    companion object {
        fun from(row: LibraryRow): LibraryRowDraft =
            LibraryRowDraft(
                hebrewPlain = row.hebrewPlain,
                hebrewNiqqud = row.hebrewNiqqud,
                translit = row.translit,
                translitRu = row.translitRu,
                russian = row.russian,
            )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: RoomLibraryRepository,
    private val clock: () -> String = { Instant.now().toString() },
) : ViewModel() {
    private val includeArchived = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        includeArchived
            .flatMapLatest { repository.observeTexts(includeArchived = it) }
            .onEach { summaries ->
                _uiState.update { current ->
                    current.copy(
                        includeArchived = includeArchived.value,
                        summaries = summaries,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setIncludeArchived(value: Boolean) {
        includeArchived.value = value
        _uiState.update { it.copy(includeArchived = value, message = null) }
    }

    fun openText(textId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                repository.markOpened(textId, clock())
                repository.getText(textId)
            }.fold(
                onSuccess = { text ->
                    _uiState.update {
                        it.copy(
                            selectedText = text,
                            isLoading = false,
                            message = "Opened ${text.title}.",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            selectedText = null,
                            isLoading = false,
                            message = "Could not open library text: ${error.message.orEmpty()}",
                        )
                    }
                },
            )
        }
    }

    fun archiveSelected(archived: Boolean) {
        val selected = uiState.value.selectedText ?: return
        archiveText(selected.id, archived)
    }

    fun archiveText(textId: String, archived: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                repository.archiveText(textId, archived)
                repository.getText(textId)
            }.fold(
                onSuccess = { text ->
                    _uiState.update {
                        it.copy(
                            selectedText = if (it.selectedText?.id == textId) text else it.selectedText,
                            isLoading = false,
                            message = if (archived) "Archived ${text.title}." else "Restored ${text.title}.",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = "Could not update archive state: ${error.message.orEmpty()}")
                    }
                },
            )
        }
    }

    fun deleteSelected() {
        val selected = uiState.value.selectedText ?: return
        deleteText(selected.id)
    }

    fun requestDeleteText(textId: String) {
        _uiState.update { it.copy(pendingDeleteTextId = textId, message = null) }
    }

    fun cancelDeleteText() {
        _uiState.update { it.copy(pendingDeleteTextId = null, message = null) }
    }

    fun deleteText(textId: String) {
        val title = uiState.value.summaries.firstOrNull { it.textId == textId }?.title
            ?: uiState.value.selectedText?.takeIf { it.id == textId }?.title
            ?: "selected text"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                repository.deleteText(textId)
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            selectedText = if (it.selectedText?.id == textId) null else it.selectedText,
                            pendingDeleteTextId = null,
                            isLoading = false,
                            message = "Deleted $title.",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = "Could not delete library text: ${error.message.orEmpty()}")
                    }
                },
            )
        }
    }

    fun startEditingMetadata(textId: String) {
        val selected = uiState.value.selectedText?.takeIf { it.id == textId }
        val summary = uiState.value.summaries.firstOrNull { it.textId == textId }
        if (selected == null && summary == null) {
            _uiState.update { it.copy(message = "Could not find text metadata for editing.") }
            return
        }
        _uiState.update {
            it.copy(
                metadataDraft = LibraryTextMetadataDraft(
                    textId = textId,
                    title = selected?.title ?: summary?.title.orEmpty(),
                    level = selected?.level ?: summary?.level.orEmpty(),
                    tagsCsv = (selected?.tags ?: summary?.tags).orEmpty().joinToString(", "),
                    source = selected?.sourceLabel ?: summary?.sourceLabel.orEmpty(),
                    topic = selected?.topic ?: summary?.topic.orEmpty(),
                ),
                pendingDeleteTextId = null,
                message = null,
            )
        }
    }

    fun updateMetadataDraft(draft: LibraryTextMetadataDraft) {
        _uiState.update { it.copy(metadataDraft = draft.copy(validationMessage = null), message = null) }
    }

    fun cancelMetadataEdit() {
        _uiState.update { it.copy(metadataDraft = null, message = null) }
    }

    fun saveMetadata() {
        val draft = uiState.value.metadataDraft ?: return
        if (draft.title.isBlank()) {
            _uiState.update { it.copy(metadataDraft = draft.copy(validationMessage = "Title обязателен.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                repository.updateTextMetadata(
                    draft.textId,
                    TextMetadataUpdate(
                        title = draft.title,
                        level = draft.level,
                        tags = tagsFromCsv(draft.tagsCsv),
                        sourceLabel = draft.source,
                        topic = draft.topic,
                    ),
                )
            }.fold(
                onSuccess = { text ->
                    _uiState.update {
                        it.copy(
                            selectedText = if (it.selectedText?.id == text.id) text else it.selectedText,
                            metadataDraft = null,
                            isLoading = false,
                            message = "Saved metadata for ${text.title}.",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            metadataDraft = draft.copy(
                                validationMessage = "Could not save metadata: ${error.message.orEmpty()}",
                            ),
                        )
                    }
                },
            )
        }
    }

    fun startEditingRow(rowId: String) {
        val row = uiState.value.selectedText?.rows?.firstOrNull { it.id == rowId } ?: return
        _uiState.update {
            it.copy(
                editingRowId = rowId,
                isAddingRow = false,
                addingAfterRowId = null,
                rowDraft = LibraryRowDraft.from(row),
                message = null,
            )
        }
    }

    fun startAddingRow(afterRowId: String?) {
        _uiState.update {
            it.copy(
                editingRowId = null,
                isAddingRow = true,
                addingAfterRowId = afterRowId,
                rowDraft = LibraryRowDraft(),
                message = null,
            )
        }
    }

    fun updateRowDraft(draft: LibraryRowDraft) {
        _uiState.update { it.copy(rowDraft = draft, message = null) }
    }

    fun cancelRowEdit() {
        _uiState.update {
            it.copy(
                editingRowId = null,
                isAddingRow = false,
                addingAfterRowId = null,
                rowDraft = LibraryRowDraft(),
                message = null,
            )
        }
    }

    fun saveEditingRow() {
        val state = uiState.value
        val selected = state.selectedText ?: return
        val rowId = state.editingRowId ?: return
        val row = selected.rows.firstOrNull { it.id == rowId } ?: return
        val fields = state.rowDraft.changedFields(row)
        if (fields.values.isEmpty()) {
            _uiState.update { it.copy(message = "No row changes to save.") }
            return
        }
        mutateSelected("Saved row ${row.orderIndex + 1}.") {
            repository.patchRow(selected.id, rowId, fields)
            repository.getText(selected.id)
        }
    }

    fun saveNewRow() {
        val state = uiState.value
        val selected = state.selectedText ?: return
        val fields = state.rowDraft.nonBlankFields()
        if (fields.values.isEmpty()) {
            _uiState.update { it.copy(message = "Enter at least one row field before adding a row.") }
            return
        }
        mutateSelected("Added row.") {
            repository.addRow(selected.id, state.addingAfterRowId, fields)
            repository.getText(selected.id)
        }
    }

    fun resetEditingRow() {
        val state = uiState.value
        val rowId = state.editingRowId ?: return
        resetRow(rowId)
    }

    fun resetRow(rowId: String) {
        val state = uiState.value
        val selected = state.selectedText ?: return
        val row = selected.rows.firstOrNull { it.id == rowId } ?: return
        val editedFields = row.editMeta?.edited.orEmpty()
            .filterValues { it }
            .keys
            .mapNotNull { rowFieldFromStorageKey(it) }
            .toSet()
        if (editedFields.isEmpty()) {
            _uiState.update { it.copy(message = "No edited fields to reset.") }
            return
        }
        mutateSelected("Reset row ${row.orderIndex + 1}.") {
            repository.resetRowFields(selected.id, rowId, editedFields)
            repository.getText(selected.id)
        }
    }

    fun moveRow(rowId: String, offset: Int) {
        val selected = uiState.value.selectedText ?: return
        val rows = selected.rows.sortedBy { it.orderIndex }
        val currentIndex = rows.indexOfFirst { it.id == rowId }
        val nextIndex = currentIndex + offset
        if (currentIndex !in rows.indices || nextIndex !in rows.indices) return
        val nextIds = rows.map { it.id }.toMutableList()
        val moved = nextIds.removeAt(currentIndex)
        nextIds.add(nextIndex, moved)
        mutateSelected("Moved row ${currentIndex + 1} to position ${nextIndex + 1}.") {
            repository.reorderRows(selected.id, nextIds)
            repository.getText(selected.id)
        }
    }

    fun deleteRow(rowId: String) {
        val selected = uiState.value.selectedText ?: return
        val row = selected.rows.firstOrNull { it.id == rowId } ?: return
        mutateSelected("Deleted row ${row.orderIndex + 1}.") {
            repository.deleteRow(selected.id, rowId)
            repository.getText(selected.id)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun mutateSelected(successMessage: String, block: suspend () -> LibraryText) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching { block() }.fold(
                onSuccess = { text ->
                    _uiState.update {
                        it.copy(
                            selectedText = text,
                            editingRowId = null,
                            isAddingRow = false,
                            addingAfterRowId = null,
                            rowDraft = LibraryRowDraft(),
                            isLoading = false,
                            message = successMessage,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = "Could not update row: ${error.message.orEmpty()}")
                    }
                },
            )
        }
    }

    class Factory(
        private val repository: RoomLibraryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return LibraryViewModel(repository) as T
        }
    }
}

private fun rowFieldFromStorageKey(key: String): RowField? =
    when (key) {
        "hebrew_plain" -> RowField.HebrewPlain
        "hebrew_niqqud" -> RowField.HebrewNiqqud
        "translit" -> RowField.Translit
        "translit_ru" -> RowField.TranslitRu
        "russian" -> RowField.Russian
        else -> null
    }

private fun tagsFromCsv(value: String): List<String> =
    value.split(",", " ", "\n", "\t")
        .map { it.trim().removePrefix("#") }
        .filter { it.isNotEmpty() }
        .distinct()
