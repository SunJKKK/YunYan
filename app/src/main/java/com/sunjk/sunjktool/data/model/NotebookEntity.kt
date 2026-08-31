package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val icon: String = "folder",
    val pinned: Boolean = false,
    val createdDate: Long,
    val updatedDate: Long
)
