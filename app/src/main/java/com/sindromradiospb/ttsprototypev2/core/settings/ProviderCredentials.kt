package com.sindromradiospb.ttsprototypev2.core.settings

enum class ProviderCredentialId(
    val wireId: String,
    val displayName: String,
    val usage: String,
) {
    GcpTranslate(
        wireId = "gcp_translate",
        displayName = "GCP Translate",
        usage = "Translation provider configuration",
    ),
    GeminiLegacy(
        wireId = "gemini_legacy",
        displayName = "Gemini Legacy",
        usage = "Translation provider configuration",
    ),
    GoogleOnlineTts(
        wireId = "google_online_tts",
        displayName = "Google Online TTS",
        usage = "TTS provider configuration",
    ),
}

data class ProviderCredentialStatus(
    val id: ProviderCredentialId,
    val isConfigured: Boolean,
    val maskedValue: String? = null,
)
