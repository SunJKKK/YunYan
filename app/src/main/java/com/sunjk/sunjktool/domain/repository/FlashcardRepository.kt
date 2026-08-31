package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.Flashcard
import com.sunjk.sunjktool.domain.model.FlashcardSession
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun getLatestSession(logEntryId: Long): Flow<FlashcardSession?>
    fun getSession(sessionId: Long): Flow<FlashcardSession?>
    fun getAllSessions(logEntryId: Long): Flow<List<FlashcardSession>>
    suspend fun saveSession(logEntryId: Long, cards: List<Flashcard>, style: String = ""): Long
    suspend fun saveAnswers(sessionId: Long, answers: Map<Int, com.sunjk.sunjktool.feature.learninglog.flashcard.UserAnswer>)
    suspend fun recordAttempt(sessionId: Long, cardIndex: Int, answer: com.sunjk.sunjktool.feature.learninglog.flashcard.UserAnswer)
    suspend fun deleteSession(sessionId: Long)
    suspend fun deleteSessionsForEntry(logEntryId: Long)
}
