package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.ReviewStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewStatusDao {
    @Query("SELECT * FROM review_status WHERE reviewDate = :date ORDER BY id ASC")
    fun getByDate(date: Long): Flow<List<ReviewStatusEntity>>

    @Query("SELECT * FROM review_status ORDER BY reviewDate ASC")
    fun getAll(): Flow<List<ReviewStatusEntity>>

    @Query("SELECT * FROM review_status WHERE logEntryId = :entryId")
    fun getByEntryId(entryId: Long): Flow<List<ReviewStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReviewStatusEntity)

    @Query("UPDATE review_status SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM review_status WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM review_status WHERE logEntryId = :entryId")
    suspend fun deleteByEntryId(entryId: Long)

    @Query("SELECT DISTINCT reviewDate FROM review_status WHERE isCompleted = 0 ORDER BY reviewDate ASC")
    fun getPendingDates(): Flow<List<Long>>
}
