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
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun createAndroidTtsProviderRegistry(
    context: Context,
    clock: () -> String = { Instant.now().toString() },
): TtsProviderRegistry =
    TtsProviderRegistry(
        providers = listOf(
            MissingConfigurationTtsProvider(
                id = TtsProviderId.GoogleOnlineTts,
                message = "google_online_tts requires secure credential storage from M9 before runtime use.",
                clock = clock,
            ),
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

private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
