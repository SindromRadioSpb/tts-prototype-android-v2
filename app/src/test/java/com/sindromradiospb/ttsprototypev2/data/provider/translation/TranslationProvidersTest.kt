package com.sindromradiospb.ttsprototypev2.data.provider.translation

import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationRequest
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.test.runTest
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

    private fun assertProviderException(block: () -> Unit): ProviderException {
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
    override suspend fun get(uri: URI): TranslationHttpResponse = response
}

private class FailingHttpClient(
    private val error: Throwable,
) : TranslationHttpClient {
    override suspend fun get(uri: URI): TranslationHttpResponse {
        throw error
    }
}
