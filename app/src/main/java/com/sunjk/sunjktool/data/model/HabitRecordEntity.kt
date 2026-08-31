package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_records")
data class HabitRecordEntity(
    @PrimaryKey val date: String,  // "{habitId}_yyyy-MM-dd"
    val habitId: Long,
    val isCompleted: Boolean = false,
    val updatedAt: Long            // epoch millis, LWW sync
)
