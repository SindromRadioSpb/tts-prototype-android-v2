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
}
