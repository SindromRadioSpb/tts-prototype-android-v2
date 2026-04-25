package com.sindromradiospb.ttsprototypev2.core.provider

import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId

class TtsProviderRegistry(
    providers: List<TtsProvider>,
) {
    private val byId = providers.associateBy { it.id }

    init {
        val registeredIds = byId.keys
        require(registeredIds.all { it in AndroidV2ProviderPolicy.allowedTtsProviders }) {
            "TTS registry contains providers outside Android v2 allowlist: $registeredIds"
        }
    }

    fun get(providerId: TtsProviderId): TtsProvider {
        AndroidV2ProviderPolicy.requireAllowed(providerId)
        return byId[providerId]
            ?: throw ProviderException(
                category = ProviderErrorCategory.MissingConfiguration,
                providerId = providerId.wireId,
                userMessage = "TTS provider is allowed but not configured: ${providerId.wireId}",
            )
    }
}
