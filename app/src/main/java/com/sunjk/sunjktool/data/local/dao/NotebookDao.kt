package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.NotebookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {

    @Query("SELECT * FROM notebooks ORDER BY sortOrder ASC, createdDate ASC")
    fun getAll(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    fun getById(id: Long): Flow<NotebookEntity?>

    @Query("SELECT * FROM notebooks WHERE parentId = :parentId ORDER BY sortOrder ASC, createdDate ASC")
    fun getByParentId(parentId: Long): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE parentId IS NULL ORDER BY sortOrder ASC, createdDate ASC")
    fun getRoots(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE pinned = 1 ORDER BY sortOrder ASC, createdDate ASC")
    fun getPinned(): Flow<List<NotebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notebook: NotebookEntity): Long

    @Update
    suspend fun update(notebook: NotebookEntity)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM notebooks WHERE parentId = :parentId")
    fun getSubNotebookCount(parentId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM log_entries WHERE notebookId = :notebookId")
    fun getEntryCount(notebookId: Long): Flow<Int>

    @Query("UPDATE notebooks SET parentId = :newParentId WHERE parentId = :oldParentId")
    suspend fun reparentChildren(oldParentId: Long, newParentId: Long?)
}
