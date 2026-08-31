package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE categoryId = :categoryId ORDER BY sortOrder ASC, createdDate ASC")
    fun getByCategoryId(categoryId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    fun getById(id: Long): Flow<QuestionEntity?>

    @Query("SELECT * FROM questions ORDER BY sortOrder ASC, createdDate ASC")
    fun getAll(): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>): List<Long>

    @Update
    suspend fun update(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM questions WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)
}
