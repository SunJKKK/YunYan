package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getById(id: Long): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int
}
