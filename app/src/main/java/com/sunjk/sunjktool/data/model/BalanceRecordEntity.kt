package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance_records")
data class BalanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalBalance: Double,
    val grantedBalance: Double,
    val toppedUpBalance: Double,
    val timestamp: Long
)
