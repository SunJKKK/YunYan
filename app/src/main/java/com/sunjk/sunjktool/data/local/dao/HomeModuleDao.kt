package com.sunjk.sunjktool.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunjk.sunjktool.data.model.HomeModuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeModuleDao {

    @Query("SELECT * FROM home_modules ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<HomeModuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(modules: List<HomeModuleEntity>)

    @Query("SELECT COUNT(*) FROM home_modules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaults(modules: List<HomeModuleEntity>)

    @Query("DELETE FROM home_modules WHERE moduleKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT moduleKey FROM home_modules")
    suspend fun getAllKeys(): List<String>

    @Query("UPDATE home_modules SET size = :size WHERE moduleKey = :key")
    suspend fun updateSize(key: String, size: String)
}
