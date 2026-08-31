package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.HabitRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitRecordDao {
    @Query("SELECT * FROM habit_records ORDER BY date DESC")
    fun getAll(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE date = :date")
    suspend fun getByDate(date: String): HabitRecordEntity?

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date ASC")
    fun getByHabitId(habitId: Long): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND date >= :since ORDER BY date ASC")
    fun getByHabitIdSince(habitId: Long, since: String): Flow<List<HabitRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: HabitRecordEntity)

    @Query("DELETE FROM habit_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId")
    suspend fun deleteByHabitId(habitId: Long)
}
