package com.sunjk.sunjktool.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepSeekBalanceResponse(
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("balance_infos") val balanceInfos: List<BalanceInfo> = emptyList()
)

@Serializable
data class BalanceInfo(
    val currency: String = "CNY",
    @SerialName("total_balance") val totalBalance: String = "0.00",
    @SerialName("granted_balance") val grantedBalance: String = "0.00",
    @SerialName("topped_up_balance") val toppedUpBalance: String = "0.00"
)
