package com.sindromradiospb.ttsprototypev2.core.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryExportManifest(
    @SerialName("export_schema_version")
    val exportSchemaVersion: Int = 1,
    @SerialName("app_id")
    val appId: String = "com.sindromradiospb.ttsprototypev2",
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("partial_backup")
    val partialBackup: Boolean,
    @SerialName("text_count")
    val textCount: Int,
    @SerialName("row_count")
    val rowCount: Int,
    @SerialName("audio_count")
    val audioCount: Int,
    @SerialName("missing_audio_count")
    val missingAudioCount: Int,
    @SerialName("contains_secrets")
    val containsSecrets: Boolean = false,
    @SerialName("library_json_path")
    val libraryJsonPath: String = LibraryExportLayout.LibraryFile,
    @SerialName("missing_audio_path")
    val missingAudioPath: String = LibraryExportLayout.MissingAudioFile,
)

@Serializable
data class ExportAudioFile(
    @SerialName("asset_key")
    val assetKey: String,
    @SerialName("relative_export_path")
    val relativeExportPath: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("provider_id")
    val providerId: String,
    @SerialName("size_bytes")
    val sizeBytes: Long? = null,
    @SerialName("duration_ms")
    val durationMs: Long? = null,
)

@Serializable
data class MissingAudio(
    @SerialName("owner_type")
    val ownerType: String,
    @SerialName("text_id")
    val textId: String? = null,
    @SerialName("row_id")
    val rowId: String? = null,
    @SerialName("asset_key")
    val assetKey: String?,
    val reason: String = "",
)

@Serializable
data class LibraryJsonExport(
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    val texts: List<ExportLibraryText>,
    @SerialName("audio_assets")
    val audioAssets: List<ExportAudioAsset>,
)

@Serializable
data class ExportLibraryText(
    @SerialName("text_id")
    val textId: String,
    @SerialName("text_key")
    val textKey: String,
    val title: String,
    val level: String?,
    val tags: List<String>,
    @SerialName("source_label")
    val sourceLabel: String?,
    val topic: String?,
    @SerialName("source_text")
    val sourceText: String,
    @SerialName("source_meta")
    val sourceMeta: kotlinx.serialization.json.JsonElement?,
    @SerialName("table_model_meta")
    val tableModelMeta: kotlinx.serialization.json.JsonElement?,
    val rows: List<ExportLibraryRow>,
    @SerialName("text_audio_asset_key")
    val textAudioAssetKey: String?,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("is_archived")
    val isArchived: Boolean,
)

@Serializable
data class ExportLibraryRow(
    @SerialName("row_id")
    val rowId: String,
    @SerialName("order_index")
    val orderIndex: Int,
    @SerialName("hebrew_plain")
    val hebrewPlain: String,
    @SerialName("hebrew_niqqud")
    val hebrewNiqqud: String,
    val translit: String,
    @SerialName("translit_ru")
    val translitRu: String,
    val russian: String,
    @SerialName("edit_meta")
    val editMeta: kotlinx.serialization.json.JsonElement?,
    @SerialName("audio_asset_key")
    val audioAssetKey: String?,
)

@Serializable
data class ExportAudioAsset(
    @SerialName("asset_key")
    val assetKey: String,
    @SerialName("relative_export_path")
    val relativeExportPath: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("provider_id")
    val providerId: String,
    @SerialName("voice_name")
    val voiceName: String?,
    val language: String,
    @SerialName("duration_ms")
    val durationMs: Long?,
    @SerialName("size_bytes")
    val sizeBytes: Long?,
    @SerialName("content_hash")
    val contentHash: String?,
    val provenance: kotlinx.serialization.json.JsonElement?,
)

@Serializable
data class MissingAudioReport(
    @SerialName("missing_audio")
    val missingAudio: List<MissingAudio>,
)

object LibraryExportLayout {
    const val ManifestFile = "manifest.json"
    const val LibraryFile = "library/library.json"
    const val MissingAudioFile = "metadata/missing_audio.json"
    const val AudioDir = "audio/"
    const val MetadataDir = "metadata/"
    const val AppVersionFile = "metadata/app_version.json"
    const val ProviderProvenanceFile = "metadata/provider_provenance.json"
}
