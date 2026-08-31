package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import com.sunjk.sunjktool.data.model.FlashcardItemJson
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Stable
sealed class Flashcard {
    abstract val explanation: String
    abstract val knowledgePoint: String

    data class TrueFalse(
        val question: String,
        val answer: Boolean,
        override val explanation: String,
        override val knowledgePoint: String = ""
    ) : Flashcard()

    data class SingleChoice(
        val question: String,
        val options: List<String>,
        val answerIndex: Int,
        override val explanation: String,
        override val knowledgePoint: String = ""
    ) : Flashcard()

    data class MultiChoice(
        val question: String,
        val options: List<String>,
        val answerIndices: List<Int>,
        override val explanation: String,
        override val knowledgePoint: String = ""
    ) : Flashcard()

    data class Memory(
        val front: String,
        val back: String,
        override val explanation: String,
        override val knowledgePoint: String = ""
    ) : Flashcard()
}

@Stable
data class FlashcardSession(
    val id: Long = 0,
    val logEntryId: Long,
    val cards: List<Flashcard>,
    val answers: Map<Int, AnswerRecord> = emptyMap(),
    val style: String = "",
    val createdDate: Long
)

@Stable
data class AnswerRecord(
    val isCorrect: Boolean,
    val userChoice: String = "",
    val totalAttempts: Int = 0,
    val correctCount: Int = 0
)

fun FlashcardItemJson.toDomain(): Flashcard = when (type) {
    "true_false" -> Flashcard.TrueFalse(
        question = question,
        answer = (answer?.jsonPrimitive?.booleanOrNull) ?: false,
        explanation = explanation,
        knowledgePoint = knowledgePoint
    )
    "single_choice" -> Flashcard.SingleChoice(
        question = question,
        options = options,
        answerIndex = (answer?.jsonPrimitive?.intOrNull) ?: 0,
        explanation = explanation,
        knowledgePoint = knowledgePoint
    )
    "multi_choice" -> Flashcard.MultiChoice(
        question = question,
        options = options,
        answerIndices = answers,
        explanation = explanation,
        knowledgePoint = knowledgePoint
    )
    "memory" -> Flashcard.Memory(
        front = front.ifBlank { question },
        back = back.ifBlank { explanation },
        explanation = explanation,
        knowledgePoint = knowledgePoint
    )
    else -> Flashcard.Memory(
        front = question.ifBlank { front },
        back = explanation,
        explanation = explanation,
        knowledgePoint = knowledgePoint
    )
}
