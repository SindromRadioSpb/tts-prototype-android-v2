package com.sindromradiospb.ttsprototypev2.data.provider.tts

import com.sindromradiospb.ttsprototypev2.core.model.TtsProfile
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.TtsProviderRegistry
import com.sindromradiospb.ttsprototypev2.core.provider.TtsRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsProvidersTest {
    @Test
    fun fakeTtsProviderReturnsDeterministicMetadata() = runTest {
        val provider = FakeTtsProvider(
            id = TtsProviderId.GoogleOnlineTts,
            clock = { "2026-04-25T04:00:00Z" },
        )

        val result = provider.synthesize(sampleRequest(TtsProviderId.GoogleOnlineTts))

        val response = result.getOrThrow()
        assertEquals("google_online_tts", response.provenance.actualProviderId)
        assertEquals("m4_fake_tts_provider", response.provenance.model)
        assertEquals("audio/x-fake", response.mimeType)
        assertTrue(response.audioAssetKey.orEmpty().length == 64)
        assertEquals("${response.audioAssetKey}.fake", response.localFileName)
    }

    @Test
    fun deterministicAssetKeyChangesWithProviderAndText() {
        val first = deterministicAudioAssetKey(sampleRequest(TtsProviderId.GoogleOnlineTts, text = "שלום"))
        val same = deterministicAudioAssetKey(sampleRequest(TtsProviderId.GoogleOnlineTts, text = "שלום"))
        val otherProvider = deterministicAudioAssetKey(sampleRequest(TtsProviderId.SystemFallbackLowQuality, text = "שלום"))
        val otherText = deterministicAudioAssetKey(sampleRequest(TtsProviderId.GoogleOnlineTts, text = "עולם"))

        assertEquals(first, same)
        assertNotEquals(first, otherProvider)
        assertNotEquals(first, otherText)
    }

    @Test
    fun missingConfigurationTtsProviderDoesNotFallback() = runTest {
        val provider = MissingConfigurationTtsProvider(
            id = TtsProviderId.GoogleOnlineTts,
            message = "secure settings required",
        )

        val result = provider.synthesize(sampleRequest(TtsProviderId.GoogleOnlineTts))

        val error = result.exceptionOrNull() as ProviderException
        assertEquals(ProviderErrorCategory.MissingConfiguration, error.category)
        assertEquals("google_online_tts", error.providerId)
    }

    @Test
    fun registryRejectsMissingAllowedTtsProviderAtLookup() {
        val registry = TtsProviderRegistry(
            listOf(FakeTtsProvider(id = TtsProviderId.SystemFallbackLowQuality)),
        )

        val error = assertProviderException {
            registry.get(TtsProviderId.GoogleOnlineTts)
        }

        assertEquals(ProviderErrorCategory.MissingConfiguration, error.category)
        assertEquals("google_online_tts", error.providerId)
    }

    private fun sampleRequest(
        providerId: TtsProviderId,
        text: String = "שלום עולם",
    ): TtsRequest =
        TtsRequest(
            text = text,
            profile = TtsProfile(
                providerId = providerId,
                language = "he-IL",
                voiceName = "test-voice",
                speakingRate = 1.0,
                pitch = 0.0,
            ),
        )

    private fun assertProviderException(block: () -> Unit): ProviderException {
        try {
            block()
        } catch (error: ProviderException) {
            return error
        }
        throw AssertionError("Expected ProviderException")
    }
}
