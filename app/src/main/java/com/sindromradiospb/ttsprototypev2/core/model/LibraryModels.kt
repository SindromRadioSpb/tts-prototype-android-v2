package com.sindromradiospb.ttsprototypev2.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryText(
    val id: String,
    val textKey: String,
    val title: String,
    val level: String? = null,
    val tags: List<String> = emptyList(),
    val sourceLabel: String? = null,
    val topic: String? = null,
    val sourceText: String,
    val sourceMeta: SourceMeta? = null,
    val ttsProfile: TtsProfile? = null,
    val tableModelMeta: TableModelMeta? = null,
    val rows: List<LibraryRow>,
    val audioAssetKey: String? = null,
    val audioTtsProfile: TtsProfile? = null,
    val isArchived: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val lastOpenedAt: String? = null,
    val schemaVersion: Int = 1,
)

@Serializable
data class LibraryRow(
    val id: String,
    val textId: String,
    val orderIndex: Int,
    val hebrewPlain: String,
    val hebrewNiqqud: String = "",
    val translit: String = "",
    val translitRu: String = "",
    val russian: String = "",
    val rowHash: String? = null,
    val editMeta: EditMeta? = null,
    val audioAssetKey: String? = null,
    val audioTtsProfile: TtsProfile? = null,
)

@Serializable
data class EditMeta(
    val edited: Map<String, Boolean> = emptyMap(),
    val original: Map<String, String?> = emptyMap(),
    val added: Boolean = false,
)

@Serializable
data class SourceMeta(
    val origin: String,
    val importedAt: String? = null,
)

@Serializable
data class TtsProfile(
    val providerId: TtsProviderId,
    val language: String,
    val voiceName: String? = null,
    val speakingRate: Double = 1.0,
    val pitch: Double = 0.0,
)

@Serializable
data class TableModelMeta(
    val provider: TranslationProviderId,
    val actualProvider: TranslationProviderId? = null,
    val model: String? = null,
    val cacheKey: String? = null,
    val fromCache: Boolean = false,
    val niqqudProvider: String? = null,
    val niqqudDegraded: Boolean = false,
    val generatedAt: String,
)

@Serializable
enum class TranslationProviderId(val wireId: String) {
    GoogleTranslateFree("google_translate_free"),
    GcpTranslate("gcp_translate"),
    GeminiLegacy("gemini_legacy"),
}

@Serializable
enum class TtsProviderId(val wireId: String) {
    GoogleOnlineTts("google_online_tts"),
    SystemFallbackLowQuality("system_or_browser_fallback_low_quality"),
}
