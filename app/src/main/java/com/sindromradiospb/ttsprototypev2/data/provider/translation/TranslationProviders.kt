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
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.data.provider.google.GoogleAccessTokenProvider
import com.sindromradiospb.ttsprototypev2.data.provider.google.JwtGoogleAccessTokenProvider
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderCredentialMaterial
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderCredentialParser
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderSettingsRepository
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.add
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun createAndroidTranslationProviderRegistry(
    providerSettingsRepository: ProviderSettingsRepository? = null,
    clock: () -> String = { Instant.now().toString() },
): TranslationProviderRegistry =
    TranslationProviderRegistry(
        providers = listOf(
            GoogleTranslateFreeProvider(clock = clock),
            if (providerSettingsRepository == null) {
                MissingConfigurationTranslationProvider(
                    id = TranslationProviderId.GcpTranslate,
                    message = "gcp_translate requires secure key storage before runtime use.",
                    clock = clock,
                )
            } else {
                GcpTranslateProvider(settingsRepository = providerSettingsRepository, clock = clock)
            },
            if (providerSettingsRepository == null) {
                MissingConfigurationTranslationProvider(
                    id = TranslationProviderId.GeminiLegacy,
                    message = "gemini_legacy requires secure key storage and cost warning UI before runtime use.",
                    clock = clock,
                )
            } else {
                GeminiLegacyTranslationProvider(settingsRepository = providerSettingsRepository, clock = clock)
            },
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
            val segments = splitSource(request.sourceText)
            val translations = translateSegments(segments, request.targetLanguage)
            val rows = segments.mapIndexed { index, segment ->
                GeneratedRow(
                    segmentIndex = index,
                    hebrewPlain = segment,
                    hebrewNiqqud = "",
                    translit = "",
                    translitRu = "",
                    russian = translations.getOrElse(index) { "" },
                )
            }
            TranslationResponse(
                rows = rows,
                provenance = ProviderProvenance(
                    requestedProviderId = request.providerId.wireId,
                    actualProviderId = id.wireId,
                    model = "google-free-gtx-v1",
                    generatedAt = clock(),
                ),
            )
        }.mapFailureToProviderException(id.wireId)

    private suspend fun translateSegments(segments: List<String>, targetLanguage: String): List<String> {
        if (segments.isEmpty()) return emptyList()
        val combined = segments.joinToString("\n")
        val batch = runCatching { translateSegment(combined, targetLanguage) }
        val batchText = batch.getOrElse { error ->
            if (error is ProviderException && error.category != ProviderErrorCategory.QuotaExceeded) {
                null
            } else {
                throw error
            }
        }
        if (batchText != null) {
            val lines = batchText.split('\n')
            if (lines.size == segments.size) return lines
        }
        return segments.map { segment ->
            runCatching { translateSegment(segment, targetLanguage) }.getOrElse { error ->
                if (error is ProviderException && error.category != ProviderErrorCategory.QuotaExceeded) {
                    ""
                } else {
                    throw error
                }
            }
        }
    }

    private suspend fun translateSegment(segment: String, targetLanguage: String): String {
        val uri = buildUri(segment, targetLanguage)
        val response = withTimeout(20_000) {
            httpClient.get(uri, GoogleFreeHeaders)
        }
        if (response.statusCode !in 200..299) {
            throw httpFailure(response.statusCode)
        }
        return parseGoogleFreeTranslation(response.body)
    }

    private fun buildUri(segment: String, targetLanguage: String): URI {
        val encoded = URLEncoder.encode(segment, Charsets.UTF_8.name())
        return URI(
            "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=iw&tl=$targetLanguage&dt=t&q=$encoded",
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

    private companion object {
        val GoogleFreeHeaders = mapOf("User-Agent" to "Mozilla/5.0")
    }
}

class GcpTranslateProvider(
    private val settingsRepository: ProviderSettingsRepository,
    private val httpClient: TranslationHttpClient = UrlConnectionTranslationHttpClient(),
    private val accessTokenProvider: GoogleAccessTokenProvider = JwtGoogleAccessTokenProvider(),
    private val clock: () -> String = { Instant.now().toString() },
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TranslationProvider {
    override val id: TranslationProviderId = TranslationProviderId.GcpTranslate

    override suspend fun translate(request: TranslationRequest): Result<TranslationResponse> =
        runCatching {
            require(request.providerId == id) {
                "Request provider ${request.providerId.wireId} does not match ${id.wireId}"
            }
            val credential = requireCredential(ProviderCredentialId.GcpTranslate)
            val rows = splitSource(request.sourceText).mapIndexed { index, segment ->
                val translated = translateSegment(segment, request.sourceLanguage, request.targetLanguage, credential)
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
                    model = "cloud_translation_basic_v2",
                    generatedAt = clock(),
                ),
            )
        }.mapFailureToProviderException(id.wireId)

    private suspend fun translateSegment(
        segment: String,
        sourceLanguage: String,
        targetLanguage: String,
        credential: ProviderCredentialMaterial,
    ): String {
        val uri = when (credential) {
            is ProviderCredentialMaterial.ApiKey ->
                URI("https://translation.googleapis.com/language/translate/v2?key=${urlEncode(credential.value)}")
            is ProviderCredentialMaterial.GoogleServiceAccount ->
                URI("https://translation.googleapis.com/v3/projects/${credential.projectId}/locations/global:translateText")
        }
        val body = json.encodeToString(credential.toTranslateBody(segment, sourceLanguage, targetLanguage))
        val headers = when (credential) {
            is ProviderCredentialMaterial.ApiKey -> emptyMap()
            is ProviderCredentialMaterial.GoogleServiceAccount -> mapOf(
                "Authorization" to "Bearer ${accessTokenProvider.accessToken(credential, GcpTranslateScope, id.wireId)}",
            )
        }
        val response = withTimeout(20_000) {
            httpClient.postJson(uri, body, headers)
        }
        if (response.statusCode !in 200..299) {
            throw httpFailure(id.wireId, response.statusCode, "GCP Translate")
        }
        return parseGcpTranslation(response.body)
    }

    internal fun parseGcpTranslation(body: String): String {
        val root = json.parseToJsonElement(body).jsonObject
        val translations = root["data"]
            ?.jsonObject
            ?.get("translations")
            ?.jsonArray
            ?: root["translations"]?.jsonArray
            ?: throw invalidEnvelope(id.wireId, "GCP Translate")
        val translated = translations.firstOrNull()
            ?.jsonObject
            ?.get("translatedText")
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()
        if (translated.isBlank()) throw invalidEnvelope(id.wireId, "GCP Translate")
        return translated
    }

    private fun ProviderCredentialMaterial.toTranslateBody(
        segment: String,
        sourceLanguage: String,
        targetLanguage: String,
    ) = when (this) {
        is ProviderCredentialMaterial.ApiKey -> buildJsonObject {
            put("q", segment)
            put("source", sourceLanguage)
            put("target", targetLanguage)
            put("format", "text")
        }
        is ProviderCredentialMaterial.GoogleServiceAccount -> buildJsonObject {
            put("contents", buildJsonArray { add(segment) })
            put("mimeType", "text/plain")
            put("sourceLanguageCode", sourceLanguage)
            put("targetLanguageCode", targetLanguage)
        }
    }

    private fun requireCredential(id: ProviderCredentialId): ProviderCredentialMaterial =
        settingsRepository.getCredentialForProvider(id)
            ?.let { ProviderCredentialParser.parseStored(id, it) }
            ?: throw ProviderException(
                category = ProviderErrorCategory.MissingConfiguration,
                providerId = this.id.wireId,
                userMessage = "${this.id.wireId} is not configured in Settings.",
            )

    private companion object {
        const val GcpTranslateScope = "https://www.googleapis.com/auth/cloud-translation"
    }
}

class GeminiLegacyTranslationProvider(
    private val settingsRepository: ProviderSettingsRepository,
    private val httpClient: TranslationHttpClient = UrlConnectionTranslationHttpClient(),
    private val clock: () -> String = { Instant.now().toString() },
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val model: String = "gemini-flash-latest",
) : TranslationProvider {
    override val id: TranslationProviderId = TranslationProviderId.GeminiLegacy

    override suspend fun translate(request: TranslationRequest): Result<TranslationResponse> =
        runCatching {
            require(request.providerId == id) {
                "Request provider ${request.providerId.wireId} does not match ${id.wireId}"
            }
            val apiKey = requireCredential().value
            val rows = translateTable(request.sourceText.trim(), apiKey)
            TranslationResponse(
                rows = rows,
                provenance = ProviderProvenance(
                    requestedProviderId = request.providerId.wireId,
                    actualProviderId = id.wireId,
                    model = model,
                    generatedAt = clock(),
                ),
            )
        }.mapFailureToProviderException(id.wireId)

    private suspend fun translateTable(sourceText: String, apiKey: String): List<GeneratedRow> {
        val uri = URI("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${urlEncode(apiKey)}")
        val prompt = geminiTablePrompt(sourceText)
        val body = json.encodeToString(
            buildJsonObject {
                put(
                    "contents",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put(
                                    "parts",
                                    buildJsonArray {
                                        add(buildJsonObject { put("text", prompt) })
                                    },
                                )
                            },
                        )
                    },
                )
                put(
                    "generationConfig",
                    buildJsonObject {
                        put("temperature", 0.1)
                        put("maxOutputTokens", 4096)
                    },
                )
            },
        )
        val response = withTimeout(30_000) {
            httpClient.postJson(uri, body)
        }
        if (response.statusCode !in 200..299) {
            throw httpFailure(id.wireId, response.statusCode, "Gemini", response.body)
        }
        return parseGeminiTableResponse(response.body)
    }

    private fun geminiTablePrompt(sourceText: String): String {
        val escapedSource = "\"\"\"\n$sourceText\n\"\"\""
        return """
        You are a strict JSON generator.

        Task:
        1) Split the input Hebrew text into logical sentences / segments in the original order.
        2) Translate each segment into Russian.
        3) Produce JSON with:
           - "segments": list of original segments.
           - "rows": table rows for the UI, one row per segment.

        Input text (Hebrew, may contain newlines):

        $escapedSource

        Strict output format (JSON only, no comments, no markdown):
        {
          "segments": [
            { "index": 1, "he": "..." }
          ],
          "rows": [
            {
              "segment_index": 1,
              "he": "...",
              "he_niqqud": "...",
              "translit": "...",
              "ru": "..."
            }
          ]
        }

        Rules:
        - Preserve the original order of sentences.
        - Do NOT merge semantically different sentences into a single row.
        - If the input contains line breaks, you MAY use them as additional hints for segmentation.
        - Always return ALL data inside a single JSON object exactly in the format above.
        """.trimIndent()
    }

    internal fun parseGeminiTableResponse(body: String): List<GeneratedRow> {
        val text = extractGeminiText(body)
        val jsonText = stripJsonFence(text)
        val parsedRows = runCatching {
            rowsFromGeminiPayload(json.parseToJsonElement(jsonText))
        }.getOrElse { error ->
            rowsFromLooseGeminiText(jsonText).ifEmpty {
                throw invalidEnvelope(
                    providerId = id.wireId,
                    label = "Gemini table rows",
                    details = error.message,
                )
            }
        }
        if (parsedRows.isEmpty()) throw invalidEnvelope(id.wireId, "Gemini table rows")
        return parsedRows
    }

    private fun rowsFromGeminiPayload(root: JsonElement): List<GeneratedRow> {
        if (root is JsonArray) return rowsFromJsonElements(root, emptyMap())
        val payload = root.jsonObject
        val rows = payload["rows"]?.jsonArray ?: throw invalidEnvelope(id.wireId, "Gemini table rows")
        val segmentsByIndex = payload["segments"]
            ?.jsonArray
            ?.mapIndexedNotNull { index, element ->
                val segment = runCatching { element.jsonObject }.getOrNull() ?: return@mapIndexedNotNull null
                val segmentIndex = segment["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: (index + 1)
                val he = segment["he"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
                if (he.isBlank()) null else segmentIndex to he
            }
            ?.toMap()
            .orEmpty()
        return rowsFromJsonElements(rows, segmentsByIndex)
    }

    private fun rowsFromJsonElements(
        rows: JsonArray,
        segmentsByIndex: Map<Int, String>,
    ): List<GeneratedRow> =
        rows.mapIndexedNotNull { index, element ->
            val row = runCatching { element.jsonObject }.getOrElse { return@mapIndexedNotNull null }
            val segmentIndex = row["segment_index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?: row["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?.takeIf { it > 0 }
                ?: (index + 1)
            val he = segmentsByIndex[segmentIndex]
                ?: row["he"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            if (he.isBlank()) return@mapIndexedNotNull null
            GeneratedRow(
                segmentIndex = segmentIndex,
                hebrewPlain = he,
                hebrewNiqqud = row["he_niqqud"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
                translit = row["translit"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
                translitRu = row["translit_ru"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
                russian = row["ru"]?.jsonPrimitive?.contentOrNull
                    ?: row["russian"]?.jsonPrimitive?.contentOrNull
                    ?: row["translation"]?.jsonPrimitive?.contentOrNull
                ?: "",
            )
        }

    private fun rowsFromLooseGeminiText(text: String): List<GeneratedRow> =
        Regex("""\{[^{}]*"(?:he|hebrew)"\s*:[^{}]*\}""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .mapIndexedNotNull { index, match ->
                val objectText = match.value
                val he = looseStringField(objectText, "he").ifBlank { looseStringField(objectText, "hebrew") }
                if (he.isBlank()) return@mapIndexedNotNull null
                GeneratedRow(
                    segmentIndex = looseIntField(objectText, "segment_index")
                        ?: looseIntField(objectText, "index")
                        ?: index,
                    hebrewPlain = he,
                    hebrewNiqqud = looseStringField(objectText, "he_niqqud"),
                    translit = looseStringField(objectText, "translit"),
                    translitRu = looseStringField(objectText, "translit_ru"),
                    russian = looseStringField(objectText, "ru")
                        .ifBlank { looseStringField(objectText, "russian") }
                        .ifBlank { looseStringField(objectText, "translation") },
                )
            }
            .toList()

    private fun looseStringField(objectText: String, field: String): String {
        val knownFields = "segment_index|index|he|hebrew|he_niqqud|translit|translit_ru|ru|russian|translation"
        return Regex(
            """"$field"\s*:\s*"([\s\S]*?)(?="\s*,\s*"(?:$knownFields)"\s*:|"\s*\}|$)""",
        ).find(objectText)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.trim()
            .orEmpty()
    }

    private fun looseIntField(objectText: String, field: String): Int? =
        Regex(""""$field"\s*:\s*(\d+)""")
            .find(objectText)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

    private fun extractGeminiText(body: String): String {
        val text = runCatching {
            json.parseToJsonElement(body)
                .jsonObject["candidates"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("parts")
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.contentOrNull
                .orEmpty()
                .trim()
        }.getOrElse { error ->
            throw invalidEnvelope(
                providerId = id.wireId,
                label = "Gemini HTTP response",
                details = error.message,
            )
        }
        if (text.isBlank()) throw invalidEnvelope(id.wireId, "Gemini table response")
        return text
    }

    private fun stripJsonFence(text: String): String =
        text
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

    private fun requireCredential(): ProviderCredentialMaterial.ApiKey =
        settingsRepository.getCredentialForProvider(ProviderCredentialId.GeminiLegacy)
            ?.let { ProviderCredentialParser.parseStored(ProviderCredentialId.GeminiLegacy, it) as? ProviderCredentialMaterial.ApiKey }
            ?: throw ProviderException(
                category = ProviderErrorCategory.MissingConfiguration,
                providerId = id.wireId,
                userMessage = "${id.wireId} is not configured in Settings.",
            )
}

data class TranslationHttpResponse(
    val statusCode: Int,
    val body: String,
)

interface TranslationHttpClient {
    suspend fun get(uri: URI, headers: Map<String, String> = emptyMap()): TranslationHttpResponse
    suspend fun postJson(
        uri: URI,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): TranslationHttpResponse
}

class UrlConnectionTranslationHttpClient : TranslationHttpClient {
    override suspend fun get(uri: URI, headers: Map<String, String>): TranslationHttpResponse =
        request(uri = uri, method = "GET", body = null, headers = headers)

    override suspend fun postJson(
        uri: URI,
        body: String,
        headers: Map<String, String>,
    ): TranslationHttpResponse =
        request(uri = uri, method = "POST", body = body, headers = headers)

    private suspend fun request(
        uri: URI,
        method: String,
        body: String?,
        headers: Map<String, String> = emptyMap(),
    ): TranslationHttpResponse =
        withContext(Dispatchers.IO) {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            connection.requestMethod = method
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
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
            is SerializationException -> ProviderException(
                category = ProviderErrorCategory.InvalidResponse,
                providerId = providerId,
                userMessage = "Translation provider returned invalid JSON: ${error.message.orEmpty().take(220)}",
                cause = error,
            )
            is IllegalArgumentException -> ProviderException(
                category = ProviderErrorCategory.InvalidResponse,
                providerId = providerId,
                userMessage = "Translation provider returned invalid data: ${error.message.orEmpty().take(220)}",
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

private fun httpFailure(
    providerId: String,
    statusCode: Int,
    label: String,
    body: String = "",
): ProviderException {
    val googleMessage = extractGoogleErrorMessage(body)
    val category = when (statusCode) {
        400 -> when {
            googleMessage.contains("api key", ignoreCase = true) ||
                googleMessage.contains("API_KEY_INVALID", ignoreCase = true) -> ProviderErrorCategory.InvalidApiKey
            else -> ProviderErrorCategory.InvalidResponse
        }
        401 -> ProviderErrorCategory.Unauthorized
        403 -> ProviderErrorCategory.BillingRequired
        408 -> ProviderErrorCategory.Timeout
        429 -> ProviderErrorCategory.QuotaExceeded
        in 500..599 -> ProviderErrorCategory.ProviderUnavailable
        else -> ProviderErrorCategory.InvalidResponse
    }
    val details = googleMessage.takeIf { it.isNotBlank() }?.let { ": ${it.take(220)}" }.orEmpty()
    return ProviderException(
        category = category,
        providerId = providerId,
        userMessage = "$label failed with HTTP $statusCode$details.",
    )
}

private fun extractGoogleErrorMessage(body: String): String =
    runCatching {
        Json.parseToJsonElement(body)
            .jsonObject["error"]
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
    }.getOrDefault("")

private fun invalidEnvelope(
    providerId: String,
    label: String,
    details: String? = null,
): ProviderException {
    val suffix = details
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { ": ${it.take(220)}" }
        .orEmpty()
    return ProviderException(
        category = ProviderErrorCategory.InvalidResponse,
        providerId = providerId,
        userMessage = "$label returned an invalid response envelope$suffix.",
    )
}

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name())
