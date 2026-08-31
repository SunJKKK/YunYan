package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.QuestionBankCategoryDao
import com.sunjk.sunjktool.data.local.dao.QuestionDao
import com.sunjk.sunjktool.data.model.QuestionBankCategoryEntity
import com.sunjk.sunjktool.data.model.QuestionEntity
import com.sunjk.sunjktool.domain.model.Question
import com.sunjk.sunjktool.domain.model.QuestionBankCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class QuestionBankRepositoryImpl(
    private val categoryDao: QuestionBankCategoryDao,
    private val questionDao: QuestionDao
) : QuestionBankRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Categories ──────────────────────────────────────────────────

    override fun getAllCategories(): Flow<List<QuestionBankCategory>> = flow {
        categoryDao.getAll().collect { entities ->
            emit(entities.map { it.toDomainWithCounts() })
        }
    }

    override fun getCategoryById(id: Long): Flow<QuestionBankCategory?> =
        categoryDao.getById(id).map { it?.toDomain() }

    override fun getRootCategories(): Flow<List<QuestionBankCategory>> = flow {
        categoryDao.getRoots().collect { entities ->
            emit(entities.map { it.toDomainWithCounts() })
        }
    }

    override fun getChildCategories(parentId: Long): Flow<List<QuestionBankCategory>> = flow {
        categoryDao.getByParentId(parentId).collect { entities ->
            emit(entities.map { it.toDomainWithCounts() })
        }
    }

    override suspend fun saveCategory(category: QuestionBankCategory): Long {
        val now = LocalDateTime.now()
        val entity = category.toEntity(now)
        return if (category.id == 0L) {
            categoryDao.insert(entity)
        } else {
            categoryDao.update(entity)
            category.id
        }
    }

    override suspend fun deleteCategory(id: Long) {
        val category = categoryDao.getById(id).first()?.toDomain()
        val deletedParentId = category?.parentId
        // Reparent children to the deleted category's parent
        categoryDao.reparentChildren(id, deletedParentId)
        // Delete all questions in this category
        questionDao.deleteByCategoryId(id)
        // Delete the category
        categoryDao.deleteById(id)
    }

    override suspend fun getBreadcrumbs(categoryId: Long): List<Pair<Long, String>> {
        val breadcrumbs = mutableListOf<Pair<Long, String>>()
        var currentId: Long? = categoryId
        while (currentId != null) {
            val entity = categoryDao.getById(currentId).first()
            if (entity != null) {
                breadcrumbs.add(0, entity.id to entity.name)
                currentId = entity.parentId
            } else {
                currentId = null
            }
        }
        return breadcrumbs
    }

    override suspend fun getDescendantIds(categoryId: Long): Set<Long> {
        val result = mutableSetOf<Long>()
        suspend fun collectChildren(parentId: Long) {
            val children = categoryDao.getByParentId(parentId).first()
            for (child in children) {
                result.add(child.id)
                collectChildren(child.id)
            }
        }
        collectChildren(categoryId)
        return result
    }

    // ── Questions ───────────────────────────────────────────────────

    override fun getQuestionsByCategoryId(categoryId: Long): Flow<List<Question>> = flow {
        questionDao.getByCategoryId(categoryId).collect { entities ->
            emit(entities.map { it.toDomain() })
        }
    }

    override fun getQuestionById(id: Long): Flow<Question?> =
        questionDao.getById(id).map { it?.toDomain() }

    override fun getAllQuestions(): Flow<List<Question>> = flow {
        questionDao.getAll().collect { entities ->
            emit(entities.map { it.toDomain() })
        }
    }

    override suspend fun saveQuestion(question: Question): Long {
        val now = LocalDateTime.now()
        val entity = question.toEntity(now)
        return if (question.id == 0L) {
            questionDao.insert(entity)
        } else {
            questionDao.update(entity)
            question.id
        }
    }

    override suspend fun saveQuestions(questions: List<Question>): List<Long> {
        val now = LocalDateTime.now()
        val entities = questions.map { it.toEntity(now) }
        return questionDao.insertAll(entities)
    }

    override suspend fun deleteQuestion(id: Long) {
        questionDao.deleteById(id)
    }

    // ── Mappings ────────────────────────────────────────────────────

    private suspend fun QuestionBankCategoryEntity.toDomainWithCounts(): QuestionBankCategory {
        val subCount = categoryDao.getSubCategoryCount(id).firstOrNull() ?: 0
        val qCount = categoryDao.getQuestionCount(id).firstOrNull() ?: 0
        return QuestionBankCategory(
            id = id,
            name = name,
            parentId = parentId,
            sortOrder = sortOrder,
            subCategoryCount = subCount,
            questionCount = qCount,
            createdDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()),
            updatedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault())
        )
    }

    private fun QuestionBankCategoryEntity.toDomain() = QuestionBankCategory(
        id = id,
        name = name,
        parentId = parentId,
        sortOrder = sortOrder,
        createdDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()),
        updatedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault())
    )

    private fun QuestionBankCategory.toEntity(now: LocalDateTime) = QuestionBankCategoryEntity(
        id = id,
        name = name,
        parentId = parentId,
        sortOrder = sortOrder,
        createdDate = if (id == 0L) now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                      else createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedDate = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    private fun QuestionEntity.toDomain() = Question(
        id = id,
        categoryId = categoryId,
        content = content,
        imagePaths = decodePaths(imagePaths),
        aiAnalysis = aiAnalysis,
        sortOrder = sortOrder,
        createdDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()),
        updatedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault())
    )

    private fun Question.toEntity(now: LocalDateTime) = QuestionEntity(
        id = id,
        categoryId = categoryId,
        content = content,
        imagePaths = encodePaths(imagePaths),
        aiAnalysis = aiAnalysis,
        sortOrder = sortOrder,
        createdDate = if (id == 0L) now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                      else createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedDate = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    private fun encodePaths(paths: List<String>): String =
        if (paths.isEmpty()) "" else json.encodeToString(paths)

    private fun decodePaths(raw: String): List<String> =
        if (raw.isBlank()) emptyList()
        else try { json.decodeFromString<List<String>>(raw) } catch (_: Exception) { emptyList() }
}
