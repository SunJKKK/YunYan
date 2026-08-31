package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.PomodoroRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroRecordDao {
    @Query("SELECT * FROM pomodoro_records ORDER BY date DESC")
    fun getAll(): Flow<List<PomodoroRecordEntity>>

    @Query("SELECT * FROM pomodoro_records WHERE date = :date")
    suspend fun getByDate(date: String): PomodoroRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PomodoroRecordEntity)

    @Query("DELETE FROM pomodoro_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("SELECT COUNT(*) FROM pomodoro_records")
    suspend fun count(): Int
}
