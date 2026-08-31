package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.HomeModuleDao
import com.sunjk.sunjktool.data.model.HomeModuleEntity
import kotlinx.coroutines.flow.Flow

class HomeModuleRepositoryImpl(
    private val dao: HomeModuleDao
) : HomeModuleRepository {

    override fun getAll(): Flow<List<HomeModuleEntity>> = dao.getAll()

    override suspend fun updateAll(modules: List<HomeModuleEntity>) {
        dao.updateAll(modules)
    }

    override suspend fun cleanOrphaned(validKeys: Set<String>) {
        val allKeys = dao.getAllKeys()
        for (key in allKeys) {
            val isDynamic = key.startsWith("countdown_") || key.startsWith("habit_")
            if (isDynamic && key !in validKeys) {
                dao.deleteByKey(key)
            }
        }
    }

    override suspend fun initializeIfNeeded() {
        if (dao.count() == 0) {
            dao.insertDefaults(
                listOf(
                    HomeModuleEntity(moduleKey = "heatmap", enabled = true, sortOrder = 0),
                    HomeModuleEntity(moduleKey = "today_logs", enabled = true, sortOrder = 1),
                    HomeModuleEntity(moduleKey = "review", enabled = true, sortOrder = 2),
                    HomeModuleEntity(moduleKey = "weather", enabled = false, sortOrder = 3),
                    HomeModuleEntity(moduleKey = "pomodoro", enabled = false, sortOrder = 4),
                    HomeModuleEntity(moduleKey = "deepseek", enabled = false, sortOrder = 5)
                )
            )
        }
    }

    override suspend fun updateSize(key: String, size: String) {
        dao.updateSize(key, size)
    }
}