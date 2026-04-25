package com.sindromradiospb.ttsprototypev2.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialStatus
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val credentialStatuses: List<ProviderCredentialStatus> = emptyList(),
    val drafts: Map<ProviderCredentialId, String> = emptyMap(),
    val message: String? = null,
)

class SettingsViewModel(
    private val repository: ProviderSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(credentialStatuses = repository.getStatuses()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onCredentialDraftChanged(id: ProviderCredentialId, value: String) {
        _uiState.update {
            it.copy(
                drafts = it.drafts + (id to value),
                message = null,
            )
        }
    }

    fun saveCredential(id: ProviderCredentialId) {
        val value = uiState.value.drafts[id].orEmpty()
        runCatching {
            repository.updateCredential(id, value)
            repository.getStatuses()
        }.fold(
            onSuccess = { statuses ->
                _uiState.update {
                    it.copy(
                        credentialStatuses = statuses,
                        drafts = it.drafts - id,
                        message = "Saved ${id.displayName} credential.",
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(message = error.message ?: "Could not save provider credential.")
                }
            },
        )
    }

    fun attachJsonCredential(id: ProviderCredentialId, rawJson: String) {
        runCatching {
            repository.attachJsonCredential(id, rawJson)
            repository.getStatuses()
        }.fold(
            onSuccess = { statuses ->
                _uiState.update {
                    it.copy(
                        credentialStatuses = statuses,
                        drafts = it.drafts - id,
                        message = "Attached ${id.displayName} JSON credential.",
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(message = error.message ?: "Could not attach provider JSON credential.")
                }
            },
        )
    }

    fun validateCredential(id: ProviderCredentialId) {
        runCatching {
            repository.validateStoredCredential(id)
            repository.getStatuses()
        }.fold(
            onSuccess = { statuses ->
                _uiState.update {
                    it.copy(
                        credentialStatuses = statuses,
                        message = "Validated ${id.displayName} credential format.",
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(message = error.message ?: "Credential validation failed.")
                }
            },
        )
    }

    fun deleteCredential(id: ProviderCredentialId) {
        repository.deleteCredential(id)
        _uiState.update {
            it.copy(
                credentialStatuses = repository.getStatuses(),
                drafts = it.drafts - id,
                message = "Deleted ${id.displayName} credential.",
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    class Factory(
        private val repository: ProviderSettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return SettingsViewModel(repository) as T
        }
    }
}
