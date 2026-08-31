package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "life_log_entries")
data class LifeLogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String = "",
    val mood: String = "",           // comma-separated mood keys, e.g. "happy,calm"
    val imagePath: String? = null,   // JSON-encoded list of image paths
    val createdDate: Long,
    val updatedDate: Long
)
