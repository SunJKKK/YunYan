package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_records")
data class PomodoroRecordEntity(
    @PrimaryKey val date: String,  // "yyyy-MM-dd"
    val focusSecs: Long,
    val completedCount: Int,
    val updatedDate: Long  // epoch millis, LWW sync
)
