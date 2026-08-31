package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_notes")
data class ReviewNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logEntryId: Long,
    val content: String,
    val imagePaths: String? = null,
    val sourceType: String = "manual",
    val flashcardSessionId: Long? = null,
    val createdDate: Long,
    val updatedDate: Long
)
