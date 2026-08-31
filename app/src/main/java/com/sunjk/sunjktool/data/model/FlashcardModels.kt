package com.sunjk.sunjktool.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FlashcardSetJson(val cards: List<FlashcardItemJson>)

@Serializable
data class UserAnswerJson(
    val type: String,
    val isCorrect: Boolean = false,
    val userChoice: String = "",
    val totalAttempts: Int = 0,
    val correctCount: Int = 0
)

@Serializable
data class FlashcardItemJson(
    val type: String,
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: JsonElement? = null,
    val answers: List<Int> = emptyList(),
    val front: String = "",
    val back: String = "",
    val explanation: String = "",
    val knowledgePoint: String = ""
)

// ─── Retrieval-augmented generation intermediate models ──────────────

@Serializable
data class GapAnalysisResult(
    val gaps: List<GapItem> = emptyList(),
    val extensions: List<ExtensionItem> = emptyList(),
    val missingDetails: List<String> = emptyList()
)

@Serializable
data class GapItem(
    val topic: String,
    val description: String,
    val importance: String = "medium"  // high / medium / low
)

@Serializable
data class ExtensionItem(
    val topic: String,
    val direction: String
)

@Serializable
data class KnowledgeSupplementResult(
    val supplements: List<SupplementItem> = emptyList()
)

@Serializable
data class SupplementItem(
    val topic: String,
    val content: String,
    val keyPoints: List<String> = emptyList()
)

