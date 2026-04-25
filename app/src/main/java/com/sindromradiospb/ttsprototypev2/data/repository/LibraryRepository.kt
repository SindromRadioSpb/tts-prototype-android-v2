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
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class LibraryTextSummary(
    val textId: String,
    val textKey: String,
    val title: String,
    val level: String?,
    val updatedAt: String,
    val isArchived: Boolean,
)

data class SaveGeneratedTextRequest(
    val title: String,
    val level: String? = null,
    val tags: List<String> = emptyList(),
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

class RoomLibraryRepository(
    private val database: AppDatabase,
    private val clock: () -> String = { Instant.now().toString() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
) {
    private val dao = database.libraryDao()

    fun observeTexts(includeArchived: Boolean): Flow<List<LibraryTextSummary>> =
        dao.observeTextSummaries(includeArchived).map { rows ->
            rows.map {
                LibraryTextSummary(
                    textId = it.textId,
                    textKey = it.textKey,
                    title = it.title,
                    level = it.level,
                    updatedAt = it.updatedAt,
                    isArchived = it.isArchived,
                )
            }
        }

    suspend fun getText(textId: String): LibraryText =
        database.withTransaction { requireText(textId) }

    suspend fun saveGeneratedText(input: SaveGeneratedTextRequest): SaveTextResult =
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

    suspend fun updateGeneratedText(textId: String, input: SaveGeneratedTextRequest): SaveTextResult =
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

    suspend fun markOpened(textId: String, openedAt: String) {
        database.withTransaction {
            val current = dao.getText(textId) ?: error("Library text not found: $textId")
            dao.updateText(current.copy(lastOpenedAt = openedAt))
        }
    }

    suspend fun linkDefaultRowAudio(rowId: String, input: AudioAssetInput) {
        database.withTransaction {
            dao.insertAudioAsset(input.toEntity(createdAt = clock()))
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
        val rowDomains = rows.map { row ->
            row.toDomain(defaultAudioAssetKey = dao.getDefaultRowAudio(row.rowId)?.assetKey)
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
            tags = json.decodeFromString(tagsJson),
            sourceText = sourceText,
            sourceMeta = sourceMetaJson?.let { json.decodeFromString(it) },
            ttsProfile = ttsProfileJson?.let { json.decodeFromString(it) },
            tableModelMeta = tableModelMetaJson?.let { json.decodeFromString(it) },
            rows = rows,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastOpenedAt = lastOpenedAt,
            schemaVersion = schemaVersion,
        )

    private fun LibraryRowEntity.toDomain(defaultAudioAssetKey: String?): LibraryRow =
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
        editMetaJson?.let { json.decodeFromString(it) } ?: EditMeta()

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
