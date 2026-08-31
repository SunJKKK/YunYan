package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorArgb: Int,           // Color.toArgb()
    val createdAt: Long,          // epoch millis
    val updatedAt: Long           // epoch millis
)
