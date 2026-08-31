package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable

@Stable
data class DeepSeekBalance(
    val totalBalance: Double = 0.0,
    val grantedBalance: Double = 0.0,
    val toppedUpBalance: Double = 0.0,
    val currency: String = "CNY",
    val isAvailable: Boolean = true
)

@Stable
data class BalanceHistoryPoint(
    val timestamp: Long,
    val totalBalance: Double
)
