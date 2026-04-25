package com.sindromradiospb.ttsprototypev2.data.provider.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderProvenance
import com.sindromradiospb.ttsprototypev2.core.provider.TtsProvider
import com.sindromradiospb.ttsprototypev2.core.provider.TtsProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TtsRequest
import com.sindromradiospb.ttsprototypev2.core.provider.TtsResponse
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderSettingsRepository
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun createAndroidTtsProviderRegistry(
    context: Context,
    providerSettingsRepository: ProviderSettingsRepository? = null,
    clock: () -> String = { Instant.now().toString() },
): TtsProviderRegistry =
    TtsProviderRegistry(
        providers = listOf(
            if (providerSettingsRepository == null) {
                MissingConfigurationTtsProvider(
                    id = TtsProviderId.GoogleOnlineTts,
                    message = "google_online_tts requires secure credential storage before runtime use.",
                    clock = clock,
                )
            } else {
                GoogleOnlineTtsProvider(
                    settingsRepository = providerSettingsRepository,
                    outputDirectory = File(context.cacheDir, "tts/google-online"),
                    clock = clock,
                )
            },
            AndroidPlatformTtsProvider(
                context = context,
                clock = clock,
            ),
        ),
    )

class FakeTtsProvider(
    override val id: TtsProviderId = TtsProviderId.GoogleOnlineTts,
    private val clock: () -> String = { "2026-04-25T00:00:00Z" },
) : TtsProvider {
    override suspend fun synthesize(request: TtsRequest): Result<TtsResponse> =
        runCatching {
            require(request.profile.providerId == id) {
                "Request profile ${request.profile.providerId.wireId} does not match fake provider ${id.wireId}"
            }
            val assetKey = deterministicAudioAssetKey(request)
            TtsResponse(
                audioAssetKey = assetKey,
                localFileName = "$assetKey.fake",
                mimeType = "audio/x-fake",
                provenance = ProviderProvenance(
                    requestedProviderId = request.profile.providerId.wireId,
                    actualProviderId = id.wireId,
                    model = "m4_fake_tts_provider",
                    generatedAt = clock(),
                ),
                sizeBytes = request.text.toByteArray(Charsets.UTF_8).size.toLong(),
            )
        }
}

class MissingConfigurationTtsProvider(
    override val id: TtsProviderId,
    private val message: String,
    private val clock: () -> String = { Instant.now().toString() },
) : TtsProvider {
    override suspend fun synthesize(request: TtsRequest): Result<TtsResponse> =
        Result.failure(
            ProviderException(
                category = ProviderErrorCategory.MissingConfiguration,
                providerId = id.wireId,
                userMessage = message,
            ),
        )

    @Suppress("unused")
    fun provenanceForDiagnostics(request: TtsRequest): ProviderProvenance =
        ProviderProvenance(
            requestedProviderId = request.profile.providerId.wireId,
            actualProviderId = id.wireId,
            model = "missing_secure_configuration",
            generatedAt = clock(),
            fallbackReason = ProviderErrorCategory.MissingConfiguration,
        )
}

class GoogleOnlineTtsProvider(
    private val settingsRepository: ProviderSettingsRepository,
    private val httpClient: TtsHttpClient = UrlConnectionTtsHttpClient(),
    private val outputDirectory: File,
    private val clock: () -> String = { Instant.now().toString() },
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TtsProvider {
    override val id: TtsProviderId = TtsProviderId.GoogleOnlineTts

    override suspend fun synthesize(request: TtsRequest): Result<TtsResponse> =
        runCatching {
            require(request.profile.providerId == id) {
                "Request profile ${request.profile.providerId.wireId} does not match ${id.wireId}"
            }
            val credential = settingsRepository.getCredentialForProvider(ProviderCredentialId.GoogleOnlineTts)
                ?: throw ProviderException(
                    category = ProviderErrorCategory.MissingConfiguration,
                    providerId = id.wireId,
                    userMessage = "${id.wireId} is not configured in Settings.",
                )
            val uri = URI("https://texttospeech.googleapis.com/v1/text:synthesize?key=${urlEncode(credential)}")
            val body = json.encodeToString(request.toGoogleTtsBody())
            val response = withTimeout(45_000) {
                httpClient.postJson(uri, body)
            }
            if (response.statusCode !in 200..299) {
                throw httpFailure(response.statusCode)
            }
            val audioBytes = parseAudioContent(response.body)
            outputDirectory.mkdirs()
            val assetKey = deterministicAudioAssetKey(request)
            val output = File(outputDirectory, "$assetKey.mp3")
            withContext(Dispatchers.IO) {
                output.writeBytes(audioBytes)
            }
            TtsResponse(
                audioAssetKey = assetKey,
                localFileName = output.name,
                localFilePath = output.absolutePath,
                mimeType = "audio/mpeg",
                provenance = ProviderProvenance(
                    requestedProviderId = request.profile.providerId.wireId,
                    actualProviderId = id.wireId,
                    model = "google_cloud_text_to_speech_v1",
                    generatedAt = clock(),
                ),
                sizeBytes = output.length(),
            )
        }.mapFailureToTtsProviderException(id.wireId)

    internal fun parseAudioContent(body: String): ByteArray {
        val encoded = json.parseToJsonElement(body)
            .jsonObject["audioContent"]
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()
        if (encoded.isBlank()) {
            throw ProviderException(
                category = ProviderErrorCategory.InvalidResponse,
                providerId = id.wireId,
                userMessage = "Google Online TTS returned no audio content.",
            )
        }
        return Base64.getDecoder().decode(encoded)
    }

    private fun TtsRequest.toGoogleTtsBody() =
        buildJsonObject {
            put("input", buildJsonObject { put("text", text) })
            put(
                "voice",
                buildJsonObject {
                    put("languageCode", profile.language)
                    profile.voiceName?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                },
            )
            put(
                "audioConfig",
                buildJsonObject {
                    put("audioEncoding", "MP3")
                    put("speakingRate", profile.speakingRate)
                    put("pitch", profile.pitch)
                },
            )
        }

    private fun httpFailure(statusCode: Int): ProviderException {
        val category = when (statusCode) {
            400 -> ProviderErrorCategory.InvalidApiKey
            401 -> ProviderErrorCategory.Unauthorized
            403 -> ProviderErrorCategory.BillingRequired
            408 -> ProviderErrorCategory.Timeout
            429 -> ProviderErrorCategory.QuotaExceeded
            in 500..599 -> ProviderErrorCategory.ProviderUnavailable
            else -> ProviderErrorCategory.InvalidResponse
        }
        return ProviderException(
            category = category,
            providerId = id.wireId,
            userMessage = "Google Online TTS failed with HTTP $statusCode.",
        )
    }
}

class AndroidPlatformTtsProvider(
    context: Context,
    private val outputDirectory: File = File(context.cacheDir, "tts/system"),
    private val clock: () -> String = { Instant.now().toString() },
) : TtsProvider {
    override val id: TtsProviderId = TtsProviderId.SystemFallbackLowQuality

    private val initStatus = CompletableDeferred<Int>()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        initStatus.complete(status)
    }

    override suspend fun synthesize(request: TtsRequest): Result<TtsResponse> =
        runCatching {
            require(request.profile.providerId == id) {
                "Request profile ${request.profile.providerId.wireId} does not match ${id.wireId}"
            }
            withTimeout(45_000) {
                awaitReady(request)
                outputDirectory.mkdirs()
                val assetKey = deterministicAudioAssetKey(request)
                val output = File(outputDirectory, "$assetKey.wav")
                synthesizeToFile(request, output)
                if (!output.exists() || output.length() <= 0L) {
                    throw ProviderException(
                        category = ProviderErrorCategory.InvalidResponse,
                        providerId = id.wireId,
                        userMessage = "Android system TTS produced an empty audio file.",
                    )
                }
                TtsResponse(
                    audioAssetKey = assetKey,
                    localFileName = output.name,
                    localFilePath = output.absolutePath,
                    mimeType = "audio/wav",
                    provenance = ProviderProvenance(
                        requestedProviderId = request.profile.providerId.wireId,
                        actualProviderId = id.wireId,
                        model = "android_text_to_speech",
                        generatedAt = clock(),
                    ),
                    sizeBytes = output.length(),
                )
            }
        }.mapFailureToTtsProviderException(id.wireId)

    fun shutdown() {
        tts.shutdown()
    }

    private suspend fun awaitReady(request: TtsRequest) {
        val status = initStatus.await()
        if (status != TextToSpeech.SUCCESS) {
            throw ProviderException(
                category = ProviderErrorCategory.ProviderUnavailable,
                providerId = id.wireId,
                userMessage = "Android system TextToSpeech is unavailable.",
            )
        }
        tts.setSpeechRate(request.profile.speakingRate.toFloat())
        tts.setPitch((1.0 + request.profile.pitch).toFloat().coerceAtLeast(0.1f))
        val languageResult = tts.setLanguage(request.profile.language.toLocale())
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            throw ProviderException(
                category = ProviderErrorCategory.UnsupportedLanguage,
                providerId = id.wireId,
                userMessage = "Android system TextToSpeech does not support language ${request.profile.language}.",
            )
        }
    }

    private suspend fun synthesizeToFile(request: TtsRequest, output: File) {
        val utteranceId = "tts-${UUID.randomUUID()}"
        suspendCancellableCoroutine { continuation ->
            tts.setOnUtteranceProgressListener(
                @Suppress("OVERRIDE_DEPRECATION")
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(doneUtteranceId: String?) {
                        if (doneUtteranceId == utteranceId && continuation.isActive) {
                            continuation.resume(Unit) { _, _, _ -> }
                        }
                    }

                    @Deprecated("Deprecated by Android framework but still called on old engines")
                    override fun onError(errorUtteranceId: String?) {
                        onError(errorUtteranceId, TextToSpeech.ERROR)
                    }

                    override fun onError(errorUtteranceId: String?, errorCode: Int) {
                        if (errorUtteranceId == utteranceId && continuation.isActive) {
                            continuation.resumeWith(
                                Result.failure(
                                    ProviderException(
                                        category = ProviderErrorCategory.ProviderUnavailable,
                                        providerId = id.wireId,
                                        userMessage = "Android system TextToSpeech failed with code $errorCode.",
                                    ),
                                ),
                            )
                        }
                    }
                },
            )
            val result = tts.synthesizeToFile(request.text, Bundle.EMPTY, output, utteranceId)
            if (result != TextToSpeech.SUCCESS && continuation.isActive) {
                continuation.resumeWith(
                    Result.failure(
                        ProviderException(
                            category = ProviderErrorCategory.ProviderUnavailable,
                            providerId = id.wireId,
                            userMessage = "Android system TextToSpeech rejected synthesis request.",
                        ),
                    ),
                )
            }
            continuation.invokeOnCancellation {
                tts.stop()
            }
        }
    }
}

fun deterministicAudioAssetKey(request: TtsRequest): String {
    val profileJson = Json.encodeToString(request.profile)
    return sha256(
        listOf(
            request.profile.providerId.wireId,
            request.profile.voiceName.orEmpty(),
            request.profile.language,
            request.text.trim(),
            profileJson,
        ).joinToString("|"),
    )
}

private fun String.toLocale(): Locale {
    val normalized = replace('_', '-')
    return Locale.forLanguageTag(normalized)
}

private fun <T> Result<T>.mapFailureToTtsProviderException(providerId: String): Result<T> =
    recoverCatching { error ->
        throw when (error) {
            is ProviderException -> error
            is IOException -> ProviderException(
                category = ProviderErrorCategory.NetworkUnavailable,
                providerId = providerId,
                userMessage = "Network is unavailable for TTS.",
                cause = error,
            )
            is TimeoutCancellationException -> ProviderException(
                category = ProviderErrorCategory.Timeout,
                providerId = providerId,
                userMessage = "TTS provider timed out.",
                cause = error,
            )
            else -> ProviderException(
                category = ProviderErrorCategory.Unknown,
                providerId = providerId,
                userMessage = "TTS provider failed.",
                cause = error,
            )
        }
    }

data class TtsHttpResponse(
    val statusCode: Int,
    val body: String,
)

interface TtsHttpClient {
    suspend fun postJson(uri: URI, body: String): TtsHttpResponse
}

class UrlConnectionTtsHttpClient : TtsHttpClient {
    override suspend fun postJson(uri: URI, body: String): TtsHttpResponse =
        withContext(Dispatchers.IO) {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 45_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                TtsHttpResponse(
                    statusCode = status,
                    body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty(),
                )
            } finally {
                connection.disconnect()
            }
        }
}

private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name())
