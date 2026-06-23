package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String = "",
    val title: String,
    val timeSpent: Int = 0,
    val imagePath: String? = null,
    val createdDate: Long,
    val updatedDate: Long
)
