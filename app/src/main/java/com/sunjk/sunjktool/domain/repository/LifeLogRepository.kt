package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.LifeLogEntry
import kotlinx.coroutines.flow.Flow

interface LifeLogRepository {
    fun getAllEntries(): Flow<List<LifeLogEntry>>
    fun getEntryById(id: Long): Flow<LifeLogEntry?>
    suspend fun saveEntry(entry: LifeLogEntry): Long
    suspend fun deleteEntry(id: Long)
}
