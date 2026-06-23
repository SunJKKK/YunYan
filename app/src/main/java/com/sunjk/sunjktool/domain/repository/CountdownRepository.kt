package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.Countdown
import kotlinx.coroutines.flow.Flow

interface CountdownRepository {
    fun getAll(): Flow<List<Countdown>>
    fun getById(id: Long): Flow<Countdown?>
    suspend fun save(countdown: Countdown): Long
    suspend fun delete(id: Long)
}
