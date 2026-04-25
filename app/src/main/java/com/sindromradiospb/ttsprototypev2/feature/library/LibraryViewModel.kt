package com.sindromradiospb.ttsprototypev2.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sindromradiospb.ttsprototypev2.core.model.LibraryText
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository
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
    val isLoading: Boolean = false,
    val message: String? = null,
)

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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                repository.archiveText(selected.id, archived)
                repository.getText(selected.id)
            }.fold(
                onSuccess = { text ->
                    _uiState.update {
                        it.copy(
                            selectedText = text,
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                repository.deleteText(selected.id)
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            selectedText = null,
                            isLoading = false,
                            message = "Deleted ${selected.title}.",
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
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
