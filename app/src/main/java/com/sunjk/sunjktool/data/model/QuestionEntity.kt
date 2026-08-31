package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val content: String,
    val imagePaths: String = "",
    val aiAnalysis: String = "",
    val sortOrder: Int = 0,
    val createdDate: Long,
    val updatedDate: Long
)
