package com.sindromradiospb.ttsprototypev2.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class LibraryTextSummaryRow(
    val textId: String,
    val textKey: String,
    val title: String,
    val level: String?,
    val updatedAt: String,
    val isArchived: Boolean,
)

@Dao
interface LibraryDao {
    @Query(
        """
        SELECT text_id AS textId, text_key AS textKey, title, level, updated_at AS updatedAt,
               is_archived AS isArchived
        FROM library_texts
        WHERE (:includeArchived = 1 OR is_archived = 0)
        ORDER BY updated_at DESC
        """,
    )
    fun observeTextSummaries(includeArchived: Boolean): Flow<List<LibraryTextSummaryRow>>

    @Query("SELECT * FROM library_texts WHERE text_id = :textId")
    suspend fun getText(textId: String): LibraryTextEntity?

    @Query("SELECT * FROM library_texts WHERE text_key = :textKey")
    suspend fun getTextByKey(textKey: String): LibraryTextEntity?

    @Query("SELECT * FROM library_rows WHERE text_id = :textId ORDER BY order_index ASC")
    suspend fun getRows(textId: String): List<LibraryRowEntity>

    @Query("SELECT * FROM library_rows WHERE row_id = :rowId AND text_id = :textId")
    suspend fun getRow(textId: String, rowId: String): LibraryRowEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertText(text: LibraryTextEntity)

    @Update
    suspend fun updateText(text: LibraryTextEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRows(rows: List<LibraryRowEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRow(row: LibraryRowEntity)

    @Update
    suspend fun updateRows(rows: List<LibraryRowEntity>)

    @Update
    suspend fun updateRow(row: LibraryRowEntity)

    @Delete
    suspend fun deleteRow(row: LibraryRowEntity)

    @Query("DELETE FROM library_rows WHERE text_id = :textId")
    suspend fun deleteRowsForText(textId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAudioAsset(asset: AudioAssetEntity)

    @Query("SELECT * FROM audio_assets WHERE asset_key = :assetKey")
    suspend fun getAudioAsset(assetKey: String): AudioAssetEntity?

    @Query("UPDATE audio_assets SET is_missing = 1 WHERE asset_key = :assetKey")
    suspend fun markAudioAssetMissing(assetKey: String)

    @Query("UPDATE row_audio SET is_default = 0 WHERE row_id = :rowId")
    suspend fun clearDefaultRowAudio(rowId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRowAudio(rowAudio: RowAudioEntity)

    @Query(
        """
        UPDATE row_audio
        SET is_stale = 1, stale_reason = :reason
        WHERE row_id = :rowId AND is_default = 1
        """,
    )
    suspend fun markDefaultRowAudioStale(rowId: String, reason: String)

    @Query("SELECT * FROM row_audio WHERE row_id = :rowId AND is_default = 1")
    suspend fun getDefaultRowAudio(rowId: String): RowAudioEntity?

    @Query("SELECT * FROM library_texts ORDER BY updated_at DESC, text_id ASC")
    suspend fun getTextsForExport(): List<LibraryTextEntity>

    @Query("SELECT * FROM library_rows ORDER BY text_id ASC, order_index ASC")
    suspend fun getRowsForExport(): List<LibraryRowEntity>

    @Query("SELECT * FROM audio_assets ORDER BY asset_key ASC")
    suspend fun getAudioAssetsForExport(): List<AudioAssetEntity>

    @Query("SELECT * FROM row_audio WHERE is_default = 1 ORDER BY row_id ASC, asset_key ASC")
    suspend fun getDefaultRowAudioForExport(): List<RowAudioEntity>

    @Query("SELECT * FROM text_audio WHERE is_default = 1 ORDER BY text_id ASC, asset_key ASC")
    suspend fun getDefaultTextAudioForExport(): List<TextAudioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportHistory(exportHistory: ExportHistoryEntity)

    @Query("SELECT * FROM export_history ORDER BY created_at DESC")
    suspend fun getExportHistory(): List<ExportHistoryEntity>
}
