package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_point_stats")
data class KnowledgePointStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logEntryId: Long,
    val knowledgePoint: String,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val weaknessSummary: String = "",  // AI-generated weakness analysis, JSON array of strings
    val updatedDate: Long
)
