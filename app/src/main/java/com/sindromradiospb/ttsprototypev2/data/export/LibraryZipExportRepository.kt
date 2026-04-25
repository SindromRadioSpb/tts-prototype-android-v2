package com.sindromradiospb.ttsprototypev2.data.export

import androidx.room.withTransaction
import com.sindromradiospb.ttsprototypev2.core.export.ExportAudioAsset
import com.sindromradiospb.ttsprototypev2.core.export.ExportAudioFile
import com.sindromradiospb.ttsprototypev2.core.export.ExportLibraryRow
import com.sindromradiospb.ttsprototypev2.core.export.ExportLibraryText
import com.sindromradiospb.ttsprototypev2.core.export.LibraryExportLayout
import com.sindromradiospb.ttsprototypev2.core.export.LibraryExportManifest
import com.sindromradiospb.ttsprototypev2.core.export.LibraryJsonExport
import com.sindromradiospb.ttsprototypev2.core.export.MissingAudio
import com.sindromradiospb.ttsprototypev2.core.export.MissingAudioReport
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.db.AudioAssetEntity
import com.sindromradiospb.ttsprototypev2.data.db.ExportHistoryEntity
import com.sindromradiospb.ttsprototypev2.data.db.LibraryRowEntity
import com.sindromradiospb.ttsprototypev2.data.db.LibraryTextEntity
import com.sindromradiospb.ttsprototypev2.data.db.RowAudioEntity
import com.sindromradiospb.ttsprototypev2.data.db.TextAudioEntity
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class LibraryZipExportResult(
    val exportId: String,
    val zipFile: File,
    val textCount: Int,
    val rowCount: Int,
    val audioCount: Int,
    val missingAudioCount: Int,
    val partialBackup: Boolean,
)

class LibraryZipExportRepository(
    private val database: AppDatabase,
    private val appFilesDir: File,
    private val clock: () -> String = { Instant.now().toString() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val json: Json = Json {
        encodeDefaults = true
        prettyPrint = true
        ignoreUnknownKeys = true
    },
) {
    private val dao = database.libraryDao()

    suspend fun exportToZip(
        destinationZip: File,
        destinationUriRedacted: String? = null,
    ): LibraryZipExportResult {
        val exportId = idFactory()
        val createdAt = clock()
        val snapshot = database.withTransaction { readSnapshot() }
        val temporaryZip = File(destinationZip.parentFile ?: File("."), "${destinationZip.name}.tmp")

        try {
            destinationZip.parentFile?.mkdirs()
            if (temporaryZip.exists()) {
                temporaryZip.delete()
            }

            val missingAudio = mutableListOf<MissingAudio>()
            val exportedAudioFiles = mutableListOf<ExportAudioFile>()
            val exportedAudioAssets = mutableListOf<ExportAudioAsset>()
            val exportedAssetKeys = mutableSetOf<String>()

            ZipOutputStream(FileOutputStream(temporaryZip).buffered()).use { zip ->
                copyLinkedAudio(
                    snapshot = snapshot,
                    zip = zip,
                    missingAudio = missingAudio,
                    exportedAudioFiles = exportedAudioFiles,
                    exportedAudioAssets = exportedAudioAssets,
                    exportedAssetKeys = exportedAssetKeys,
                )

                zip.writeJson(
                    LibraryExportLayout.LibraryFile,
                    LibraryJsonExport(
                        texts = snapshot.toExportTexts(),
                        audioAssets = exportedAudioAssets,
                    ),
                )
                zip.writeJson(
                    LibraryExportLayout.MissingAudioFile,
                    MissingAudioReport(missingAudio = missingAudio),
                )
                zip.writeJson(
                    LibraryExportLayout.ManifestFile,
                    LibraryExportManifest(
                        createdAt = createdAt,
                        partialBackup = missingAudio.isNotEmpty(),
                        textCount = snapshot.texts.size,
                        rowCount = snapshot.rows.size,
                        audioCount = exportedAudioFiles.size,
                        missingAudioCount = missingAudio.size,
                    ),
                )
            }

            if (destinationZip.exists()) {
                destinationZip.delete()
            }
            check(temporaryZip.renameTo(destinationZip)) {
                "Could not move export ZIP into final destination."
            }

            val result = LibraryZipExportResult(
                exportId = exportId,
                zipFile = destinationZip,
                textCount = snapshot.texts.size,
                rowCount = snapshot.rows.size,
                audioCount = exportedAudioFiles.size,
                missingAudioCount = missingAudio.size,
                partialBackup = missingAudio.isNotEmpty(),
            )
            recordHistory(result, createdAt, destinationUriRedacted, status = "success", errorCategory = null)
            return result
        } catch (error: Exception) {
            temporaryZip.delete()
            recordHistory(
                result = LibraryZipExportResult(
                    exportId = exportId,
                    zipFile = destinationZip,
                    textCount = snapshot.texts.size,
                    rowCount = snapshot.rows.size,
                    audioCount = 0,
                    missingAudioCount = 0,
                    partialBackup = true,
                ),
                createdAt = createdAt,
                destinationUriRedacted = destinationUriRedacted,
                status = "failed",
                errorCategory = "export_failure",
            )
            throw error
        }
    }

    private suspend fun readSnapshot(): ExportSnapshot =
        ExportSnapshot(
            texts = dao.getTextsForExport(),
            rows = dao.getRowsForExport(),
            audioAssets = dao.getAudioAssetsForExport(),
            rowAudio = dao.getDefaultRowAudioForExport(),
            textAudio = dao.getDefaultTextAudioForExport(),
        )

    private fun copyLinkedAudio(
        snapshot: ExportSnapshot,
        zip: ZipOutputStream,
        missingAudio: MutableList<MissingAudio>,
        exportedAudioFiles: MutableList<ExportAudioFile>,
        exportedAudioAssets: MutableList<ExportAudioAsset>,
        exportedAssetKeys: MutableSet<String>,
    ) {
        val assetsByKey = snapshot.audioAssets.associateBy { it.assetKey }
        val rowsById = snapshot.rows.associateBy { it.rowId }

        for (rowAudio in snapshot.rowAudio) {
            val row = rowsById[rowAudio.rowId]
            val owner = MissingAudio(ownerType = "row", textId = row?.textId, rowId = rowAudio.rowId, assetKey = rowAudio.assetKey)
            copyLinkedAsset(
                asset = assetsByKey[rowAudio.assetKey],
                owner = owner,
                zip = zip,
                missingAudio = missingAudio,
                exportedAudioFiles = exportedAudioFiles,
                exportedAudioAssets = exportedAudioAssets,
                exportedAssetKeys = exportedAssetKeys,
            )
        }

        for (textAudio in snapshot.textAudio) {
            copyLinkedAsset(
                asset = assetsByKey[textAudio.assetKey],
                owner = MissingAudio(ownerType = "text", textId = textAudio.textId, rowId = null, assetKey = textAudio.assetKey),
                zip = zip,
                missingAudio = missingAudio,
                exportedAudioFiles = exportedAudioFiles,
                exportedAudioAssets = exportedAudioAssets,
                exportedAssetKeys = exportedAssetKeys,
            )
        }
    }

    private fun copyLinkedAsset(
        asset: AudioAssetEntity?,
        owner: MissingAudio,
        zip: ZipOutputStream,
        missingAudio: MutableList<MissingAudio>,
        exportedAudioFiles: MutableList<ExportAudioFile>,
        exportedAudioAssets: MutableList<ExportAudioAsset>,
        exportedAssetKeys: MutableSet<String>,
    ) {
        if (asset == null) {
            missingAudio += owner.copy(reason = "asset_metadata_missing")
            return
        }
        if (asset.assetKey in exportedAssetKeys) {
            return
        }
        if (asset.isMissing) {
            missingAudio += owner.copy(reason = "asset_marked_missing")
            return
        }

        val exportPath = safeExportPath(asset.relativePath)
        if (exportPath == null || !exportPath.startsWith(LibraryExportLayout.AudioDir)) {
            missingAudio += owner.copy(reason = "unsafe_export_path")
            return
        }

        val sourceFile = File(appFilesDir, asset.relativePath)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            missingAudio += owner.copy(reason = "file_missing_in_app_storage")
            return
        }

        zip.putNextEntry(ZipEntry(exportPath))
        sourceFile.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()

        exportedAssetKeys += asset.assetKey
        exportedAudioFiles += ExportAudioFile(
            assetKey = asset.assetKey,
            relativeExportPath = exportPath,
            mimeType = asset.mimeType,
            providerId = asset.providerId,
            sizeBytes = asset.sizeBytes ?: sourceFile.length(),
            durationMs = asset.durationMs,
        )
        exportedAudioAssets += asset.toExportAudioAsset(exportPath)
    }

    private fun ExportSnapshot.toExportTexts(): List<ExportLibraryText> {
        val rowsByText = rows.groupBy { it.textId }
        val defaultRowAudioByRow = rowAudio.associateBy { it.rowId }
        val defaultTextAudioByText = textAudio.associateBy { it.textId }

        return texts.map { text ->
            ExportLibraryText(
                textId = text.textId,
                textKey = text.textKey,
                title = text.title,
                level = text.level,
                tags = json.decodeFromString<List<String>>(text.tagsJson),
                sourceText = text.sourceText,
                sourceMeta = text.sourceMetaJson?.parseJsonElementOrNull(),
                tableModelMeta = text.tableModelMetaJson?.parseJsonElementOrNull(),
                rows = rowsByText[text.textId].orEmpty().map { row ->
                    ExportLibraryRow(
                        rowId = row.rowId,
                        orderIndex = row.orderIndex,
                        hebrewPlain = row.hebrewPlain,
                        hebrewNiqqud = row.hebrewNiqqud,
                        translit = row.translit,
                        translitRu = row.translitRu,
                        russian = row.russian,
                        editMeta = row.editMetaJson?.parseJsonElementOrNull(),
                        audioAssetKey = defaultRowAudioByRow[row.rowId]?.assetKey,
                    )
                },
                textAudioAssetKey = defaultTextAudioByText[text.textId]?.assetKey,
                createdAt = text.createdAt,
                updatedAt = text.updatedAt,
                isArchived = text.isArchived,
            )
        }
    }

    private fun AudioAssetEntity.toExportAudioAsset(exportPath: String): ExportAudioAsset =
        ExportAudioAsset(
            assetKey = assetKey,
            relativeExportPath = exportPath,
            mimeType = mimeType,
            providerId = providerId,
            voiceName = voiceName,
            language = language,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            contentHash = contentHash,
            provenance = provenanceJson.parseJsonElementOrNull(),
        )

    private suspend fun recordHistory(
        result: LibraryZipExportResult,
        createdAt: String,
        destinationUriRedacted: String?,
        status: String,
        errorCategory: String?,
    ) {
        dao.insertExportHistory(
            ExportHistoryEntity(
                exportId = result.exportId,
                createdAt = createdAt,
                destinationUriRedacted = destinationUriRedacted,
                textCount = result.textCount,
                rowCount = result.rowCount,
                audioCount = result.audioCount,
                missingAudioCount = result.missingAudioCount,
                partialBackup = result.partialBackup,
                schemaVersion = 1,
                status = status,
                errorCategory = errorCategory,
            ),
        )
    }

    private fun ZipOutputStream.writeJson(path: String, value: Any) {
        putNextEntry(ZipEntry(path))
        val content = when (value) {
            is LibraryJsonExport -> json.encodeToString(value)
            is MissingAudioReport -> json.encodeToString(value)
            is LibraryExportManifest -> json.encodeToString(value)
            else -> error("Unsupported export JSON type: ${value::class}")
        }
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun String.parseJsonElementOrNull(): JsonElement? =
        runCatching { json.decodeFromString<JsonElement>(this) }.getOrNull()

    private fun safeExportPath(path: String): String? {
        if (path.isBlank() || path.startsWith("/") || path.contains("\\") || path.contains(":")) {
            return null
        }
        val segments = path.split("/")
        if (segments.any { it.isBlank() || it == "." || it == ".." }) {
            return null
        }
        return segments.joinToString("/")
    }
}

private data class ExportSnapshot(
    val texts: List<LibraryTextEntity>,
    val rows: List<LibraryRowEntity>,
    val audioAssets: List<AudioAssetEntity>,
    val rowAudio: List<RowAudioEntity>,
    val textAudio: List<TextAudioEntity>,
)
