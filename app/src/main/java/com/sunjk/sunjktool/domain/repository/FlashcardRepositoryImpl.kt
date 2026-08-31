package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.FlashcardSessionDao
import com.sunjk.sunjktool.data.model.FlashcardItemJson
import com.sunjk.sunjktool.data.model.FlashcardSetJson
import com.sunjk.sunjktool.data.model.FlashcardSessionEntity
import com.sunjk.sunjktool.data.model.UserAnswerJson
import com.sunjk.sunjktool.domain.model.AnswerRecord
import com.sunjk.sunjktool.domain.model.Flashcard
import com.sunjk.sunjktool.domain.model.FlashcardSession
import com.sunjk.sunjktool.domain.model.toDomain
import com.sunjk.sunjktool.feature.learninglog.flashcard.UserAnswer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.encodeToString

class FlashcardRepositoryImpl(
    private val dao: FlashcardSessionDao
) : FlashcardRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun getLatestSession(logEntryId: Long): Flow<FlashcardSession?> =
        dao.getLatestForEntry(logEntryId).map { it?.toDomain() }

    override fun getSession(sessionId: Long): Flow<FlashcardSession?> =
        dao.getById(sessionId).map { it?.toDomain() }

    override fun getAllSessions(logEntryId: Long): Flow<List<FlashcardSession>> =
        dao.getAllForEntry(logEntryId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveSession(logEntryId: Long, cards: List<Flashcard>, style: String): Long {
        val items = cards.map { card ->
            when (card) {
                is Flashcard.TrueFalse -> FlashcardItemJson(
                    type = "true_false", question = card.question,
                    answer = JsonPrimitive(card.answer),
                    explanation = card.explanation,
                    knowledgePoint = card.knowledgePoint
                )
                is Flashcard.SingleChoice -> FlashcardItemJson(
                    type = "single_choice", question = card.question,
                    options = card.options,
                    answer = JsonPrimitive(card.answerIndex),
                    explanation = card.explanation,
                    knowledgePoint = card.knowledgePoint
                )
                is Flashcard.MultiChoice -> FlashcardItemJson(
                    type = "multi_choice", question = card.question,
                    options = card.options, answers = card.answerIndices,
                    explanation = card.explanation,
                    knowledgePoint = card.knowledgePoint
                )
                is Flashcard.Memory -> FlashcardItemJson(
                    type = "memory", front = card.front, back = card.back,
                    explanation = card.explanation,
                    knowledgePoint = card.knowledgePoint
                )
            }
        }
        val cardsJson = json.encodeToString(FlashcardSetJson(items))
        return dao.insert(
            FlashcardSessionEntity(
                logEntryId = logEntryId,
                cardsJson = cardsJson,
                style = style,
                createdDate = System.currentTimeMillis()
            )
        )
    }

    override suspend fun saveAnswers(sessionId: Long, answers: Map<Int, UserAnswer>) {
        val entity = dao.getByIdOnce(sessionId) ?: return
        // Merge with existing attempt counts so recordAttempt data is not overwritten
        val existing: Map<String, UserAnswerJson> = try {
            json.decodeFromString<Map<String, UserAnswerJson>>(entity.answersJson)
        } catch (_: Exception) { emptyMap() }
        val answerMap = answers.mapValues { (cardIdx, v) ->
            val prev = existing[cardIdx.toString()]
            buildAnswerJson(v).copy(
                totalAttempts = prev?.totalAttempts ?: 0,
                correctCount = prev?.correctCount ?: 0
            )
        }
        dao.updateAnswers(sessionId, json.encodeToString(answerMap))
    }

    override suspend fun recordAttempt(sessionId: Long, cardIndex: Int, answer: UserAnswer) {
        val entity = dao.getByIdOnce(sessionId) ?: return
        val existing: MutableMap<String, UserAnswerJson> = try {
            val raw = json.decodeFromString<Map<String, UserAnswerJson>>(entity.answersJson)
            raw.toMutableMap()
        } catch (_: Exception) { mutableMapOf() }

        val key = cardIndex.toString()
        val prev = existing[key]
        val totalAttempts = (prev?.totalAttempts ?: 0) + 1
        val correct = answer.isAnswerCorrect()
        val correctCount = (prev?.correctCount ?: 0) + if (correct) 1 else 0

        existing[key] = buildAnswerJson(answer).copy(
            totalAttempts = totalAttempts,
            correctCount = correctCount
        )
        dao.updateAnswers(sessionId, json.encodeToString(existing))
    }

    private fun buildAnswerJson(v: UserAnswer) = when (v) {
        is UserAnswer.TrueFalse -> UserAnswerJson("true_false", v.isCorrect, v.userAnswer.toString())
        is UserAnswer.SingleChoice -> UserAnswerJson("single_choice", v.isCorrect, v.selectedIndex.toString())
        is UserAnswer.MultiChoiceChoice -> UserAnswerJson("multi_choice", v.isCorrect, v.selectedIndices.joinToString(","))
        is UserAnswer.Memory -> UserAnswerJson("memory", v.known, if (v.known) "known" else "unknown")
    }

    private fun UserAnswer.isAnswerCorrect(): Boolean = when (this) {
        is UserAnswer.TrueFalse -> isCorrect
        is UserAnswer.SingleChoice -> isCorrect
        is UserAnswer.MultiChoiceChoice -> isCorrect
        is UserAnswer.Memory -> known
    }

    override suspend fun deleteSession(sessionId: Long) {
        dao.deleteById(sessionId)
    }

    override suspend fun deleteSessionsForEntry(logEntryId: Long) {
        dao.deleteAllForEntry(logEntryId)
    }

    private fun FlashcardSessionEntity.toDomain(): FlashcardSession {
        val set = json.decodeFromString<FlashcardSetJson>(cardsJson)
        val answers: Map<Int, AnswerRecord> = try {
            val raw = json.decodeFromString<Map<String, UserAnswerJson>>(answersJson)
            raw.mapKeys { it.key.toInt() }.mapValues { (_, v) -> AnswerRecord(v.isCorrect, v.userChoice, v.totalAttempts, v.correctCount) }
        } catch (_: Exception) { emptyMap() }
        return FlashcardSession(
            id = id,
            logEntryId = logEntryId,
            cards = set.cards.map { it.toDomain() },
            answers = answers,
            style = style,
            createdDate = createdDate
        )
    }
}
