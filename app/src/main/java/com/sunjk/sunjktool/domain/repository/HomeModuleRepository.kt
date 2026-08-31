package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.model.HomeModuleEntity
import kotlinx.coroutines.flow.Flow

interface HomeModuleRepository {
    fun getAll(): Flow<List<HomeModuleEntity>>
    suspend fun updateAll(modules: List<HomeModuleEntity>)
    suspend fun initializeIfNeeded()
    suspend fun cleanOrphaned(validKeys: Set<String>)
    suspend fun updateSize(key: String, size: String)
}