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

    override suspend fun updateSummary(id: Long, summary: String, now: Long) {
        dao.updateSummary(id, summary, now)
    }

    override suspend fun updateSelfCheckContent(id: Long, content: String, now: Long) {
        dao.updateSelfCheckContent(id, content, now)
    }


    override fun getEntriesByNotebookId(notebookId: Long): Flow<List<LogEntry>> =
        dao.getEntriesByNotebookId(notebookId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun clearNotebookId(notebookId: Long) {
        dao.clearNotebookId(notebookId)
    }

    override fun countUnfiled(): Flow<Int> = dao.countUnfiled()

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
        imagePaths = LogEntry.decodePaths(imagePath),
        description = description,
        aiSummary = aiSummary,
        selfCheckContent = selfCheckContent,
        mindMapJson = mindMapJson,
        attachmentPaths = LogEntry.decodePaths(attachmentPaths),
        attachmentText = attachmentText,
        notebookId = notebookId,
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
        imagePath = LogEntry.encodePaths(imagePaths),
        description = description,
        aiSummary = aiSummary,
        selfCheckContent = selfCheckContent,
        mindMapJson = mindMapJson,
        attachmentPaths = LogEntry.encodePaths(attachmentPaths),
        attachmentText = attachmentText,
        notebookId = notebookId,
        createdDate = createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedDate = updatedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}
