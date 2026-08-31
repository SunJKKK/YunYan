package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.ReviewNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewNoteDao {
    @Query("SELECT * FROM review_notes WHERE logEntryId = :logEntryId ORDER BY createdDate DESC")
    fun getAllByLogEntryId(logEntryId: Long): Flow<List<ReviewNoteEntity>>

    @Query("SELECT * FROM review_notes ORDER BY createdDate DESC")
    fun getAll(): Flow<List<ReviewNoteEntity>>

    @Query("SELECT * FROM review_notes WHERE id = :id")
    fun getById(id: Long): Flow<ReviewNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReviewNoteEntity): Long

    @Query("DELETE FROM review_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM review_notes WHERE logEntryId = :logEntryId")
    suspend fun deleteAllForEntry(logEntryId: Long)

    @Query("SELECT COUNT(*) FROM review_notes")
    suspend fun count(): Int
}
