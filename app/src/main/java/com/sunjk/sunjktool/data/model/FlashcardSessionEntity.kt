package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcard_sessions")
data class FlashcardSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logEntryId: Long,
    val cardsJson: String,
    val answersJson: String = "{}",
    val style: String = "",
    val createdDate: Long
)
