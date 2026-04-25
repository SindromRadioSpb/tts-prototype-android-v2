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
    fun attachJsonCredentialUpdatesStatus() {
        viewModel.attachJsonCredential(
            ProviderCredentialId.GoogleOnlineTts,
            """
            {
              "type": "service_account",
              "project_id": "tts-project",
              "private_key": "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----\n",
              "client_email": "tts@example.iam.gserviceaccount.com"
            }
            """.trimIndent(),
        )

        val state = viewModel.uiState.value
        val status = state.credentialStatuses.single { it.id == ProviderCredentialId.GoogleOnlineTts }
        assertTrue(status.isConfigured)
        assertTrue(status.maskedValue!!.startsWith("sa:"))
        assertEquals("Attached Google Online TTS JSON credential.", state.message)
    }

    @Test
    fun validateCredentialReportsStoredJsonAsValid() {
        viewModel.attachJsonCredential(
            ProviderCredentialId.GeminiLegacy,
            """{"provider":"gemini_legacy","api_key":"gemini-key-123456"}""",
        )

        viewModel.validateCredential(ProviderCredentialId.GeminiLegacy)

        assertEquals("Validated Gemini Legacy credential.", viewModel.uiState.value.message)
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
