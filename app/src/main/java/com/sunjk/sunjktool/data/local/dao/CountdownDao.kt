package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.CountdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {

    @Query("SELECT * FROM countdowns ORDER BY targetDate ASC")
    fun getAll(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns WHERE id = :id")
    fun getById(id: Long): Flow<CountdownEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(countdown: CountdownEntity): Long

    @Update
    suspend fun update(countdown: CountdownEntity)

    @Delete
    suspend fun delete(countdown: CountdownEntity)
}
