package com.sindromradiospb.ttsprototypev2.core.provider

import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderPolicyTest {
    @Test
    fun androidV2TranslationProvidersAreRestricted() {
        assertEquals(
            setOf(
                TranslationProviderId.GoogleTranslateFree,
                TranslationProviderId.GcpTranslate,
                TranslationProviderId.GeminiLegacy,
            ),
            AndroidV2ProviderPolicy.allowedTranslationProviders,
        )
    }

    @Test
    fun androidV2TtsProvidersAreRestricted() {
        assertEquals(
            setOf(
                TtsProviderId.GoogleOnlineTts,
                TtsProviderId.SystemFallbackLowQuality,
            ),
            AndroidV2ProviderPolicy.allowedTtsProviders,
        )
    }
}
