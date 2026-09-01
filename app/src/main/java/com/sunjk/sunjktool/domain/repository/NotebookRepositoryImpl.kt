package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.NotebookDao
import com.sunjk.sunjktool.data.model.NotebookEntity
import com.sunjk.sunjktool.domain.model.Notebook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NotebookRepositoryImpl(
    private val notebookDao: NotebookDao,
    private val logEntryDao: LogEntryDao
) : NotebookRepository {

    override fun getAll(): Flow<List<Notebook>> = flow {
        notebookDao.getAll().collect { entities ->
            emit(entities.map { it.toDomainWithCounts() })
        }
    }

    override fun getById(id: Long): Flow<Notebook?> =
        notebookDao.getById(id).map { it?.toDomain() }

    override fun getRoots(): Flow<List<Notebook>> = flow {
        notebookDao.getRoots().collect { entities ->
            emit(entities.map { it.toDomainWithCounts() })
        }
    }

    override fun getPinned(): Flow<List<Notebook>> = flow {
        notebookDao.getPinned().collect { entities ->
            emit(entities.map { it.toDomain() })
        }
    }

    override fun getChildren(parentId: Long): Flow<List<Notebook>> = flow {
        notebookDao.getByParentId(parentId).collect { entities ->
            emit(entities.map { it.toDomainWithCounts() })
        }
    }

    private suspend fun NotebookEntity.toDomainWithCounts(): Notebook {
        val subCount = notebookDao.getSubNotebookCount(id).firstOrNull() ?: 0
        val entCount = notebookDao.getEntryCount(id).firstOrNull() ?: 0
        // 递归统计所有后代笔记本及其记录数
        val descendantIds = getDescendantIds(id)
        val descendantEntryCount = if (descendantIds.isEmpty()) 0
            else logEntryDao.countByNotebookIds(descendantIds.toList())
        return Notebook(
            id = id,
            name = name,
            parentId = parentId,
            sortOrder = sortOrder,
            icon = icon,
            pinned = pinned,
            subNotebookCount = subCount,
            entryCount = entCount,
            totalSubNotebookCount = descendantIds.size,
            totalEntryCount = entCount + descendantEntryCount,
            createdDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()),
            updatedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault())
        )
    }

    override suspend fun save(notebook: Notebook): Long {
        val now = LocalDateTime.now()
        val entity = notebook.toEntity(now)
        return if (notebook.id == 0L) {
            notebookDao.insert(entity)
        } else {
            notebookDao.update(entity)
            notebook.id
        }
    }

    override suspend fun delete(id: Long) {
        val notebook = notebookDao.getById(id).first()?.toDomain()
        val deletedParentId = notebook?.parentId
        // Reparent children to the deleted notebook's parent
        notebookDao.reparentChildren(id, deletedParentId)
        // Unlink log entries
        logEntryDao.clearNotebookId(id)
        // Delete the notebook
        notebookDao.deleteById(id)
    }

    override suspend fun getBreadcrumbs(notebookId: Long): List<Pair<Long, String>> {
        val breadcrumbs = mutableListOf<Pair<Long, String>>()
        var currentId: Long? = notebookId
        while (currentId != null) {
            val entity = notebookDao.getById(currentId).first()
            if (entity != null) {
                breadcrumbs.add(0, entity.id to entity.name)
                currentId = entity.parentId
            } else {
                currentId = null
            }
        }
        return breadcrumbs
    }

    override suspend fun getDescendantIds(notebookId: Long): Set<Long> {
        val result = mutableSetOf<Long>()
        suspend fun collectChildren(parentId: Long) {
            val children = notebookDao.getByParentId(parentId).first()
            for (child in children) {
                result.add(child.id)
                collectChildren(child.id)
            }
        }
        collectChildren(notebookId)
        return result
    }

    private fun NotebookEntity.toDomain() = Notebook(
        id = id,
        name = name,
        parentId = parentId,
        sortOrder = sortOrder,
        icon = icon,
        pinned = pinned,
        createdDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()),
        updatedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault())
    )

    private fun Notebook.toEntity(now: LocalDateTime) = NotebookEntity(
        id = id,
        name = name,
        parentId = parentId,
        sortOrder = sortOrder,
        icon = icon,
        pinned = pinned,
        createdDate = if (id == 0L) now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                      else createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedDate = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}
