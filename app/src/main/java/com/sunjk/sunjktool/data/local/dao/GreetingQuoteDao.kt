package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.GreetingQuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GreetingQuoteDao {
    @Query("SELECT * FROM greeting_quotes ORDER BY id DESC")
    fun getAll(): Flow<List<GreetingQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: GreetingQuoteEntity)

    @Query("DELETE FROM greeting_quotes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM greeting_quotes")
    suspend fun count(): Int
}
