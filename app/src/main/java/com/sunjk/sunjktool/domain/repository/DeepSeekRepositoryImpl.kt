package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.BalanceRecordDao
import com.sunjk.sunjktool.data.model.BalanceRecordEntity
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.BalanceHistoryPoint
import com.sunjk.sunjktool.domain.model.DeepSeekBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class DeepSeekRepositoryImpl(
    private val api: DeepSeekApi,
    private val dao: BalanceRecordDao
) : DeepSeekRepository {

    private val _balance = MutableStateFlow(DeepSeekBalance())
    override val balance: StateFlow<DeepSeekBalance> = _balance.asStateFlow()

    override suspend fun refresh() {
        try {
            val resp = api.getBalance()
            val info = resp.balanceInfos.firstOrNull() ?: return
            val b = DeepSeekBalance(
                totalBalance = info.totalBalance.toDoubleOrNull() ?: 0.0,
                grantedBalance = info.grantedBalance.toDoubleOrNull() ?: 0.0,
                toppedUpBalance = info.toppedUpBalance.toDoubleOrNull() ?: 0.0,
                currency = info.currency,
                isAvailable = resp.isAvailable
            )
            _balance.value = b
            dao.insert(BalanceRecordEntity(totalBalance = b.totalBalance, grantedBalance = b.grantedBalance, toppedUpBalance = b.toppedUpBalance, timestamp = System.currentTimeMillis()))
            SyncTrigger.requestAutoSync()
            SyncTrigger.bumpEntity("balance_records")
        } catch (_: Exception) {
            // keep last known value
        }
    }

    override fun getHistory(days: Int): Flow<List<BalanceHistoryPoint>> =
        dao.getSince(System.currentTimeMillis() - days * 86400000L)
            .map { list -> list.map { BalanceHistoryPoint(it.timestamp, it.totalBalance) } }
}
