package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.TickTickProjectEntity
import com.sunjk.sunjktool.data.model.TickTickTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TickTickProjectDao {
    @Query("SELECT * FROM ticktick_projects ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<TickTickProjectEntity>>

    @Query("SELECT * FROM ticktick_projects")
    suspend fun getAllOnce(): List<TickTickProjectEntity>

    @Query("DELETE FROM ticktick_projects")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<TickTickProjectEntity>)
}

@Dao
interface TickTickTaskDao {
    @Query("SELECT * FROM ticktick_tasks ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<TickTickTaskEntity>>

    @Query("SELECT * FROM ticktick_tasks WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun getByProject(projectId: String): Flow<List<TickTickTaskEntity>>

    @Query("SELECT * FROM ticktick_tasks WHERE dueDate = :date ORDER BY sortOrder ASC")
    fun getByDueDate(date: String): Flow<List<TickTickTaskEntity>>

    @Query("SELECT * FROM ticktick_tasks")
    suspend fun getAllOnce(): List<TickTickTaskEntity>

    @Query("DELETE FROM ticktick_tasks")
    suspend fun clear()

    @Query("DELETE FROM ticktick_tasks WHERE projectId = :projectId")
    suspend fun clearByProject(projectId: String)

    @Query("DELETE FROM ticktick_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TickTickTaskEntity>)
}
