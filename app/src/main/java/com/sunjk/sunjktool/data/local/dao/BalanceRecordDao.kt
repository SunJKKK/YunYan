package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.BalanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceRecordDao {
    @Query("SELECT * FROM balance_records WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getSince(since: Long): Flow<List<BalanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BalanceRecordEntity)

    @Query("DELETE FROM balance_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
