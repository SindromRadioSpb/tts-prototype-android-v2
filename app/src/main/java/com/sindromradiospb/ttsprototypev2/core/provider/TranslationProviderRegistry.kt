package com.sindromradiospb.ttsprototypev2.core.provider

import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId

class TranslationProviderRegistry(
    providers: List<TranslationProvider>,
) {
    private val byId = providers.associateBy { it.id }

    init {
        val registeredIds = byId.keys
        require(registeredIds.all { it in AndroidV2ProviderPolicy.allowedTranslationProviders }) {
            "Translation registry contains providers outside Android v2 allowlist: $registeredIds"
        }
    }

    fun get(providerId: TranslationProviderId): TranslationProvider {
        AndroidV2ProviderPolicy.requireAllowed(providerId)
        return byId[providerId]
            ?: throw ProviderException(
                category = ProviderErrorCategory.MissingConfiguration,
                providerId = providerId.wireId,
                userMessage = "Provider is allowed but not configured: ${providerId.wireId}",
            )
    }
}
