package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.model.LogEntryEntity
import com.sunjk.sunjktool.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LogRepositoryImpl(
    private val dao: LogEntryDao
) : LogRepository {

    override fun getAllEntries(): Flow<List<LogEntry>> =
        dao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getEntryById(id: Long): Flow<LogEntry?> =
        dao.getEntryById(id).map { it?.toDomain() }

    override suspend fun saveEntry(entry: LogEntry): Long {
        val entity = entry.toEntity()
        return if (entry.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entry.id
        }
    }

    override suspend fun deleteEntry(id: Long) {
        dao.delete(
            LogEntryEntity(
                id = id, title = "",
                createdDate = 0L, updatedDate = 0L
            )
        )
    }

    private fun LogEntryEntity.toDomain() = LogEntry(
        id = id,
        subject = subject,
        title = title,
        timeSpent = timeSpent,
        imagePath = imagePath,
        createdDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()
        ),
        updatedDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault()
        )
    )

    private fun LogEntry.toEntity() = LogEntryEntity(
        id = id,
        subject = subject,
        title = title,
        timeSpent = timeSpent,
        imagePath = imagePath,
        createdDate = createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedDate = updatedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}
