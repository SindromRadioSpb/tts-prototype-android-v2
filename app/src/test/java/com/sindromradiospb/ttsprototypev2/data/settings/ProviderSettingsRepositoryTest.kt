package com.sindromradiospb.ttsprototypev2.data.settings

import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderSettingsRepositoryTest {
    private val store = InMemorySecureKeyValueStore()
    private val repository = ProviderSettingsRepository(store)

    @Test
    fun updateCredentialStoresMaskedStatusWithoutExposingFullValue() {
        val status = repository.updateCredential(
            ProviderCredentialId.GcpTranslate,
            "test-api-key-123456",
        )

        assertTrue(status.isConfigured)
        assertEquals("test...3456", status.maskedValue)
        assertEquals("test-api-key-123456", repository.getCredentialForProvider(ProviderCredentialId.GcpTranslate))
        assertFalse(repository.getStatuses().single { it.id == ProviderCredentialId.GcpTranslate }.maskedValue!!.contains("api-key"))
    }

    @Test
    fun deleteCredentialRemovesStoredValue() {
        repository.updateCredential(ProviderCredentialId.GeminiLegacy, "gemini-key-123456")

        val status = repository.deleteCredential(ProviderCredentialId.GeminiLegacy)

        assertFalse(status.isConfigured)
        assertNull(repository.getCredentialForProvider(ProviderCredentialId.GeminiLegacy))
    }

    @Test
    fun serviceAccountJsonIsRejected() {
        val error = assertFailsWithMessage("Raw service account JSON") {
            repository.updateCredential(
                ProviderCredentialId.GoogleOnlineTts,
                serviceAccountLikeValue(),
            )
        }

        assertTrue(error.message!!.contains("Raw service account JSON"))
        assertNull(repository.getCredentialForProvider(ProviderCredentialId.GoogleOnlineTts))
    }

    @Test
    fun attachJsonCredentialAcceptsServiceAccountWithoutExposingSecret() {
        val status = repository.attachJsonCredential(
            ProviderCredentialId.GcpTranslate,
            serviceAccountJson(),
        )

        assertTrue(status.isConfigured)
        assertEquals("sa:svc...oj-123", status.maskedValue)
        assertFalse(repository.getStatuses().single { it.id == ProviderCredentialId.GcpTranslate }.maskedValue!!.contains("private"))
        assertTrue(repository.getCredentialForProvider(ProviderCredentialId.GcpTranslate)!!.contains("service_account"))
    }

    @Test
    fun attachJsonCredentialAcceptsGeminiWrapper() {
        val status = repository.attachJsonCredential(
            ProviderCredentialId.GeminiLegacy,
            """{"provider":"gemini_legacy","api_key":"gemini-json-key-123456"}""",
        )

        assertTrue(status.isConfigured)
        assertEquals("gemi...3456", status.maskedValue)
    }

    @Test
    fun attachJsonCredentialRejectsWrongProviderWrapper() {
        val error = assertFailsWithMessage("provider") {
            repository.attachJsonCredential(
                ProviderCredentialId.GeminiLegacy,
                """{"provider":"gcp_translate","api_key":"gemini-json-key-123456"}""",
            )
        }

        assertTrue(error.message!!.contains("provider"))
    }

    @Test
    fun multilineCredentialIsRejected() {
        val error = assertFailsWithMessage("single-line") {
            repository.updateCredential(ProviderCredentialId.GcpTranslate, "first\nsecond")
        }

        assertTrue(error.message!!.contains("single-line"))
    }
}

private fun serviceAccountLikeValue(): String =
    """{"client_${"email"}":"service@example.com","private_${"key"}":"BEGIN ${"PRIVATE KEY"}"}"""

private fun serviceAccountJson(): String =
    """
    {
      "type": "service_account",
      "project_id": "proj-123",
      "private_key": "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----\n",
      "client_email": "svc@example.iam.gserviceaccount.com"
    }
    """.trimIndent()

class InMemorySecureKeyValueStore : SecureKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun get(key: String): String? = values[key]

    override fun put(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

private fun assertFailsWithMessage(
    expectedMessagePart: String,
    block: () -> Unit,
): Throwable {
    try {
        block()
    } catch (error: IllegalArgumentException) {
        require(error.message.orEmpty().contains(expectedMessagePart)) {
            "Expected '$expectedMessagePart' in '${error.message}'"
        }
        return error
    }
    throw AssertionError("Expected IllegalArgumentException")
}
