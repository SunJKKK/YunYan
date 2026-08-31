package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.Notebook
import kotlinx.coroutines.flow.Flow

interface NotebookRepository {
    fun getAll(): Flow<List<Notebook>>
    fun getById(id: Long): Flow<Notebook?>
    fun getRoots(): Flow<List<Notebook>>
    fun getPinned(): Flow<List<Notebook>>
    fun getChildren(parentId: Long): Flow<List<Notebook>>
    suspend fun save(notebook: Notebook): Long
    suspend fun delete(id: Long)
    suspend fun getBreadcrumbs(notebookId: Long): List<Pair<Long, String>>
    suspend fun getDescendantIds(notebookId: Long): Set<Long>
}
