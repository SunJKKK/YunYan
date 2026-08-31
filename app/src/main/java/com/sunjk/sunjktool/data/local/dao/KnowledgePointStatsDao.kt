package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.KnowledgePointStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgePointStatsDao {

    @Query("SELECT * FROM knowledge_point_stats WHERE logEntryId = :logEntryId ORDER BY totalQuestions DESC")
    fun getByLogEntryId(logEntryId: Long): Flow<List<KnowledgePointStatsEntity>>

    @Query("SELECT * FROM knowledge_point_stats WHERE logEntryId = :logEntryId AND knowledgePoint = :kp LIMIT 1")
    suspend fun getByLogEntryAndKp(logEntryId: Long, kp: String): KnowledgePointStatsEntity?

    @Query("SELECT * FROM knowledge_point_stats")
    fun getAll(): Flow<List<KnowledgePointStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KnowledgePointStatsEntity): Long

    @Update
    suspend fun update(entity: KnowledgePointStatsEntity)

    @Query("UPDATE knowledge_point_stats SET weaknessSummary = :summary, updatedDate = :updatedDate WHERE id = :id")
    suspend fun updateWeaknessSummary(id: Long, summary: String, updatedDate: Long)
}
