package com.sindromradiospb.ttsprototypev2.data.provider.translation

import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.GeneratedRow
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderProvenance
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationProvider
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationRequest
import com.sindromradiospb.ttsprototypev2.core.provider.TranslationResponse
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLEncoder
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

fun createAndroidTranslationProviderRegistry(
    clock: () -> String = { Instant.now().toString() },
): TranslationProviderRegistry =
    TranslationProviderRegistry(
        providers = listOf(
            GoogleTranslateFreeProvider(clock = clock),
            MissingConfigurationTranslationProvider(
                id = TranslationProviderId.GcpTranslate,
                message = "gcp_translate requires secure key storage from M9 before runtime use.",
                clock = clock,
            ),
            MissingConfigurationTranslationProvider(
                id = TranslationProviderId.GeminiLegacy,
                message = "gemini_legacy requires secure key storage and cost warning UI from M9 before runtime use.",
                clock = clock,
            ),
        ),
    )

class FakeTranslationProvider(
    override val id: TranslationProviderId = TranslationProviderId.GoogleTranslateFree,
    private val clock: () -> String = { "2026-04-25T00:00:00Z" },
) : TranslationProvider {
    override suspend fun translate(request: TranslationRequest): Result<TranslationResponse> =
        runCatching {
            require(request.providerId == id) {
                "Request provider ${request.providerId.wireId} does not match fake provider ${id.wireId}"
            }
            TranslationResponse(
                rows = splitSource(request.sourceText).mapIndexed { index, segment ->
                    GeneratedRow(
                        segmentIndex = index,
                        hebrewPlain = segment,
                        hebrewNiqqud = "",
                        translit = "m3-fake-sbl-${index + 1}",
                        translitRu = "м3-фейк-${index + 1}",
                        russian = "[M3 fake translation ${index + 1}] ${segment.take(48)}",
                    )
                },
                provenance = ProviderProvenance(
                    requestedProviderId = request.providerId.wireId,
                    actualProviderId = id.wireId,
                    model = "m3_fake_translation_provider",
                    generatedAt = clock(),
                ),
            )
        }
}

class MissingConfigurationTranslationProvider(
    override val id: TranslationProviderId,
    private val message: String,
    private val clock: () -> String = { Instant.now().toString() },
) : TranslationProvider {
    override suspend fun translate(request: TranslationRequest): Result<TranslationResponse> =
        Result.failure(
            ProviderException(
                category = ProviderErrorCategory.MissingConfiguration,
                providerId = id.wireId,
                userMessage = message,
            ),
        )

    @Suppress("unused")
    fun provenanceForDiagnostics(request: TranslationRequest): ProviderProvenance =
        ProviderProvenance(
            requestedProviderId = request.providerId.wireId,
            actualProviderId = id.wireId,
            model = "missing_secure_configuration",
            generatedAt = clock(),
            fallbackReason = ProviderErrorCategory.MissingConfiguration,
        )
}

class GoogleTranslateFreeProvider(
    private val httpClient: TranslationHttpClient = UrlConnectionTranslationHttpClient(),
    private val clock: () -> String = { Instant.now().toString() },
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TranslationProvider {
    override val id: TranslationProviderId = TranslationProviderId.GoogleTranslateFree

    override suspend fun translate(request: TranslationRequest): Result<TranslationResponse> =
        runCatching {
            require(request.providerId == id) {
                "Request provider ${request.providerId.wireId} does not match ${id.wireId}"
            }
            val rows = splitSource(request.sourceText).mapIndexed { index, segment ->
                val translated = translateSegment(segment, request.sourceLanguage, request.targetLanguage)
                GeneratedRow(
                    segmentIndex = index,
                    hebrewPlain = segment,
                    hebrewNiqqud = "",
                    translit = "",
                    translitRu = "",
                    russian = translated,
                )
            }
            TranslationResponse(
                rows = rows,
                provenance = ProviderProvenance(
                    requestedProviderId = request.providerId.wireId,
                    actualProviderId = id.wireId,
                    model = "google_translate_free_http",
                    generatedAt = clock(),
                ),
            )
        }.mapFailureToProviderException(id.wireId)

    private suspend fun translateSegment(segment: String, sourceLanguage: String, targetLanguage: String): String {
        val uri = buildUri(segment, sourceLanguage, targetLanguage)
        val response = withTimeout(20_000) {
            httpClient.get(uri)
        }
        if (response.statusCode !in 200..299) {
            throw httpFailure(response.statusCode)
        }
        return parseGoogleFreeTranslation(response.body)
    }

    private fun buildUri(segment: String, sourceLanguage: String, targetLanguage: String): URI {
        val encoded = URLEncoder.encode(segment, Charsets.UTF_8.name())
        return URI(
            "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=$sourceLanguage&tl=$targetLanguage&dt=t&q=$encoded",
        )
    }

    internal fun parseGoogleFreeTranslation(body: String): String {
        val root = json.parseToJsonElement(body).jsonArray
        val translationRows = root[0] as? JsonArray
            ?: throw ProviderException(
                category = ProviderErrorCategory.InvalidResponse,
                providerId = id.wireId,
                userMessage = "Google Free returned an invalid translation envelope.",
            )
        val translated = translationRows.joinToString(separator = "") { row ->
            row.jsonArray.getOrNull(0)?.jsonPrimitive?.contentOrNull.orEmpty()
        }
        if (translated.isBlank()) {
            throw ProviderException(
                category = ProviderErrorCategory.InvalidResponse,
                providerId = id.wireId,
                userMessage = "Google Free returned an empty translation.",
            )
        }
        return translated
    }

    private fun httpFailure(statusCode: Int): ProviderException {
        val category = when (statusCode) {
            401 -> ProviderErrorCategory.Unauthorized
            403 -> ProviderErrorCategory.Unauthorized
            408 -> ProviderErrorCategory.Timeout
            429 -> ProviderErrorCategory.QuotaExceeded
            in 500..599 -> ProviderErrorCategory.ProviderUnavailable
            else -> ProviderErrorCategory.InvalidResponse
        }
        return ProviderException(
            category = category,
            providerId = id.wireId,
            userMessage = "Google Free translation failed with HTTP $statusCode.",
        )
    }
}

data class TranslationHttpResponse(
    val statusCode: Int,
    val body: String,
)

interface TranslationHttpClient {
    suspend fun get(uri: URI): TranslationHttpResponse
}

class UrlConnectionTranslationHttpClient : TranslationHttpClient {
    override suspend fun get(uri: URI): TranslationHttpResponse =
        withContext(Dispatchers.IO) {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            connection.requestMethod = "GET"
            try {
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                TranslationHttpResponse(
                    statusCode = status,
                    body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty(),
                )
            } finally {
                connection.disconnect()
            }
        }
}

internal fun splitSource(sourceText: String): List<String> =
    sourceText
        .split('\n', '.', '!', '?', '׃', '׀')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf(sourceText.trim()) }

private fun <T> Result<T>.mapFailureToProviderException(providerId: String): Result<T> =
    recoverCatching { error ->
        throw when (error) {
            is ProviderException -> error
            is TimeoutCancellationException -> ProviderException(
                category = ProviderErrorCategory.Timeout,
                providerId = providerId,
                userMessage = "Translation provider timed out.",
                cause = error,
            )
            is SocketTimeoutException -> ProviderException(
                category = ProviderErrorCategory.Timeout,
                providerId = providerId,
                userMessage = "Translation provider timed out.",
                cause = error,
            )
            is IOException -> ProviderException(
                category = ProviderErrorCategory.NetworkUnavailable,
                providerId = providerId,
                userMessage = "Network is unavailable for translation.",
                cause = error,
            )
            else -> ProviderException(
                category = ProviderErrorCategory.Unknown,
                providerId = providerId,
                userMessage = "Translation provider failed.",
                cause = error,
            )
        }
    }
