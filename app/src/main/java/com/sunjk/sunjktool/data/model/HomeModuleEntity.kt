package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_modules")
data class HomeModuleEntity(
    @PrimaryKey val moduleKey: String,
    val enabled: Boolean,
    val sortOrder: Int,
    val selectedCountdownId: Long? = null,
    val size: String = "small"
)