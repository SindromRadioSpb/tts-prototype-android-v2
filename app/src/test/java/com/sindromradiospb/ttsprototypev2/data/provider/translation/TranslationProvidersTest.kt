package com.sindromradiospb.ttsprototypev2.data.provider.translation

import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationRequest
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.data.provider.google.GoogleAccessTokenProvider
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderCredentialMaterial
import com.sindromradiospb.ttsprototypev2.data.settings.InMemorySecureKeyValueStore
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderSettingsRepository
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationProvidersTest {
    @Test
    fun fakeTranslationProviderReturnsDeterministicRows() = runTest {
        val provider = FakeTranslationProvider(
            id = TranslationProviderId.GeminiLegacy,
            clock = { "2026-04-25T03:00:00Z" },
        )

        val result = provider.translate(
            TranslationRequest(
                sourceText = "שלום עולם\nבדיקה",
                providerId = TranslationProviderId.GeminiLegacy,
            ),
        )

        val response = result.getOrThrow()
        assertEquals("gemini_legacy", response.provenance.actualProviderId)
        assertEquals("m3_fake_translation_provider", response.provenance.model)
        assertEquals(2, response.rows.size)
        assertEquals("m3-fake-sbl-1", response.rows.first().translit)
    }

    @Test
    fun registryRejectsMissingAllowedProviderAtLookup() {
        val registry = TranslationProviderRegistry(
            listOf(FakeTranslationProvider(id = TranslationProviderId.GoogleTranslateFree)),
        )

        val error = assertProviderException {
            registry.get(TranslationProviderId.GcpTranslate)
        }

        assertEquals(ProviderErrorCategory.MissingConfiguration, error.category)
        assertEquals("gcp_translate", error.providerId)
    }

    @Test
    fun missingConfigurationProviderDoesNotFallback() = runTest {
        val provider = MissingConfigurationTranslationProvider(
            id = TranslationProviderId.GcpTranslate,
            message = "secure settings required",
        )

        val result = provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GcpTranslate),
        )

        val error = result.exceptionOrNull() as ProviderException
        assertEquals(ProviderErrorCategory.MissingConfiguration, error.category)
        assertEquals("gcp_translate", error.providerId)
    }

    @Test
    fun googleFreeParserReadsNestedTranslationEnvelope() {
        val provider = GoogleTranslateFreeProvider(clock = { "2026-04-25T03:00:00Z" })

        val translated = provider.parseGoogleFreeTranslation(
            """[[["Привет","שלום",null,null,10],[", мир"," עולם",null,null,10]],null,"he"]""",
        )

        assertEquals("Привет, мир", translated)
    }

    @Test
    fun googleFreeMapsHttpErrorsToProviderCategories() = runTest {
        val provider = GoogleTranslateFreeProvider(
            httpClient = StaticHttpClient(TranslationHttpResponse(statusCode = 429, body = "quota")),
        )

        val result = provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GoogleTranslateFree),
        )

        val error = result.exceptionOrNull() as ProviderException
        assertEquals(ProviderErrorCategory.QuotaExceeded, error.category)
    }

    @Test
    fun googleFreeMapsIoFailureToNetworkUnavailable() = runTest {
        val provider = GoogleTranslateFreeProvider(
            httpClient = FailingHttpClient(IOException("offline")),
        )

        val result = provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GoogleTranslateFree),
        )

        val error = result.exceptionOrNull() as ProviderException
        assertEquals(ProviderErrorCategory.NetworkUnavailable, error.category)
    }

    @Test
    fun googleFreeUsesPrototypeCompatibleBatchRequest() = runTest {
        val httpClient = StaticHttpClient(
            TranslationHttpResponse(
                statusCode = 200,
                body = """[[["Привет\nМир","שלום\nעולם",null,null,10]],null,"iw"]""",
            ),
        )
        val provider = GoogleTranslateFreeProvider(httpClient = httpClient)

        val response = provider.translate(
            TranslationRequest(
                sourceText = "שלום\nעולם",
                providerId = TranslationProviderId.GoogleTranslateFree,
            ),
        ).getOrThrow()

        assertEquals(2, response.rows.size)
        assertEquals("Привет", response.rows[0].russian)
        assertEquals("Мир", response.rows[1].russian)
        assertTrue(httpClient.lastUri.toString().contains("sl=iw"))
        assertEquals("Mozilla/5.0", httpClient.lastHeaders["User-Agent"])
        assertEquals("google-free-gtx-v1", response.provenance.model)
    }

    @Test
    fun gcpTranslateUsesStoredCredentialAndParsesResponse() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.updateCredential(ProviderCredentialId.GcpTranslate, "gcp-key")
        val provider = GcpTranslateProvider(
            settingsRepository = settings,
            httpClient = StaticHttpClient(
                TranslationHttpResponse(
                    statusCode = 200,
                    body = """{"data":{"translations":[{"translatedText":"Привет"}]}}""",
                ),
            ),
            clock = { "2026-04-25T03:00:00Z" },
        )

        val response = provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GcpTranslate),
        ).getOrThrow()

        assertEquals("Привет", response.rows.single().russian)
        assertEquals("cloud_translation_basic_v2", response.provenance.model)
    }

    @Test
    fun gcpTranslateUsesServiceAccountBearerTokenWhenJsonCredentialIsAttached() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.attachJsonCredential(ProviderCredentialId.GcpTranslate, serviceAccountJson())
        val httpClient = StaticHttpClient(
            TranslationHttpResponse(
                statusCode = 200,
                body = """{"data":{"translations":[{"translatedText":"Привет"}]}}""",
            ),
        )
        val provider = GcpTranslateProvider(
            settingsRepository = settings,
            httpClient = httpClient,
            accessTokenProvider = FakeGoogleAccessTokenProvider("access-token"),
            clock = { "2026-04-25T03:00:00Z" },
        )

        provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GcpTranslate),
        ).getOrThrow()

        assertEquals("https://translation.googleapis.com/v3/projects/proj-123/locations/global:translateText", httpClient.lastUri.toString())
        assertEquals("Bearer access-token", httpClient.lastHeaders["Authorization"])
    }

    @Test
    fun gcpTranslateMissingCredentialDoesNotFallback() = runTest {
        val provider = GcpTranslateProvider(
            settingsRepository = ProviderSettingsRepository(InMemorySecureKeyValueStore()),
            httpClient = StaticHttpClient(TranslationHttpResponse(statusCode = 200, body = "{}")),
        )

        val result = provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GcpTranslate),
        )

        val error = result.exceptionOrNull() as ProviderException
        assertEquals(ProviderErrorCategory.MissingConfiguration, error.category)
    }

    @Test
    fun geminiLegacyUsesStoredCredentialAndParsesResponse() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.updateCredential(ProviderCredentialId.GeminiLegacy, "gemini-key")
        val httpClient = StaticHttpClient(
            TranslationHttpResponse(
                statusCode = 200,
                body = geminiTableResponse(),
            ),
        )
        val provider = GeminiLegacyTranslationProvider(
            settingsRepository = settings,
            httpClient = httpClient,
            clock = { "2026-04-25T03:00:00Z" },
        )

        val response = provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GeminiLegacy),
        ).getOrThrow()

        assertEquals("שָׁלוֹם", response.rows.single().hebrewNiqqud)
        assertEquals("shalom", response.rows.single().translit)
        assertEquals("Здравствуйте", response.rows.single().russian)
        assertEquals("gemini-flash-latest", response.provenance.model)
        assertTrue(httpClient.lastBody.contains("responseMimeType"))
    }

    @Test
    fun geminiLegacyAcceptsJsonWrapperCredential() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.attachJsonCredential(
            ProviderCredentialId.GeminiLegacy,
            """{"provider":"gemini_legacy","api_key":"gemini-json-key"}""",
        )
        val httpClient = StaticHttpClient(
            TranslationHttpResponse(
                statusCode = 200,
                body = geminiTableResponse(),
            ),
        )
        val provider = GeminiLegacyTranslationProvider(
            settingsRepository = settings,
            httpClient = httpClient,
        )

        provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GeminiLegacy),
        ).getOrThrow()

        assertTrue(httpClient.lastUri.toString().contains("key=gemini-json-key"))
    }

    @Test
    fun geminiLegacyParsesArrayRowsAndTranslationAliases() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.updateCredential(ProviderCredentialId.GeminiLegacy, "gemini-key")
        val provider = GeminiLegacyTranslationProvider(
            settingsRepository = settings,
            httpClient = StaticHttpClient(
                TranslationHttpResponse(
                    statusCode = 200,
                    body = geminiCandidateText(
                        """[{"segment_index":7,"he":"שלום","he_niqqud":"שָׁלוֹם","translit":"shalom","translation":"Привет"}]""",
                    ),
                ),
            ),
        )

        val response = provider.translate(
            TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GeminiLegacy),
        ).getOrThrow()

        assertEquals(7, response.rows.single().segmentIndex)
        assertEquals("Привет", response.rows.single().russian)
    }

    @Test
    fun geminiLegacyMalformedCandidateJsonReturnsInvalidResponse() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.updateCredential(ProviderCredentialId.GeminiLegacy, "gemini-key")
        val provider = GeminiLegacyTranslationProvider(
            settingsRepository = settings,
            httpClient = StaticHttpClient(
                TranslationHttpResponse(
                    statusCode = 200,
                    body = geminiCandidateText("""{"rows": ["""),
                ),
            ),
        )

        val error = assertSuspendProviderException {
            provider.translate(
                TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GeminiLegacy),
            ).getOrThrow()
        }

        assertEquals(ProviderErrorCategory.InvalidResponse, error.category)
        assertTrue(error.userMessage.contains("Gemini table rows returned an invalid response envelope"))
    }

    @Test
    fun geminiLegacyMapsBadRequestByErrorBody() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.updateCredential(ProviderCredentialId.GeminiLegacy, "gemini-key")
        val provider = GeminiLegacyTranslationProvider(
            settingsRepository = settings,
            httpClient = StaticHttpClient(
                TranslationHttpResponse(
                    statusCode = 400,
                    body = """{"error":{"message":"Invalid JSON payload received. Unknown name response_mime_type."}}""",
                ),
            ),
        )

        val error = assertSuspendProviderException {
            provider.translate(
                TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GeminiLegacy),
            ).getOrThrow()
        }

        assertEquals(ProviderErrorCategory.InvalidResponse, error.category)
        assertTrue(error.userMessage.contains("Invalid JSON payload"))
    }

    @Test
    fun geminiLegacyStillMapsApiKeyBadRequestToInvalidApiKey() = runTest {
        val settings = ProviderSettingsRepository(InMemorySecureKeyValueStore())
        settings.updateCredential(ProviderCredentialId.GeminiLegacy, "gemini-key")
        val provider = GeminiLegacyTranslationProvider(
            settingsRepository = settings,
            httpClient = StaticHttpClient(
                TranslationHttpResponse(
                    statusCode = 400,
                    body = """{"error":{"message":"API key not valid. Please pass a valid API key."}}""",
                ),
            ),
        )

        val error = assertSuspendProviderException {
            provider.translate(
                TranslationRequest(sourceText = "שלום", providerId = TranslationProviderId.GeminiLegacy),
            ).getOrThrow()
        }

        assertEquals(ProviderErrorCategory.InvalidApiKey, error.category)
        assertTrue(error.userMessage.contains("API key not valid"))
    }

    private fun assertProviderException(block: () -> Unit): ProviderException {
        try {
            block()
        } catch (error: ProviderException) {
            return error
        }
        throw AssertionError("Expected ProviderException")
    }

    private suspend fun assertSuspendProviderException(block: suspend () -> Unit): ProviderException {
        try {
            block()
        } catch (error: ProviderException) {
            return error
        }
        throw AssertionError("Expected ProviderException")
    }
}

private class StaticHttpClient(
    private val response: TranslationHttpResponse,
) : TranslationHttpClient {
    lateinit var lastUri: URI
    var lastHeaders: Map<String, String> = emptyMap()
    var lastBody: String = ""

    override suspend fun get(uri: URI, headers: Map<String, String>): TranslationHttpResponse {
        lastUri = uri
        lastHeaders = headers
        return response
    }

    override suspend fun postJson(
        uri: URI,
        body: String,
        headers: Map<String, String>,
    ): TranslationHttpResponse {
        lastUri = uri
        lastHeaders = headers
        lastBody = body
        return response
    }
}

private class FailingHttpClient(
    private val error: Throwable,
) : TranslationHttpClient {
    override suspend fun get(uri: URI, headers: Map<String, String>): TranslationHttpResponse {
        throw error
    }

    override suspend fun postJson(
        uri: URI,
        body: String,
        headers: Map<String, String>,
    ): TranslationHttpResponse {
        throw error
    }
}

private class FakeGoogleAccessTokenProvider(
    private val token: String,
) : GoogleAccessTokenProvider {
    override suspend fun accessToken(
        serviceAccount: ProviderCredentialMaterial.GoogleServiceAccount,
        scope: String,
        providerId: String,
    ): String = token
}

private fun serviceAccountJson(): String =
    """
    {
      "type": "service_account",
      "project_id": "proj-123",
      "private_key": "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----\n",
      "client_email": "svc@example.iam.gserviceaccount.com"
    }
    """.trimIndent()

private fun geminiTableResponse(): String =
    geminiCandidateText(
        "{\"segments\":[{\"index\":1,\"he\":\"שלום\"}],\"rows\":[{\"segment_index\":1,\"he\":\"שלום\",\"he_niqqud\":\"שָׁלוֹם\",\"translit\":\"shalom\",\"ru\":\"Здравствуйте\"}]}",
    )

private fun geminiCandidateText(text: String): String =
    """
    {
      "candidates": [
        {
          "content": {
            "parts": [
              {
                "text": ${Json.encodeToString(text)}
              }
            ]
          }
        }
      ]
    }
    """.trimIndent()
