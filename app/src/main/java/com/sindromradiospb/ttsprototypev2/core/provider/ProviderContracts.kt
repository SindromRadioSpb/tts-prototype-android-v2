package com.sindromradiospb.ttsprototypev2.core.provider

import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TtsProfile
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId

data class TranslationRequest(
    val sourceText: String,
    val sourceLanguage: String = "he",
    val targetLanguage: String = "ru",
    val providerId: TranslationProviderId,
)

data class TranslationResponse(
    val rows: List<GeneratedRow>,
    val provenance: ProviderProvenance,
    val fromCache: Boolean = false,
)

data class GeneratedRow(
    val segmentIndex: Int,
    val hebrewPlain: String,
    val hebrewNiqqud: String,
    val translit: String,
    val translitRu: String,
    val russian: String,
)

data class TtsRequest(
    val text: String,
    val profile: TtsProfile,
    val libraryTextId: String? = null,
    val libraryRowId: String? = null,
)

data class TtsResponse(
    val audioAssetKey: String?,
    val localFileName: String?,
    val mimeType: String,
    val provenance: ProviderProvenance,
    val fromCache: Boolean = false,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
)

data class ProviderProvenance(
    val requestedProviderId: String,
    val actualProviderId: String,
    val model: String? = null,
    val generatedAt: String,
    val fallbackReason: ProviderErrorCategory? = null,
)

enum class ProviderErrorCategory {
    MissingConfiguration,
    NetworkUnavailable,
    Timeout,
    Unauthorized,
    QuotaExceeded,
    BillingRequired,
    InvalidApiKey,
    ProviderUnavailable,
    InvalidResponse,
    UnsupportedLanguage,
    Unknown,
}

interface TranslationProvider {
    val id: TranslationProviderId
    suspend fun translate(request: TranslationRequest): Result<TranslationResponse>
}

class ProviderException(
    val category: ProviderErrorCategory,
    val providerId: String,
    val userMessage: String,
    cause: Throwable? = null,
) : RuntimeException(userMessage, cause)

interface TtsProvider {
    val id: TtsProviderId
    suspend fun synthesize(request: TtsRequest): Result<TtsResponse>
}

object AndroidV2ProviderPolicy {
    val allowedTranslationProviders: Set<TranslationProviderId> = setOf(
        TranslationProviderId.GoogleTranslateFree,
        TranslationProviderId.GcpTranslate,
        TranslationProviderId.GeminiLegacy,
    )

    val allowedTtsProviders: Set<TtsProviderId> = setOf(
        TtsProviderId.GoogleOnlineTts,
        TtsProviderId.SystemFallbackLowQuality,
    )

    fun requireAllowed(providerId: TranslationProviderId) {
        require(providerId in allowedTranslationProviders) {
            "Translation provider is not allowed in Android v2: $providerId"
        }
    }

    fun requireAllowed(providerId: TtsProviderId) {
        require(providerId in allowedTtsProviders) {
            "TTS provider is not allowed in Android v2: $providerId"
        }
    }
}
