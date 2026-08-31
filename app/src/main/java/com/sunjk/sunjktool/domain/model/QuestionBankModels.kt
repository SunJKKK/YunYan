package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

@Stable
data class QuestionBankCategory(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val subCategoryCount: Int = 0,
    val questionCount: Int = 0,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)

@Stable
data class Question(
    val id: Long = 0,
    val categoryId: Long,
    val content: String,
    val imagePaths: List<String> = emptyList(),
    val aiAnalysis: String = "",
    val sortOrder: Int = 0,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)

@Stable
data class SplitQuestionItem(
    val index: Int,
    val content: String
)
