package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getAllEntries(): Flow<List<LogEntry>>
    fun getEntryById(id: Long): Flow<LogEntry?>
    fun getEntriesByNotebookId(notebookId: Long): Flow<List<LogEntry>>
    suspend fun saveEntry(entry: LogEntry): Long
    suspend fun deleteEntry(id: Long)
    suspend fun updateSummary(id: Long, summary: String, now: Long)
    suspend fun updateSelfCheckContent(id: Long, content: String, now: Long)
    suspend fun clearNotebookId(notebookId: Long)
    fun countUnfiled(): Flow<Int>
}
