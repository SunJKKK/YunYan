package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.NotebookDao
import com.sunjk.sunjktool.data.model.LogEntryEntity
import com.sunjk.sunjktool.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LogRepositoryImpl(
    private val dao: LogEntryDao,
    private val notebookDao: NotebookDao
) : LogRepository {

    // 学习记录的展示分类改为其所在笔记本（最低级目录）名称，subject 字段仅保留历史数据
    override fun getAllEntries(): Flow<List<LogEntry>> =
        combine(dao.getAllEntries(), notebookDao.getAll()) { entities, notebooks ->
            val nameById = notebooks.associate { it.id to it.name }
            entities.map { it.toDomain(nameById) }
        }

    override fun getEntryById(id: Long): Flow<LogEntry?> =
        combine(dao.getEntryById(id), notebookDao.getAll()) { entity, notebooks ->
            entity?.let { it.toDomain(notebooks.associate { n -> n.id to n.name }) }
        }

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
        combine(dao.getEntriesByNotebookId(notebookId), notebookDao.getAll()) { entities, notebooks ->
            val nameById = notebooks.associate { it.id to it.name }
            entities.map { it.toDomain(nameById) }
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

    private fun LogEntryEntity.toDomain(nameById: Map<Long, String> = emptyMap()) = LogEntry(
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
        notebookName = notebookId?.let { nameById[it] } ?: "",
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
