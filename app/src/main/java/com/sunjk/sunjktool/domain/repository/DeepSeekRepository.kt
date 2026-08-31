package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.BalanceHistoryPoint
import com.sunjk.sunjktool.domain.model.DeepSeekBalance
import kotlinx.coroutines.flow.Flow

interface DeepSeekRepository {
    val balance: Flow<DeepSeekBalance>
    suspend fun refresh()
    fun getHistory(days: Int): Flow<List<BalanceHistoryPoint>>
}
