package com.sindromradiospb.ttsprototypev2.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "library_texts",
    indices = [
        Index(value = ["text_key"], unique = true),
        Index(value = ["updated_at"]),
        Index(value = ["is_archived", "updated_at"]),
    ],
)
data class LibraryTextEntity(
    @PrimaryKey
    @ColumnInfo(name = "text_id")
    val textId: String,
    @ColumnInfo(name = "text_key")
    val textKey: String,
    val title: String,
    val level: String?,
    @ColumnInfo(name = "tags_json")
    val tagsJson: String,
    @ColumnInfo(name = "source_text")
    val sourceText: String,
    @ColumnInfo(name = "source_meta_json")
    val sourceMetaJson: String?,
    @ColumnInfo(name = "table_model_meta_json")
    val tableModelMetaJson: String?,
    @ColumnInfo(name = "tts_profile_json")
    val ttsProfileJson: String?,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "last_opened_at")
    val lastOpenedAt: String?,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
)

@Entity(
    tableName = "library_rows",
    foreignKeys = [
        ForeignKey(
            entity = LibraryTextEntity::class,
            parentColumns = ["text_id"],
            childColumns = ["text_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["text_id"]),
        Index(value = ["text_id", "order_index"], unique = true),
        Index(value = ["row_hash"]),
    ],
)
data class LibraryRowEntity(
    @PrimaryKey
    @ColumnInfo(name = "row_id")
    val rowId: String,
    @ColumnInfo(name = "text_id")
    val textId: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "hebrew_plain")
    val hebrewPlain: String,
    @ColumnInfo(name = "hebrew_niqqud")
    val hebrewNiqqud: String,
    val translit: String,
    @ColumnInfo(name = "translit_ru")
    val translitRu: String,
    val russian: String,
    @ColumnInfo(name = "row_hash")
    val rowHash: String?,
    @ColumnInfo(name = "edit_meta_json")
    val editMetaJson: String?,
    @ColumnInfo(name = "source_meta_json")
    val sourceMetaJson: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "audio_assets",
    indices = [
        Index(value = ["provider_id", "created_at"]),
        Index(value = ["content_hash"]),
    ],
)
data class AudioAssetEntity(
    @PrimaryKey
    @ColumnInfo(name = "asset_key")
    val assetKey: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "voice_name")
    val voiceName: String?,
    val language: String,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long?,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long?,
    @ColumnInfo(name = "content_hash")
    val contentHash: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "provenance_json")
    val provenanceJson: String,
    @ColumnInfo(name = "is_missing")
    val isMissing: Boolean,
)

@Entity(
    tableName = "row_audio",
    primaryKeys = ["row_id", "asset_key"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryRowEntity::class,
            parentColumns = ["row_id"],
            childColumns = ["row_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AudioAssetEntity::class,
            parentColumns = ["asset_key"],
            childColumns = ["asset_key"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["asset_key"])],
)
data class RowAudioEntity(
    @ColumnInfo(name = "row_id")
    val rowId: String,
    @ColumnInfo(name = "asset_key")
    val assetKey: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "is_stale")
    val isStale: Boolean,
    @ColumnInfo(name = "stale_reason")
    val staleReason: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "text_audio",
    primaryKeys = ["text_id", "asset_key"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryTextEntity::class,
            parentColumns = ["text_id"],
            childColumns = ["text_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AudioAssetEntity::class,
            parentColumns = ["asset_key"],
            childColumns = ["asset_key"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["asset_key"])],
)
data class TextAudioEntity(
    @ColumnInfo(name = "text_id")
    val textId: String,
    @ColumnInfo(name = "asset_key")
    val assetKey: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "is_stale")
    val isStale: Boolean,
    @ColumnInfo(name = "stale_reason")
    val staleReason: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(tableName = "export_history")
data class ExportHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "export_id")
    val exportId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "destination_uri_redacted")
    val destinationUriRedacted: String?,
    @ColumnInfo(name = "text_count")
    val textCount: Int,
    @ColumnInfo(name = "row_count")
    val rowCount: Int,
    @ColumnInfo(name = "audio_count")
    val audioCount: Int,
    @ColumnInfo(name = "missing_audio_count")
    val missingAudioCount: Int,
    @ColumnInfo(name = "partial_backup")
    val partialBackup: Boolean,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
    val status: String,
    @ColumnInfo(name = "error_category")
    val errorCategory: String?,
)

@Entity(
    tableName = "provider_call_log",
    indices = [Index(value = ["provider_id", "created_at"])],
)
data class ProviderCallLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    val operation: String,
    val status: String,
    @ColumnInfo(name = "error_category")
    val errorCategory: String?,
    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long?,
    val model: String?,
    @ColumnInfo(name = "quota_sensitive")
    val quotaSensitive: Boolean,
    @ColumnInfo(name = "request_summary_json")
    val requestSummaryJson: String?,
    @ColumnInfo(name = "response_summary_json")
    val responseSummaryJson: String?,
)
