package com.sindromradiospb.ttsprototypev2.feature.settings

import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.data.settings.InMemorySecureKeyValueStore
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    private val repository = ProviderSettingsRepository(InMemorySecureKeyValueStore())
    private val viewModel = SettingsViewModel(repository)

    @Test
    fun saveCredentialUpdatesStatusAndClearsDraft() {
        viewModel.onCredentialDraftChanged(ProviderCredentialId.GcpTranslate, "gcp-key-123456")

        viewModel.saveCredential(ProviderCredentialId.GcpTranslate)

        val state = viewModel.uiState.value
        val status = state.credentialStatuses.single { it.id == ProviderCredentialId.GcpTranslate }
        assertTrue(status.isConfigured)
        assertEquals("gcp-...3456", status.maskedValue)
        assertFalse(state.drafts.containsKey(ProviderCredentialId.GcpTranslate))
        assertEquals("Saved GCP Translate credential.", state.message)
    }

    @Test
    fun invalidCredentialLeavesStatusMissingAndShowsValidationMessage() {
        viewModel.onCredentialDraftChanged(
            ProviderCredentialId.GoogleOnlineTts,
            """{"private_${"key"}":"BEGIN ${"PRIVATE KEY"}"}""",
        )

        viewModel.saveCredential(ProviderCredentialId.GoogleOnlineTts)

        val state = viewModel.uiState.value
        val status = state.credentialStatuses.single { it.id == ProviderCredentialId.GoogleOnlineTts }
        assertFalse(status.isConfigured)
        assertTrue(state.message!!.contains("Raw service account JSON"))
    }

    @Test
    fun deleteCredentialClearsStatus() {
        viewModel.onCredentialDraftChanged(ProviderCredentialId.GeminiLegacy, "gemini-key-123456")
        viewModel.saveCredential(ProviderCredentialId.GeminiLegacy)

        viewModel.deleteCredential(ProviderCredentialId.GeminiLegacy)

        val state = viewModel.uiState.value
        val status = state.credentialStatuses.single { it.id == ProviderCredentialId.GeminiLegacy }
        assertFalse(status.isConfigured)
        assertEquals("Deleted Gemini Legacy credential.", state.message)
    }
}
