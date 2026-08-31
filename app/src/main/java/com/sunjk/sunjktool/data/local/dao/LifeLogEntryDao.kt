package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunjk.sunjktool.data.model.LifeLogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeLogEntryDao {

    @Query("SELECT * FROM life_log_entries ORDER BY createdDate DESC")
    fun getAllEntries(): Flow<List<LifeLogEntryEntity>>

    @Query("SELECT * FROM life_log_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<LifeLogEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LifeLogEntryEntity): Long

    @Update
    suspend fun update(entry: LifeLogEntryEntity)

    @Delete
    suspend fun delete(entry: LifeLogEntryEntity)
}
