package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getAllEntries(): Flow<List<LogEntry>>
    fun getEntryById(id: Long): Flow<LogEntry?>
    suspend fun saveEntry(entry: LogEntry): Long
    suspend fun deleteEntry(id: Long)
}
