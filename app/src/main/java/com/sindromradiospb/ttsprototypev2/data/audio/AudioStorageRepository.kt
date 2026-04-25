package com.sindromradiospb.ttsprototypev2.data.audio

import androidx.room.withTransaction
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderErrorCategory
import com.sindromradiospb.ttsprototypev2.core.provider.ProviderException
import com.sindromradiospb.ttsprototypev2.core.provider.TtsResponse
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.db.AudioAssetEntity
import com.sindromradiospb.ttsprototypev2.data.db.RowAudioEntity
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AdoptedRowAudio(
    val asset: AudioAssetEntity,
    val absoluteFile: File,
)

sealed class PlayableAudioResult {
    data class Available(val asset: AudioAssetEntity, val absoluteFile: File) : PlayableAudioResult()
    data class Missing(val assetKey: String) : PlayableAudioResult()
}

class AudioStorageRepository(
    private val database: AppDatabase,
    private val appFilesDir: File,
    private val clock: () -> String = { Instant.now().toString() },
    private val json: Json = Json { encodeDefaults = true },
) {
    private val dao = database.libraryDao()

    suspend fun adoptRowAudio(
        textId: String,
        rowId: String,
        ttsResponse: TtsResponse,
    ): AdoptedRowAudio {
        val assetKey = requireNotNull(ttsResponse.audioAssetKey) { "TTS response must include audioAssetKey" }
        if (!SafeAssetKey.matches(assetKey)) {
            throw ProviderException(
                category = ProviderErrorCategory.InvalidResponse,
                providerId = ttsResponse.provenance.actualProviderId,
                userMessage = "TTS response returned an invalid audio asset key.",
            )
        }
        val sourcePath = requireNotNull(ttsResponse.localFilePath) { "TTS response must include localFilePath" }
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            throw ProviderException(
                category = ProviderErrorCategory.MissingAudio,
                providerId = ttsResponse.provenance.actualProviderId,
                userMessage = "TTS output file is missing or empty before adoption.",
            )
        }

        val extension = extensionForMime(ttsResponse.mimeType)
        val relativePath = "audio/rows/$textId/$rowId/$assetKey.$extension"
        val targetFile = File(appFilesDir, relativePath)
        targetFile.parentFile?.mkdirs()
        sourceFile.copyTo(targetFile, overwrite = true)

        val contentHash = sha256(targetFile.readBytes())
        val asset = AudioAssetEntity(
            assetKey = assetKey,
            fileName = targetFile.name,
            relativePath = relativePath,
            mimeType = ttsResponse.mimeType,
            providerId = ttsResponse.provenance.actualProviderId,
            voiceName = null,
            language = "he",
            durationMs = ttsResponse.durationMs,
            sizeBytes = ttsResponse.sizeBytes ?: targetFile.length(),
            contentHash = contentHash,
            createdAt = clock(),
            provenanceJson = json.encodeToString(ttsResponse.provenance),
            isMissing = false,
        )

        database.withTransaction {
            dao.insertAudioAsset(asset)
            dao.clearDefaultRowAudio(rowId)
            dao.insertRowAudio(
                RowAudioEntity(
                    rowId = rowId,
                    assetKey = asset.assetKey,
                    isDefault = true,
                    isStale = false,
                    staleReason = null,
                    createdAt = clock(),
                ),
            )
        }

        return AdoptedRowAudio(asset = asset, absoluteFile = targetFile)
    }

    suspend fun resolvePlayableAudio(assetKey: String): PlayableAudioResult {
        val asset = dao.getAudioAsset(assetKey) ?: return PlayableAudioResult.Missing(assetKey)
        val file = File(appFilesDir, asset.relativePath)
        if (!file.exists() || file.length() <= 0L) {
            dao.markAudioAssetMissing(assetKey)
            return PlayableAudioResult.Missing(assetKey)
        }
        return PlayableAudioResult.Available(asset = asset, absoluteFile = file)
    }

    private fun extensionForMime(mimeType: String): String =
        when (mimeType.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            "audio/ogg" -> "ogg"
            else -> "bin"
        }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val SafeAssetKey = Regex("[A-Za-z0-9._-]+")
    }
}
