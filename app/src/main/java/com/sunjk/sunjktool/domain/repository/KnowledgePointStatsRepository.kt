package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.KnowledgePointStatsDao
import com.sunjk.sunjktool.data.model.KnowledgePointStatsEntity
import com.sunjk.sunjktool.data.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import java.time.Instant

enum class Trend { IMPROVING, STABLE, DECLINING }

data class KnowledgePointStats(
    val id: Long = 0,
    val logEntryId: Long,
    val knowledgePoint: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val mastery: Float,
    val trend: Trend,
)

class KnowledgePointStatsRepository(private val dao: KnowledgePointStatsDao) {

    fun getByLogEntryId(logEntryId: Long): Flow<List<KnowledgePointStatsEntity>> =
        dao.getByLogEntryId(logEntryId)

    fun getAll(): Flow<List<KnowledgePointStatsEntity>> = dao.getAll()

    suspend fun recordAnswer(logEntryId: Long, knowledgePoint: String, isCorrect: Boolean) {
        if (knowledgePoint.isBlank()) return
        val existing = dao.getByLogEntryAndKp(logEntryId, knowledgePoint)
        val now = Instant.now().toEpochMilli()
        if (existing != null) {
            dao.update(existing.copy(
                totalQuestions = existing.totalQuestions + 1,
                correctAnswers = existing.correctAnswers + (if (isCorrect) 1 else 0),
                updatedDate = now
            ))
        } else {
            dao.insert(KnowledgePointStatsEntity(
                logEntryId = logEntryId,
                knowledgePoint = knowledgePoint,
                totalQuestions = 1,
                correctAnswers = if (isCorrect) 1 else 0,
                updatedDate = now
            ))
        }
        SyncTrigger.bumpEntity("knowledge_point_stats")
        SyncTrigger.requestAutoSync()
    }


    companion object {
        fun computeStats(entity: KnowledgePointStatsEntity): KnowledgePointStats {
            val mastery = if (entity.totalQuestions > 0) entity.correctAnswers.toFloat() / entity.totalQuestions else 0f
            return KnowledgePointStats(
                id = entity.id,
                logEntryId = entity.logEntryId,
                knowledgePoint = entity.knowledgePoint,
                totalQuestions = entity.totalQuestions,
                correctAnswers = entity.correctAnswers,
                mastery = mastery,
                trend = Trend.STABLE,
            )
        }
    }
}
