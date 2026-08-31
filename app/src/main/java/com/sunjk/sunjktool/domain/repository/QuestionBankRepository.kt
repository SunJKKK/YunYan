package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.Question
import com.sunjk.sunjktool.domain.model.QuestionBankCategory
import kotlinx.coroutines.flow.Flow

interface QuestionBankRepository {
    // Categories
    fun getAllCategories(): Flow<List<QuestionBankCategory>>
    fun getCategoryById(id: Long): Flow<QuestionBankCategory?>
    fun getRootCategories(): Flow<List<QuestionBankCategory>>
    fun getChildCategories(parentId: Long): Flow<List<QuestionBankCategory>>
    suspend fun saveCategory(category: QuestionBankCategory): Long
    suspend fun deleteCategory(id: Long)
    suspend fun getBreadcrumbs(categoryId: Long): List<Pair<Long, String>>
    suspend fun getDescendantIds(categoryId: Long): Set<Long>

    // Questions
    fun getQuestionsByCategoryId(categoryId: Long): Flow<List<Question>>
    fun getQuestionById(id: Long): Flow<Question?>
    fun getAllQuestions(): Flow<List<Question>>
    suspend fun saveQuestion(question: Question): Long
    suspend fun saveQuestions(questions: List<Question>): List<Long>
    suspend fun deleteQuestion(id: Long)
}
