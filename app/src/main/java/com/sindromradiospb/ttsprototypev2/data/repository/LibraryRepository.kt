package com.sindromradiospb.ttsprototypev2.data.repository

import androidx.room.withTransaction
import com.sindromradiospb.ttsprototypev2.core.model.EditMeta
import com.sindromradiospb.ttsprototypev2.core.model.LibraryRow
import com.sindromradiospb.ttsprototypev2.core.model.LibraryText
import com.sindromradiospb.ttsprototypev2.core.model.SourceMeta
import com.sindromradiospb.ttsprototypev2.core.model.TableModelMeta
import com.sindromradiospb.ttsprototypev2.core.model.TtsProfile
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.db.AudioAssetEntity
import com.sindromradiospb.ttsprototypev2.data.db.LibraryRowEntity
import com.sindromradiospb.ttsprototypev2.data.db.LibraryTextEntity
import com.sindromradiospb.ttsprototypev2.data.db.RowAudioEntity
import com.sindromradiospb.ttsprototypev2.data.db.SentenceNoteEntity
import com.sindromradiospb.ttsprototypev2.data.db.TextAudioEntity
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class LibraryTextSummary(
    val textId: String,
    val textKey: String,
    val title: String,
    val level: String?,
    val tags: List<String>,
    val sourceLabel: String?,
    val topic: String?,
    val createdAt: String,
    val updatedAt: String,
    val lastOpenedAt: String?,
    val isArchived: Boolean,
)

data class SaveGeneratedTextRequest(
    val title: String,
    val level: String? = null,
    val tags: List<String> = emptyList(),
    val sourceLabel: String? = null,
    val topic: String? = null,
    val sourceText: String,
    val sourceMeta: SourceMeta? = null,
    val tableModelMeta: TableModelMeta? = null,
    val ttsProfile: TtsProfile? = null,
    val rows: List<GeneratedLibraryRowInput>,
)

data class GeneratedLibraryRowInput(
    val hebrewPlain: String,
    val hebrewNiqqud: String = "",
    val translit: String = "",
    val translitRu: String = "",
    val russian: String = "",
    val rowHash: String? = null,
)

enum class RowField {
    HebrewPlain,
    HebrewNiqqud,
    Translit,
    TranslitRu,
    Russian,
}

data class EditableRowFields(
    val values: Map<RowField, String>,
)

data class TextMetadataUpdate(
    val title: String,
    val level: String?,
    val tags: List<String>,
    val sourceLabel: String?,
    val topic: String?,
)

sealed class SaveTextResult {
    data class Saved(val text: LibraryText) : SaveTextResult()
    data class Conflict(val existingTextId: String, val textKey: String) : SaveTextResult()
}

data class AudioAssetInput(
    val assetKey: String,
    val fileName: String,
    val relativePath: String,
    val mimeType: String,
    val providerId: String,
    val voiceName: String? = null,
    val language: String,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val contentHash: String? = null,
    val provenanceJson: String,
    val isMissing: Boolean = false,
)

enum class LegacyWebImportMode {
    SKIP,
    AS_NEW,
}

data class LegacyWebLibraryImportResult(
    val mode: LegacyWebImportMode,
    val importedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val rowCount: Int,
    val missingAudioLinkCount: Int,
    val errors: List<String>,
)

interface LibraryRepository {
    fun observeTexts(includeArchived: Boolean): Flow<List<LibraryTextSummary>>
    suspend fun getText(textId: String): LibraryText
    suspend fun markOpened(textId: String, openedAt: String)
    suspend fun saveGeneratedText(input: SaveGeneratedTextRequest): SaveTextResult
    suspend fun updateGeneratedText(textId: String, input: SaveGeneratedTextRequest): SaveTextResult
    suspend fun saveRowNote(textId: String, rowId: String, note: String): LibraryRow
    suspend fun deleteRowNote(textId: String, rowId: String): LibraryRow
}

class RoomLibraryRepository(
    private val database: AppDatabase,
    private val clock: () -> String = { Instant.now().toString() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
) : LibraryRepository {
    private val dao = database.libraryDao()

    override fun observeTexts(includeArchived: Boolean): Flow<List<LibraryTextSummary>> =
        dao.observeTextSummaries(includeArchived).map { rows ->
            rows.map {
                LibraryTextSummary(
                    textId = it.textId,
                    textKey = it.textKey,
                    title = it.title,
                    level = it.level,
                    tags = decodeTags(it.tagsJson),
                    sourceLabel = it.sourceLabel,
                    topic = it.topic,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    lastOpenedAt = it.lastOpenedAt,
                    isArchived = it.isArchived,
                )
            }
        }

    override suspend fun getText(textId: String): LibraryText =
        database.withTransaction { requireText(textId) }

    override suspend fun saveGeneratedText(input: SaveGeneratedTextRequest): SaveTextResult =
        database.withTransaction {
            val textKey = computeTextKey(input)
            val existing = dao.getTextByKey(textKey)
            if (existing != null) {
                return@withTransaction SaveTextResult.Conflict(existing.textId, textKey)
            }

            val now = clock()
            val textId = idFactory()
            dao.insertText(input.toEntity(textId = textId, textKey = textKey, now = now))
            dao.insertRows(input.rows.toEntities(textId = textId, now = now, existingRows = emptyList()))
            SaveTextResult.Saved(requireText(textId))
        }

    override suspend fun saveRowNote(textId: String, rowId: String, note: String): LibraryRow =
        database.withTransaction {
            requireRow(textId, rowId)
            val trimmed = note.trim()
            if (trimmed.isBlank()) {
                dao.deleteNote(textId, rowId)
            } else {
                require(trimmed.length <= 16_000) { "Note is too long" }
                val existing = dao.getNote(textId, rowId)
                val now = clock()
                dao.upsertNote(
                    SentenceNoteEntity(
                        noteId = existing?.noteId ?: idFactory(),
                        textId = textId,
                        sentenceId = rowId,
                        note = trimmed,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    ),
                )
            }
            requireRow(textId, rowId).toDomain(
                defaultAudioAssetKey = dao.getDefaultRowAudio(rowId)?.assetKey,
                note = dao.getNote(textId, rowId)?.note,
            )
        }

    override suspend fun deleteRowNote(textId: String, rowId: String): LibraryRow =
        database.withTransaction {
            requireRow(textId, rowId)
            dao.deleteNote(textId, rowId)
            requireRow(textId, rowId).toDomain(
                defaultAudioAssetKey = dao.getDefaultRowAudio(rowId)?.assetKey,
                note = null,
            )
        }

    override suspend fun updateGeneratedText(textId: String, input: SaveGeneratedTextRequest): SaveTextResult =
        database.withTransaction {
            val current = dao.getText(textId) ?: error("Library text not found: $textId")
            val now = clock()
            val textKey = computeTextKey(input)
            val duplicate = dao.getTextByKey(textKey)
            if (duplicate != null && duplicate.textId != textId) {
                return@withTransaction SaveTextResult.Conflict(duplicate.textId, textKey)
            }

            val existingRows = dao.getRows(textId)
            dao.updateText(input.toEntity(textId = textId, textKey = textKey, now = now, createdAt = current.createdAt))
            val nextRows = input.rows.toEntities(textId = textId, now = now, existingRows = existingRows)
            val nextRowIds = nextRows.map { it.rowId }.toSet()
            val existingRowIds = existingRows.map { it.rowId }.toSet()
            val rowsToUpdate = nextRows.filter { it.rowId in existingRowIds }
            val rowsToInsert = nextRows.filter { it.rowId !in existingRowIds }
            val rowsToDelete = existingRows.filter { it.rowId !in nextRowIds }

            if (rowsToUpdate.isNotEmpty()) {
                dao.updateRows(rowsToUpdate)
            }
            if (rowsToInsert.isNotEmpty()) {
                dao.insertRows(rowsToInsert)
            }
            for (row in rowsToDelete) {
                dao.deleteRow(row)
            }
            SaveTextResult.Saved(requireText(textId))
        }

    suspend fun patchRow(textId: String, rowId: String, fields: EditableRowFields): LibraryRow =
        database.withTransaction {
            require(fields.values.isNotEmpty()) { "At least one field is required" }
            val row = requireRow(textId, rowId)
            val editMeta = decodeEditMeta(row.editMetaJson)
            val original = editMeta.original.toMutableMap()
            val edited = editMeta.edited.toMutableMap()
            var changedHebrewForAudio = false

            var next = row
            for ((field, value) in fields.values) {
                val key = field.key
                if (!original.containsKey(key)) {
                    original[key] = row.valueFor(field)
                }
                edited[key] = true
                next = next.withValue(field, value)
                if (field == RowField.HebrewPlain || field == RowField.HebrewNiqqud) {
                    changedHebrewForAudio = true
                }
            }

            val updated = next.copy(
                editMetaJson = json.encodeToString(EditMeta(edited = edited, original = original)),
                updatedAt = clock(),
            )
            dao.updateRow(updated)
            if (changedHebrewForAudio) {
                dao.markDefaultRowAudioStale(rowId, "row_text_changed")
            }
            updated.toDomain(defaultAudioAssetKey = dao.getDefaultRowAudio(rowId)?.assetKey)
        }

    suspend fun resetRowFields(textId: String, rowId: String, fields: Set<RowField>): LibraryRow =
        database.withTransaction {
            require(fields.isNotEmpty()) { "At least one field is required" }
            val row = requireRow(textId, rowId)
            val editMeta = decodeEditMeta(row.editMetaJson)
            val edited = editMeta.edited.toMutableMap()
            var next = row
            var changedHebrewForAudio = false

            for (field in fields) {
                val key = field.key
                if (editMeta.original.containsKey(key)) {
                    next = next.withValue(field, editMeta.original[key].orEmpty())
                    edited[key] = false
                    if (field == RowField.HebrewPlain || field == RowField.HebrewNiqqud) {
                        changedHebrewForAudio = true
                    }
                }
            }

            val updated = next.copy(
                editMetaJson = json.encodeToString(editMeta.copy(edited = edited)),
                updatedAt = clock(),
            )
            dao.updateRow(updated)
            if (changedHebrewForAudio) {
                dao.markDefaultRowAudioStale(rowId, "row_text_changed")
            }
            updated.toDomain(defaultAudioAssetKey = dao.getDefaultRowAudio(rowId)?.assetKey)
        }

    suspend fun reorderRows(textId: String, orderedRowIds: List<String>): List<LibraryRow> =
        database.withTransaction {
            val rows = dao.getRows(textId)
            require(rows.map { it.rowId }.toSet() == orderedRowIds.toSet()) {
                "Reorder must include exactly the current row IDs"
            }
            require(rows.size == orderedRowIds.size) { "Reorder cannot contain duplicate row IDs" }
            val byId = rows.associateBy { it.rowId }
            rewriteRowsInOrder(orderedRowIds.map { requireNotNull(byId[it]) })
            dao.getRows(textId).map { it.toDomain(defaultAudioAssetKey = dao.getDefaultRowAudio(it.rowId)?.assetKey) }
        }

    suspend fun deleteRow(textId: String, rowId: String) {
        database.withTransaction {
            val row = requireRow(textId, rowId)
            dao.deleteRow(row)
            rewriteRowsInOrder(dao.getRows(textId))
        }
    }

    suspend fun addRow(textId: String, afterRowId: String?, fields: EditableRowFields): LibraryRow =
        database.withTransaction {
            require(fields.values.isNotEmpty()) { "At least one field is required" }
            val rows = dao.getRows(textId)
            val insertIndex = if (afterRowId == null) {
                rows.size
            } else {
                rows.indexOfFirst { it.rowId == afterRowId }.also {
                    require(it >= 0) { "afterRowId does not belong to text: $afterRowId" }
                } + 1
            }
            val now = clock()
            val added = LibraryRowEntity(
                rowId = idFactory(),
                textId = textId,
                orderIndex = 10_000 + insertIndex,
                hebrewPlain = fields.values[RowField.HebrewPlain].orEmpty(),
                hebrewNiqqud = fields.values[RowField.HebrewNiqqud].orEmpty(),
                translit = fields.values[RowField.Translit].orEmpty(),
                translitRu = fields.values[RowField.TranslitRu].orEmpty(),
                russian = fields.values[RowField.Russian].orEmpty(),
                rowHash = null,
                editMetaJson = json.encodeToString(EditMeta(added = true)),
                sourceMetaJson = null,
                createdAt = now,
                updatedAt = now,
            )
            val next = rows.toMutableList()
            next.add(insertIndex, added)
            dao.insertRow(added)
            rewriteRowsInOrder(next)
            requireRow(textId, added.rowId).toDomain(defaultAudioAssetKey = null)
        }

    suspend fun archiveText(textId: String, archived: Boolean) {
        database.withTransaction {
            val current = dao.getText(textId) ?: error("Library text not found: $textId")
            dao.updateText(current.copy(isArchived = archived, updatedAt = clock()))
        }
    }

    suspend fun deleteText(textId: String) {
        database.withTransaction {
            dao.getText(textId) ?: error("Library text not found: $textId")
            dao.deleteText(textId)
        }
    }

    override suspend fun markOpened(textId: String, openedAt: String) {
        database.withTransaction {
            val current = dao.getText(textId) ?: error("Library text not found: $textId")
            dao.updateText(current.copy(lastOpenedAt = openedAt))
        }
    }

    suspend fun updateTextMetadata(textId: String, metadata: TextMetadataUpdate): LibraryText =
        database.withTransaction {
            require(metadata.title.isNotBlank()) { "Title is required" }
            val current = dao.getText(textId) ?: error("Library text not found: $textId")
            dao.updateText(
                current.copy(
                    title = metadata.title.trim(),
                    level = metadata.level?.trim()?.takeIf { it.isNotEmpty() },
                    tagsJson = json.encodeToString(normalizeTags(metadata.tags)),
                    sourceLabel = metadata.sourceLabel?.trim()?.takeIf { it.isNotEmpty() },
                    topic = metadata.topic?.trim()?.takeIf { it.isNotEmpty() },
                    updatedAt = clock(),
                ),
            )
            requireText(textId)
        }

    suspend fun importLegacyWebLibraryJson(
        payload: String,
        mode: LegacyWebImportMode = LegacyWebImportMode.SKIP,
    ): LegacyWebLibraryImportResult =
        database.withTransaction {
            val root = json.decodeFromString<JsonObject>(payload)
            val exportType = root.stringOrNull("exportType")
            require(exportType == null || exportType == "linguist-pro-library") {
                "Unsupported exportType: ${exportType.orEmpty()}"
            }

            val items = root["texts"]?.jsonArray.orEmpty()
            require(items.isNotEmpty()) { "Legacy library export does not contain texts." }

            var importedCount = 0
            var skippedCount = 0
            var errorCount = 0
            var rowCount = 0
            var missingAudioLinkCount = 0
            val errors = mutableListOf<String>()

            for (itemElement in items) {
                val item = itemElement.jsonObject
                val textObject = (item["text"] ?: item["meta"] ?: itemElement).jsonObject
                val titleForError = textObject.stringOrNull("title").orEmpty().ifBlank { "Untitled" }
                var insertedTextId: String? = null
                try {
                    val sourceText = textObject.stringOrNull("source_text")
                        ?: textObject.stringOrNull("sourceText")
                        ?: ""
                    if (sourceText.isBlank()) {
                        errorCount++
                        errors += "$titleForError: NO_SOURCE_TEXT"
                        continue
                    }

                    val ttsProfileJson = textObject.rawJsonOrNull("tts_profile_json", "ttsProfile")
                    val tableModelMetaJson = textObject.rawJsonOrNull("table_model_meta_json", "tableModelMeta")
                    val sourceMetaJson = textObject.rawJsonOrNull("source_meta_json", "sourceMeta")
                    val sourceTextKey = textObject.stringOrNull("text_key")
                        ?: textObject.stringOrNull("textKey")
                    val textKey = if (mode == LegacyWebImportMode.AS_NEW || sourceTextKey.isNullOrBlank()) {
                        computeLegacyWebTextKey(
                            sourceText = sourceText,
                            ttsProfileJson = ttsProfileJson,
                            tableModelMetaJson = if (mode == LegacyWebImportMode.AS_NEW) {
                                appendImportSalt(tableModelMetaJson)
                            } else {
                                tableModelMetaJson
                            },
                        )
                    } else {
                        sourceTextKey
                    }

                    if (mode == LegacyWebImportMode.SKIP && dao.getTextByKey(textKey) != null) {
                        skippedCount++
                        continue
                    }

                    val sentences = item["sentences"]?.jsonArray.orEmpty()
                    if (sentences.isEmpty()) {
                        errorCount++
                        errors += "$titleForError: NO_SENTENCES"
                        continue
                    }

                    val now = clock()
                    val textId = idFactory()
                    val createdAt = textObject.stringOrNull("created_at") ?: now
                    val updatedAt = textObject.stringOrNull("updated_at") ?: now
                    val progress = item["progress"]?.jsonObject
                    val lastOpenedAt = textObject.stringOrNull("last_opened_at")
                        ?: progress?.stringOrNull("lastOpenedAt")
                    val tags = textObject.tagsFromLegacy()
                    val textEntity = LibraryTextEntity(
                        textId = textId,
                        textKey = textKey,
                        title = textObject.stringOrNull("title")?.takeIf { it.isNotBlank() }
                            ?: guessTitle(sourceText),
                        level = textObject.stringOrNull("level")?.trim()?.takeIf { it.isNotEmpty() },
                        tagsJson = json.encodeToString(normalizeTags(tags)),
                        sourceLabel = textObject.stringOrNull("source")?.trim()?.takeIf { it.isNotEmpty() },
                        topic = textObject.stringOrNull("topic")?.trim()?.takeIf { it.isNotEmpty() },
                        sourceText = sourceText.trim(),
                        sourceMetaJson = sourceMetaJson,
                        tableModelMetaJson = tableModelMetaJson,
                        ttsProfileJson = ttsProfileJson,
                        isArchived = textObject.boolCompat("is_archived") || textObject.boolCompat("isArchived"),
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        lastOpenedAt = lastOpenedAt,
                        schemaVersion = 1,
                    )

                    dao.insertText(textEntity)
                    insertedTextId = textId
                    val rowEntitiesRaw = sentences.mapIndexed { index, sentenceElement ->
                        val sentence = sentenceElement.jsonObject
                        LibraryRowEntity(
                            rowId = idFactory(),
                            textId = textId,
                            orderIndex = sentence.intOrNull("order_index") ?: index,
                            hebrewPlain = sentence.stringOrNull("he_plain")
                                ?: sentence.stringOrNull("he")
                                ?: "",
                            hebrewNiqqud = sentence.stringOrNull("he_niqqud")
                                ?: sentence.stringOrNull("heNiq")
                                ?: sentence.stringOrNull("he_niqqud_text")
                                ?: "",
                            translit = sentence.stringOrNull("translit").orEmpty(),
                            translitRu = sentence.stringOrNull("translit_ru").orEmpty(),
                            russian = sentence.stringOrNull("ru").orEmpty(),
                            rowHash = sentence.stringOrNull("row_hash"),
                            editMetaJson = null,
                            sourceMetaJson = sentence.rawJsonOrNull("meta_json"),
                            createdAt = sentence.stringOrNull("created_at") ?: createdAt,
                            updatedAt = updatedAt,
                        )
                    }
                    val rowEntities = rowEntitiesRaw.sortedBy { it.orderIndex }
                        .mapIndexed { index, row -> row.copy(orderIndex = index) }
                    val rowEntitiesById = rowEntities.associateBy { it.rowId }
                    var itemMissingAudioLinkCount = 0

                    dao.insertRows(rowEntities)

                    itemMissingAudioLinkCount += importMissingAudioLink(
                        ownerType = "text",
                        ownerId = textId,
                        assetKey = textObject.stringOrNull("audio_asset_key"),
                        ttsProfileJson = textObject.rawJsonOrNull("audio_tts_profile_json")
                            ?: ttsProfileJson,
                        createdAt = createdAt,
                    )

                    sentences.zip(rowEntitiesRaw).forEach { (sentenceElement, originalRowEntity) ->
                        val sentence = sentenceElement.jsonObject
                        val rowEntity = rowEntitiesById.getValue(originalRowEntity.rowId)
                        itemMissingAudioLinkCount += importMissingAudioLink(
                            ownerType = "row",
                            ownerId = rowEntity.rowId,
                            assetKey = sentence.stringOrNull("audio_asset_key"),
                            ttsProfileJson = sentence.rawJsonOrNull("audio_tts_profile_json")
                                ?: ttsProfileJson,
                            createdAt = rowEntity.createdAt,
                        )
                    }

                    rowCount += rowEntities.size
                    missingAudioLinkCount += itemMissingAudioLinkCount
                    importedCount++
                } catch (error: Exception) {
                    insertedTextId?.let { textId ->
                        runCatching { dao.deleteText(textId) }
                    }
                    errorCount++
                    if (errors.size < 50) {
                        errors += "$titleForError: ${error.message.orEmpty()}"
                    }
                }
            }

            LegacyWebLibraryImportResult(
                mode = mode,
                importedCount = importedCount,
                skippedCount = skippedCount,
                errorCount = errorCount,
                rowCount = rowCount,
                missingAudioLinkCount = missingAudioLinkCount,
                errors = errors.take(50),
            )
        }

    suspend fun linkDefaultRowAudio(rowId: String, input: AudioAssetInput) {
        database.withTransaction {
            dao.insertAudioAsset(input.toEntity(createdAt = clock()))
            dao.clearDefaultRowAudio(rowId)
            dao.insertRowAudio(
                RowAudioEntity(
                    rowId = rowId,
                    assetKey = input.assetKey,
                    isDefault = true,
                    isStale = false,
                    staleReason = null,
                    createdAt = clock(),
                ),
            )
        }
    }

    suspend fun getDefaultRowAudio(rowId: String): RowAudioEntity? = dao.getDefaultRowAudio(rowId)

    private suspend fun requireText(textId: String): LibraryText {
        val text = dao.getText(textId) ?: error("Library text not found: $textId")
        val rows = dao.getRows(textId)
        val notes = dao.getNotesForText(textId).associateBy { it.sentenceId }
        val rowDomains = rows.map { row ->
            row.toDomain(
                defaultAudioAssetKey = dao.getDefaultRowAudio(row.rowId)?.assetKey,
                note = notes[row.rowId]?.note,
            )
        }
        return text.toDomain(rowDomains)
    }

    private suspend fun requireRow(textId: String, rowId: String): LibraryRowEntity =
        dao.getRow(textId, rowId) ?: error("Library row not found: $rowId")

    private suspend fun rewriteRowsInOrder(rows: List<LibraryRowEntity>) {
        val offsetRows = rows.mapIndexed { index, row -> row.copy(orderIndex = 10_000 + index) }
        dao.updateRows(offsetRows)
        dao.updateRows(offsetRows.mapIndexed { index, row -> row.copy(orderIndex = index, updatedAt = clock()) })
    }

    private fun SaveGeneratedTextRequest.toEntity(
        textId: String,
        textKey: String,
        now: String,
        createdAt: String = now,
    ): LibraryTextEntity =
        LibraryTextEntity(
            textId = textId,
            textKey = textKey,
            title = title,
            level = level,
            tagsJson = json.encodeToString(normalizeTags(tags)),
            sourceLabel = sourceLabel?.trim()?.takeIf { it.isNotEmpty() },
            topic = topic?.trim()?.takeIf { it.isNotEmpty() },
            sourceText = sourceText,
            sourceMetaJson = sourceMeta?.let { json.encodeToString(it) },
            tableModelMetaJson = tableModelMeta?.let { json.encodeToString(it) },
            ttsProfileJson = ttsProfile?.let { json.encodeToString(it) },
            isArchived = false,
            createdAt = createdAt,
            updatedAt = now,
            lastOpenedAt = null,
            schemaVersion = 1,
        )

    private fun List<GeneratedLibraryRowInput>.toEntities(
        textId: String,
        now: String,
        existingRows: List<LibraryRowEntity>,
    ): List<LibraryRowEntity> =
        mapIndexed { index, row ->
            val existing = existingRows.getOrNull(index)
            LibraryRowEntity(
                rowId = existing?.rowId ?: idFactory(),
                textId = textId,
                orderIndex = index,
                hebrewPlain = row.hebrewPlain,
                hebrewNiqqud = row.hebrewNiqqud,
                translit = row.translit,
                translitRu = row.translitRu,
                russian = row.russian,
                rowHash = row.rowHash ?: computeRowHash(row),
                editMetaJson = existing?.editMetaJson,
                sourceMetaJson = null,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        }

    private fun LibraryTextEntity.toDomain(rows: List<LibraryRow>): LibraryText =
        LibraryText(
            id = textId,
            textKey = textKey,
            title = title,
            level = level,
            tags = decodeTags(tagsJson),
            sourceLabel = sourceLabel,
            topic = topic,
            sourceText = sourceText,
            sourceMeta = sourceMetaJson?.decodeOrNull(),
            ttsProfile = ttsProfileJson?.decodeOrNull(),
            tableModelMeta = tableModelMetaJson?.decodeOrNull(),
            rows = rows,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastOpenedAt = lastOpenedAt,
            schemaVersion = schemaVersion,
        )

    private fun LibraryRowEntity.toDomain(defaultAudioAssetKey: String?, note: String? = null): LibraryRow =
        LibraryRow(
            id = rowId,
            textId = textId,
            orderIndex = orderIndex,
            hebrewPlain = hebrewPlain,
            hebrewNiqqud = hebrewNiqqud,
            translit = translit,
            translitRu = translitRu,
            russian = russian,
            rowHash = rowHash,
            editMeta = decodeEditMeta(editMetaJson),
            audioAssetKey = defaultAudioAssetKey,
            note = note,
        )

    private fun AudioAssetInput.toEntity(createdAt: String): AudioAssetEntity =
        AudioAssetEntity(
            assetKey = assetKey,
            fileName = fileName,
            relativePath = relativePath,
            mimeType = mimeType,
            providerId = providerId,
            voiceName = voiceName,
            language = language,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            contentHash = contentHash,
            createdAt = createdAt,
            provenanceJson = provenanceJson,
            isMissing = isMissing,
        )

    private fun decodeEditMeta(editMetaJson: String?): EditMeta =
        editMetaJson?.decodeOrNull() ?: EditMeta()

    private inline fun <reified T> String.decodeOrNull(): T? =
        runCatching { json.decodeFromString<T>(this) }.getOrNull()

    private fun decodeTags(tagsJson: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(tagsJson) }.getOrElse { emptyList() }

    private suspend fun importMissingAudioLink(
        ownerType: String,
        ownerId: String,
        assetKey: String?,
        ttsProfileJson: String?,
        createdAt: String,
    ): Int {
        val key = assetKey?.trim()?.takeIf { it.isNotEmpty() } ?: return 0
        if (dao.getAudioAsset(key) == null) {
            val profile = ttsProfileJson?.parseJsonObjectOrNull()
            dao.insertAudioAsset(
                AudioAssetEntity(
                    assetKey = key,
                    fileName = safeAudioFileName(key),
                    relativePath = "audio/missing/${sha256(key).take(24)}.missing",
                    mimeType = "application/octet-stream",
                    providerId = profile?.stringOrNull("providerId")
                        ?: profile?.stringOrNull("provider")
                        ?: "legacy_web_import",
                    voiceName = profile?.stringOrNull("voiceName"),
                    language = profile?.stringOrNull("language") ?: "he-IL",
                    durationMs = null,
                    sizeBytes = null,
                    contentHash = null,
                    createdAt = createdAt,
                    provenanceJson = buildJsonObject {
                        put("origin", "legacy_web_json_import")
                        put("asset_key", key)
                        if (ttsProfileJson != null) put("tts_profile", ttsProfileJson)
                    }.toString(),
                    isMissing = true,
                ),
            )
        }
        when (ownerType) {
            "text" -> dao.insertTextAudio(
                TextAudioEntity(
                    textId = ownerId,
                    assetKey = key,
                    isDefault = true,
                    isStale = true,
                    staleReason = "legacy_web_json_missing_audio_file",
                    createdAt = createdAt,
                ),
            )
            "row" -> dao.insertRowAudio(
                RowAudioEntity(
                    rowId = ownerId,
                    assetKey = key,
                    isDefault = true,
                    isStale = true,
                    staleReason = "legacy_web_json_missing_audio_file",
                    createdAt = createdAt,
                ),
            )
        }
        return 1
    }

    private fun String.parseJsonObjectOrNull(): JsonObject? =
        runCatching { json.decodeFromString<JsonObject>(this) }.getOrNull()

    private fun JsonObject.stringOrNull(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.intOrNull(name: String): Int? =
        this[name]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

    private fun JsonObject.boolCompat(name: String): Boolean {
        val primitive = this[name]?.jsonPrimitive ?: return false
        return primitive.booleanOrNull ?: (primitive.contentOrNull == "1")
    }

    private fun JsonObject.rawJsonOrNull(vararg names: String): String? {
        for (name in names) {
            val value = this[name] ?: continue
            if (value is JsonNull) continue
            return if (value is JsonPrimitive && value.isString) {
                value.contentOrNull?.takeIf { it.isNotBlank() }
            } else {
                value.toString()
            }
        }
        return null
    }

    private fun JsonObject.tagsFromLegacy(): List<String> {
        val tags = this["tags"]
        if (tags != null && tags !is JsonNull) {
            runCatching {
                return tags.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
            }
        }
        val tagsJson = stringOrNull("tags_json") ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(tagsJson) }.getOrElse { emptyList() }
    }

    private fun computeLegacyWebTextKey(
        sourceText: String,
        ttsProfileJson: String?,
        tableModelMetaJson: String?,
    ): String {
        val payload = buildJsonObject {
            put("v", 1)
            put("sourceText", sourceText.replace("\r\n", "\n").replace("\r", "\n").trim())
            put("ttsProfile", ttsProfileJson?.let { json.decodeFromString<JsonElement>(it) } ?: JsonNull)
            put("tableModelMeta", tableModelMetaJson?.let { json.decodeFromString<JsonElement>(it) } ?: JsonNull)
        }
        return sha256(payload.toString())
    }

    private fun appendImportSalt(tableModelMetaJson: String?): String =
        buildJsonObject {
            val original = tableModelMetaJson?.parseJsonObjectOrNull()
            if (original != null) {
                original.forEach { (key, value) -> put(key, value) }
            }
            put("importSalt", idFactory())
        }.toString()

    private fun safeAudioFileName(assetKey: String): String =
        assetKey.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "legacy-audio" }.take(96)

    private fun guessTitle(sourceText: String): String =
        sourceText.replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.take(80)
            ?: "Untitled"

    private fun LibraryRowEntity.valueFor(field: RowField): String =
        when (field) {
            RowField.HebrewPlain -> hebrewPlain
            RowField.HebrewNiqqud -> hebrewNiqqud
            RowField.Translit -> translit
            RowField.TranslitRu -> translitRu
            RowField.Russian -> russian
        }

    private fun LibraryRowEntity.withValue(field: RowField, value: String): LibraryRowEntity =
        when (field) {
            RowField.HebrewPlain -> copy(hebrewPlain = value)
            RowField.HebrewNiqqud -> copy(hebrewNiqqud = value)
            RowField.Translit -> copy(translit = value)
            RowField.TranslitRu -> copy(translitRu = value)
            RowField.Russian -> copy(russian = value)
        }

    private val RowField.key: String
        get() = when (this) {
            RowField.HebrewPlain -> "hebrew_plain"
            RowField.HebrewNiqqud -> "hebrew_niqqud"
            RowField.Translit -> "translit"
            RowField.TranslitRu -> "translit_ru"
            RowField.Russian -> "russian"
        }

    private fun computeTextKey(input: SaveGeneratedTextRequest): String =
        sha256(
            listOf(
                input.sourceText,
                input.tableModelMeta?.provider?.wireId.orEmpty(),
                input.tableModelMeta?.actualProvider?.wireId.orEmpty(),
                input.ttsProfile?.providerId?.wireId.orEmpty(),
                normalizeTags(input.tags).joinToString(","),
            ).joinToString("|"),
        )

    private fun computeRowHash(row: GeneratedLibraryRowInput): String =
        sha256(
            listOf(
                row.hebrewPlain,
                row.hebrewNiqqud,
                row.translit,
                row.translitRu,
                row.russian,
            ).joinToString("|"),
        )

    private fun normalizeTags(tags: List<String>): List<String> =
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
