package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ticktick_projects")
data class TickTickProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Long = 0,
    val color: String = ""
)

@Entity(tableName = "ticktick_tasks")
data class TickTickTaskEntity(
    @PrimaryKey val id: String,
    val projectId: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val dueDate: String? = null,
    val priority: Int = 0,
    val sortOrder: Long = 0,
    val content: String = ""
)
