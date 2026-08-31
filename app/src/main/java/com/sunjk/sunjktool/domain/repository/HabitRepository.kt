package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.model.HabitRecordEntity
import com.sunjk.sunjktool.domain.model.Habit
import com.sunjk.sunjktool.domain.model.HabitRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getAll(): Flow<List<Habit>>
    fun getById(id: Long): Flow<Habit?>
    fun getRecordsByHabitId(habitId: Long): Flow<List<HabitRecord>>
    fun getRecordsByHabitIdSince(habitId: Long, since: LocalDate): Flow<List<HabitRecord>>
    fun getAllRecords(): Flow<List<HabitRecordEntity>>
    suspend fun save(habit: Habit): Long
    suspend fun delete(id: Long)
    suspend fun toggleRecord(habitId: Long, date: LocalDate)
    suspend fun isCompleted(habitId: Long, date: LocalDate): Boolean
}
