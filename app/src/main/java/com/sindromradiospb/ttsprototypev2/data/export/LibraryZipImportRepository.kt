package com.sindromradiospb.ttsprototypev2.data.export

import androidx.room.withTransaction
import com.sindromradiospb.ttsprototypev2.core.export.ExportAudioAsset
import com.sindromradiospb.ttsprototypev2.core.export.LibraryJsonExport
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.db.AudioAssetEntity
import com.sindromradiospb.ttsprototypev2.data.db.LibraryRowEntity
import com.sindromradiospb.ttsprototypev2.data.db.LibraryTextEntity
import com.sindromradiospb.ttsprototypev2.data.db.RowAudioEntity
import com.sindromradiospb.ttsprototypev2.data.db.TextAudioEntity
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ZipImportResult(
    val importedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val rowCount: Int,
    val importedAudio: Int,
    val linkedAudio: Int,
    val errors: List<String>,
)

enum class ZipImportMode {
    SKIP,
    AS_NEW,
}

class LibraryZipImportRepository(
    private val database: AppDatabase,
    private val appFilesDir: File,
    private val clock: () -> String = { Instant.now().toString() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    },
) {
    private val dao = database.libraryDao()
    private val audioDir = File(appFilesDir, "audio")

    suspend fun importFromZip(
        inputStream: InputStream,
        mode: ZipImportMode = ZipImportMode.SKIP,
    ): ZipImportResult {
        audioDir.mkdirs()

        var libraryJsonBytes: ByteArray? = null
        val extractedAudioKeys = mutableSetOf<String>()
        var importedAudio = 0

        // Single-pass ZIP read: stream audio to disk, buffer JSON files
        ZipInputStream(inputStream.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    when {
                        entry.name == "library/library.json" -> {
                            libraryJsonBytes = zis.readBytes()
                        }
                        entry.name.startsWith("audio/") && entry.name.endsWith(".mp3") -> {
                            val assetKey = entry.name.substringAfterLast("/").removeSuffix(".mp3")
                            if (assetKey.matches(Regex("[0-9a-f]{64}"))) {
                                val destFile = File(audioDir, "$assetKey.mp3")
                                if (!destFile.exists()) {
                                    val tmp = File(audioDir, "$assetKey.tmp")
                                    try {
                                        tmp.outputStream().buffered().use { out -> zis.copyTo(out) }
                                        tmp.renameTo(destFile)
                                        importedAudio++
                                    } catch (_: Exception) {
                                        tmp.delete()
                                    }
                                }
                                if (destFile.exists() && destFile.length() > 0) {
                                    extractedAudioKeys += assetKey
                                }
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val jsonBytes = libraryJsonBytes
            ?: return ZipImportResult(0, 0, 1, 0, importedAudio, 0, listOf("library/library.json not found in ZIP"))

        val libraryExport = try {
            json.decodeFromString<LibraryJsonExport>(String(jsonBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            return ZipImportResult(0, 0, 1, 0, importedAudio, 0, listOf("Failed to parse library/library.json: ${e.message}"))
        }

        val audioAssetsMeta = libraryExport.audioAssets.associateBy { it.assetKey }
        val now = clock()

        return database.withTransaction {
            // Upsert audio asset records for files that were extracted
            for (assetKey in extractedAudioKeys) {
                if (dao.getAudioAsset(assetKey) != null) continue
                val meta = audioAssetsMeta[assetKey]
                val relativePath = "audio/$assetKey.mp3"
                val file = File(appFilesDir, relativePath)
                try {
                    dao.insertAudioAsset(buildAudioAssetEntity(assetKey, meta, file, relativePath, now))
                } catch (_: Exception) {
                    // Constraint violation means asset already exists; safe to skip
                }
            }

            var importedCount = 0
            var skippedCount = 0
            var errorCount = 0
            var rowCount = 0
            var linkedAudio = 0
            val errors = mutableListOf<String>()

            for (exportText in libraryExport.texts) {
                val titleForError = exportText.title.ifBlank { "Untitled" }
                try {
                    if (exportText.sourceText.isBlank()) {
                        errorCount++
                        errors += "$titleForError: NO_SOURCE_TEXT"
                        continue
                    }
                    if (exportText.rows.isEmpty()) {
                        errorCount++
                        errors += "$titleForError: NO_ROWS"
                        continue
                    }

                    var textKey = exportText.textKey
                    if (mode == ZipImportMode.SKIP && dao.getTextByKey(textKey) != null) {
                        skippedCount++
                        continue
                    }
                    if (mode == ZipImportMode.AS_NEW) {
                        textKey = "${textKey.take(32)}_import_${idFactory()}"
                    }

                    val textId = idFactory()
                    val createdAt = exportText.createdAt.takeIf { it.isNotBlank() } ?: now
                    val updatedAt = exportText.updatedAt.takeIf { it.isNotBlank() } ?: now

                    dao.insertText(
                        LibraryTextEntity(
                            textId = textId,
                            textKey = textKey,
                            title = exportText.title,
                            level = exportText.level,
                            tagsJson = json.encodeToString(exportText.tags),
                            sourceLabel = exportText.sourceLabel,
                            topic = exportText.topic,
                            sourceText = exportText.sourceText,
                            sourceMetaJson = exportText.sourceMeta?.toString(),
                            tableModelMetaJson = exportText.tableModelMeta?.toString(),
                            ttsProfileJson = null,
                            isArchived = exportText.isArchived,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            lastOpenedAt = null,
                            schemaVersion = 1,
                        ),
                    )

                    val rowEntities = exportText.rows.mapIndexed { idx, exportRow ->
                        LibraryRowEntity(
                            rowId = idFactory(),
                            textId = textId,
                            orderIndex = exportRow.orderIndex.takeIf { it >= 0 } ?: idx,
                            hebrewPlain = exportRow.hebrewPlain,
                            hebrewNiqqud = exportRow.hebrewNiqqud,
                            translit = exportRow.translit,
                            translitRu = exportRow.translitRu,
                            russian = exportRow.russian,
                            rowHash = null,
                            editMetaJson = exportRow.editMeta?.toString(),
                            sourceMetaJson = null,
                            createdAt = now,
                            updatedAt = now,
                        )
                    }
                    dao.insertRows(rowEntities)
                    rowCount += rowEntities.size
                    importedCount++

                    // Link row audio
                    exportText.rows.forEachIndexed { idx, exportRow ->
                        val ak = exportRow.audioAssetKey ?: return@forEachIndexed
                        if (ak !in extractedAudioKeys) return@forEachIndexed
                        val rowEntity = rowEntities.getOrNull(idx) ?: return@forEachIndexed
                        try {
                            dao.insertRowAudio(
                                RowAudioEntity(
                                    rowId = rowEntity.rowId,
                                    assetKey = ak,
                                    isDefault = true,
                                    isStale = false,
                                    staleReason = null,
                                    createdAt = now,
                                ),
                            )
                            linkedAudio++
                        } catch (_: Exception) {}
                    }

                    // Link text audio
                    val textAk = exportText.textAudioAssetKey
                    if (textAk != null && textAk in extractedAudioKeys) {
                        try {
                            dao.insertTextAudio(
                                TextAudioEntity(
                                    textId = textId,
                                    assetKey = textAk,
                                    isDefault = true,
                                    isStale = false,
                                    staleReason = null,
                                    createdAt = now,
                                ),
                            )
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("UNIQUE constraint", ignoreCase = true) &&
                        msg.contains("text_key", ignoreCase = true)
                    ) {
                        skippedCount++
                    } else {
                        errorCount++
                        errors += "$titleForError: $msg"
                    }
                }
            }

            ZipImportResult(
                importedCount = importedCount,
                skippedCount = skippedCount,
                errorCount = errorCount,
                rowCount = rowCount,
                importedAudio = importedAudio,
                linkedAudio = linkedAudio,
                errors = errors,
            )
        }
    }

    private fun buildAudioAssetEntity(
        assetKey: String,
        meta: ExportAudioAsset?,
        file: File,
        relativePath: String,
        now: String,
    ): AudioAssetEntity = AudioAssetEntity(
        assetKey = assetKey,
        fileName = "$assetKey.mp3",
        relativePath = relativePath,
        mimeType = meta?.mimeType ?: "audio/mpeg",
        providerId = meta?.providerId ?: "unknown",
        voiceName = meta?.voiceName,
        language = meta?.language ?: "he",
        durationMs = meta?.durationMs,
        sizeBytes = if (file.exists()) file.length() else meta?.sizeBytes,
        contentHash = meta?.contentHash ?: assetKey,
        createdAt = now,
        provenanceJson = meta?.provenance?.toString() ?: "{}",
        isMissing = !file.exists(),
    )
}
