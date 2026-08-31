package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.QuestionBankCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionBankCategoryDao {

    @Query("SELECT * FROM question_bank_categories ORDER BY sortOrder ASC, createdDate ASC")
    fun getAll(): Flow<List<QuestionBankCategoryEntity>>

    @Query("SELECT * FROM question_bank_categories WHERE id = :id")
    fun getById(id: Long): Flow<QuestionBankCategoryEntity?>

    @Query("SELECT * FROM question_bank_categories WHERE parentId = :parentId ORDER BY sortOrder ASC, createdDate ASC")
    fun getByParentId(parentId: Long): Flow<List<QuestionBankCategoryEntity>>

    @Query("SELECT * FROM question_bank_categories WHERE parentId IS NULL ORDER BY sortOrder ASC, createdDate ASC")
    fun getRoots(): Flow<List<QuestionBankCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: QuestionBankCategoryEntity): Long

    @Update
    suspend fun update(category: QuestionBankCategoryEntity)

    @Query("DELETE FROM question_bank_categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM question_bank_categories WHERE parentId = :parentId")
    fun getSubCategoryCount(parentId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions WHERE categoryId = :categoryId")
    fun getQuestionCount(categoryId: Long): Flow<Int>

    @Query("UPDATE question_bank_categories SET parentId = :newParentId WHERE parentId = :oldParentId")
    suspend fun reparentChildren(oldParentId: Long, newParentId: Long?)
}
