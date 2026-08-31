package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.FlashcardSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardSessionDao {
    @Query("SELECT * FROM flashcard_sessions ORDER BY createdDate DESC")
    fun getAll(): Flow<List<FlashcardSessionEntity>>

    @Query("SELECT * FROM flashcard_sessions WHERE logEntryId = :logEntryId ORDER BY createdDate DESC LIMIT 1")
    fun getLatestForEntry(logEntryId: Long): Flow<FlashcardSessionEntity?>

    @Query("SELECT * FROM flashcard_sessions WHERE id = :sessionId")
    fun getById(sessionId: Long): Flow<FlashcardSessionEntity?>

    @Query("SELECT * FROM flashcard_sessions WHERE id = :sessionId")
    suspend fun getByIdOnce(sessionId: Long): FlashcardSessionEntity?

    @Query("SELECT * FROM flashcard_sessions WHERE logEntryId = :logEntryId ORDER BY createdDate DESC")
    fun getAllForEntry(logEntryId: Long): Flow<List<FlashcardSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FlashcardSessionEntity): Long

    @Query("UPDATE flashcard_sessions SET answersJson = :answersJson WHERE id = :sessionId")
    suspend fun updateAnswers(sessionId: Long, answersJson: String)

    @Query("DELETE FROM flashcard_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Query("DELETE FROM flashcard_sessions WHERE logEntryId = :logEntryId")
    suspend fun deleteAllForEntry(logEntryId: Long)
}
