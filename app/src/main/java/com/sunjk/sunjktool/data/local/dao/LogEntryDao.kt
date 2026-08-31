package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.LogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {

    @Query("SELECT * FROM log_entries ORDER BY createdDate DESC")
    fun getAllEntries(): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<LogEntryEntity?>

    @Query("SELECT * FROM log_entries WHERE createdDate >= :sinceMillis ORDER BY createdDate DESC")
    fun getEntriesSince(sinceMillis: Long): Flow<List<LogEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntryEntity): Long

    @Update
    suspend fun update(entry: LogEntryEntity)

    @Delete
    suspend fun delete(entry: LogEntryEntity)

    // 更新内容时同时刷新 updatedDate，保证增量同步（按 updatedDate > 游标 筛选）能重新上传
    // 同一 json 文件（含 description/aiSummary/selfCheckContent 等），避免云端遗留旧内容。
    @Query("UPDATE log_entries SET aiSummary = :summary, updatedDate = :now WHERE id = :id")
    suspend fun updateSummary(id: Long, summary: String, now: Long)

    @Query("UPDATE log_entries SET selfCheckContent = :content, updatedDate = :now WHERE id = :id")
    suspend fun updateSelfCheckContent(id: Long, content: String, now: Long)

    @Query("UPDATE log_entries SET mindMapJson = :json, updatedDate = :now WHERE id = :id")
    suspend fun updateMindMapJson(id: Long, json: String, now: Long)

    @Query("SELECT * FROM log_entries WHERE notebookId = :notebookId ORDER BY createdDate DESC")
    fun getEntriesByNotebookId(notebookId: Long): Flow<List<LogEntryEntity>>

    @Query("UPDATE log_entries SET notebookId = NULL WHERE notebookId = :notebookId")
    suspend fun clearNotebookId(notebookId: Long)

    @Query("SELECT COUNT(*) FROM log_entries WHERE notebookId IS NULL")
    fun countUnfiled(): Flow<Int>
}
