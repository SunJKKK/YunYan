package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_status",
    indices = [Index("logEntryId"), Index("reviewDate")]
)
data class ReviewStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logEntryId: Long,
    val reviewDate: Long,
    val reviewType: String,
    val isCompleted: Boolean = false
)
