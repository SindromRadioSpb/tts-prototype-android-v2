package com.sindromradiospb.ttsprototypev2.core.export

import kotlinx.serialization.Serializable

@Serializable
data class LibraryExportManifest(
    val exportType: String = "tts-prototype-android-v2-library",
    val exportVersion: Int = 1,
    val appSchemaVersion: Int = 1,
    val exportedAt: String,
    val textCount: Int,
    val rowCount: Int,
    val audioFiles: List<ExportAudioFile>,
    val missingAudio: List<MissingAudio>,
)

@Serializable
data class ExportAudioFile(
    val assetKey: String,
    val relativePath: String,
    val mimeType: String,
    val providerId: String,
    val sizeBytes: Long? = null,
    val durationMs: Long? = null,
)

@Serializable
data class MissingAudio(
    val ownerType: String,
    val ownerId: String,
    val assetKey: String?,
    val reason: String,
)

object LibraryExportLayout {
    const val ManifestFile = "manifest.json"
    const val LibraryFile = "library.json"
    const val AudioDir = "audio/"
    const val MetadataDir = "metadata/"
    const val AppVersionFile = "metadata/app_version.json"
    const val ProviderProvenanceFile = "metadata/provider_provenance.json"
}
