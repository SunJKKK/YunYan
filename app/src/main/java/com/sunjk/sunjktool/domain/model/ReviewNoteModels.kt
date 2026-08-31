package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

enum class ReviewNoteSource { MANUAL, FLASHCARD }

@Stable
data class ReviewNote(
    val id: Long = 0,
    val logEntryId: Long,
    val content: String,
    val imagePaths: List<String> = emptyList(),
    val sourceType: ReviewNoteSource = ReviewNoteSource.MANUAL,
    val flashcardSessionId: Long? = null,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val updatedDate: LocalDateTime = LocalDateTime.now()
)
