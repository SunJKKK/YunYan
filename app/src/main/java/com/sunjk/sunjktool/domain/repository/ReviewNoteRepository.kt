package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.ReviewNote
import kotlinx.coroutines.flow.Flow

interface ReviewNoteRepository {
    fun getByLogEntryId(logEntryId: Long): Flow<List<ReviewNote>>
    fun getById(id: Long): Flow<ReviewNote?>
    fun getAll(): Flow<List<ReviewNote>>
    suspend fun save(note: ReviewNote): Long
    suspend fun delete(id: Long)
    suspend fun deleteAllForEntry(logEntryId: Long)
    suspend fun count(): Int
}
