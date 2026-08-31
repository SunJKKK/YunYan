package com.sunjk.sunjktool.feature.learninglog.flashcard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.Flashcard
import com.sunjk.sunjktool.domain.model.FlashcardSession
import com.sunjk.sunjktool.domain.model.ReviewNote
import com.sunjk.sunjktool.domain.model.ReviewNoteSource
import com.sunjk.sunjktool.domain.repository.FlashcardRepository
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UserAnswer {
    data class TrueFalse(val userAnswer: Boolean, val isCorrect: Boolean) : UserAnswer()
    data class SingleChoice(val selectedIndex: Int, val isCorrect: Boolean) : UserAnswer()
    data class MultiChoiceChoice(val selectedIndices: Set<Int>, val isCorrect: Boolean, val confirmed: Boolean) : UserAnswer()
    data class Memory(val known: Boolean, val assessed: Boolean = false) : UserAnswer()
}

@Immutable
data class FlashcardUiState(
    val session: FlashcardSession? = null,
    val isLoading: Boolean = true,
    val currentCardIndex: Int = 0,
    val userAnswers: Map<Int, UserAnswer> = emptyMap(),
    val showExplanation: Boolean = false,
    val isComplete: Boolean = false,
    val autoAdvance: Boolean = false,
    val showContinueDialog: Boolean = false,
    val exportedToReviewNote: Boolean = false,
    val error: String? = null
)

class FlashcardViewModel(
    private val repository: FlashcardRepository,
    private val reviewNoteRepository: ReviewNoteRepository,
    private val knowledgePointStatsRepository: com.sunjk.sunjktool.domain.repository.KnowledgePointStatsRepository,
    private val logEntryId: Long,
    private val sessionId: Long? = null,
    autoAdvance: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState(autoAdvance = autoAdvance))
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    /** Only show the continue dialog on the first emission, not on every DB-triggered re-emit. */
    private var continueDialogHandled = false

    init {
        viewModelScope.launch {
            val flow = if (sessionId != null) {
                repository.getSession(sessionId)
            } else {
                repository.getLatestSession(logEntryId)
            }
            var emissionCount = 0
            flow.collect { session ->
                // Restore saved answers
                val restored = session?.answers?.mapValues { (idx, record) ->
                    val card = session.cards.getOrNull(idx)
                    when (card) {
                        is com.sunjk.sunjktool.domain.model.Flashcard.TrueFalse ->
                            UserAnswer.TrueFalse(record.userChoice.toBooleanStrictOrNull() ?: false, record.isCorrect)
                        is com.sunjk.sunjktool.domain.model.Flashcard.SingleChoice ->
                            UserAnswer.SingleChoice(record.userChoice.toIntOrNull() ?: 0, record.isCorrect)
                        is com.sunjk.sunjktool.domain.model.Flashcard.MultiChoice ->
                            UserAnswer.MultiChoiceChoice(
                                record.userChoice.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
                                record.isCorrect, true
                            )
                        is com.sunjk.sunjktool.domain.model.Flashcard.Memory ->
                            UserAnswer.Memory(record.userChoice == "known", assessed = true)
                        else -> null
                    }
                }?.filterValues { it != null }?.mapValues { it.value!! } ?: emptyMap()
                // Determine if already complete
                val isComplete = restored.size == session?.cards?.size && session.cards.isNotEmpty()
                val hasPartial = restored.isNotEmpty() && !isComplete
                // Only show dialog on first emission (re-entry from DB), suppress on
                // subsequent emissions triggered by answer saves during this session.
                val isFirstEmission = emissionCount == 0
                val showDialog = isFirstEmission && hasPartial && !continueDialogHandled
                emissionCount++
                _uiState.update {
                    it.copy(
                        session = session,
                        isLoading = false,
                        // Only restore answers from DB on first emission; after that the
                        // UI state's userAnswers is the source of truth (restart needs this).
                        userAnswers = if (isFirstEmission) restored else it.userAnswers,
                        isComplete = if (isFirstEmission) isComplete else it.isComplete,
                        showContinueDialog = showDialog
                    )
                }
            }
        }
    }

    val currentCard: Flashcard?
        get() = _uiState.value.session?.cards?.getOrNull(_uiState.value.currentCardIndex)

    fun answerTrueFalse(userAnswer: Boolean) {
        val card = currentCard as? Flashcard.TrueFalse ?: return
        if (isAnswered()) return
        val isCorrect = userAnswer == card.answer
        _uiState.update { state ->
            state.copy(
                userAnswers = state.userAnswers + (state.currentCardIndex to UserAnswer.TrueFalse(userAnswer, isCorrect)),
                showExplanation = true
            )
        }
        saveCurrentAttempt()
        recordCurrentCardKP()
        if (isCorrect) tryAutoAdvance()
    }

    fun answerSingleChoice(selectedIndex: Int) {
        val card = currentCard as? Flashcard.SingleChoice ?: return
        if (isAnswered()) return
        val isCorrect = selectedIndex == card.answerIndex
        _uiState.update { state ->
            state.copy(
                userAnswers = state.userAnswers + (state.currentCardIndex to UserAnswer.SingleChoice(selectedIndex, isCorrect)),
                showExplanation = true
            )
        }
        saveCurrentAttempt()
        recordCurrentCardKP()
        if (isCorrect) tryAutoAdvance()
    }

    fun toggleMultiChoiceOption(optionIndex: Int) {
        val confirmed = (_uiState.value.userAnswers[_uiState.value.currentCardIndex] as? UserAnswer.MultiChoiceChoice)?.confirmed == true
        if (confirmed) return
        val current = _uiState.value.userAnswers[_uiState.value.currentCardIndex] as? UserAnswer.MultiChoiceChoice
        val selected = (current?.selectedIndices ?: emptySet()).toMutableSet()
        if (optionIndex in selected) selected.remove(optionIndex) else selected.add(optionIndex)
        _uiState.update { state ->
            state.copy(
                userAnswers = state.userAnswers + (state.currentCardIndex to UserAnswer.MultiChoiceChoice(selected, false, false))
            )
        }
    }

    fun confirmMultiChoice() {
        val card = currentCard as? Flashcard.MultiChoice ?: return
        val confirmed = (_uiState.value.userAnswers[_uiState.value.currentCardIndex] as? UserAnswer.MultiChoiceChoice)?.confirmed == true
        if (confirmed) return
        val userAns = _uiState.value.userAnswers[_uiState.value.currentCardIndex] as? UserAnswer.MultiChoiceChoice ?: return
        if (userAns.selectedIndices.isEmpty()) return
        val isCorrect = userAns.selectedIndices.sorted() == card.answerIndices.sorted()
        _uiState.update { state ->
            state.copy(
                userAnswers = state.userAnswers + (state.currentCardIndex to UserAnswer.MultiChoiceChoice(userAns.selectedIndices, isCorrect, true)),
                showExplanation = true
            )
        }
        saveCurrentAttempt()
        recordCurrentCardKP()
        if (isCorrect) tryAutoAdvance()
    }

    fun revealMemory() {
        if (isMemoryRevealed()) return
        _uiState.update { state ->
            state.copy(
                userAnswers = state.userAnswers + (state.currentCardIndex to UserAnswer.Memory(false, assessed = false)),
                showExplanation = false
            )
        }
    }

    fun markMemoryKnown() {
        _uiState.update { state ->
            state.copy(
                userAnswers = state.userAnswers + (state.currentCardIndex to UserAnswer.Memory(true, assessed = true)),
                showExplanation = true
            )
        }
        saveCurrentAttempt()
        recordCurrentCardKP()
        tryAutoAdvance()
    }

    fun markMemoryUnknown() {
        _uiState.update { state ->
            state.copy(
                userAnswers = state.userAnswers + (state.currentCardIndex to UserAnswer.Memory(false, assessed = true)),
                showExplanation = true
            )
        }
        saveCurrentAttempt()
        recordCurrentCardKP()
    }

    fun isMemoryRevealed(): Boolean = _uiState.value.userAnswers.containsKey(_uiState.value.currentCardIndex)

    fun isAnswered(): Boolean {
        val ans = _uiState.value.userAnswers[_uiState.value.currentCardIndex] ?: return false
        // Multi-choice is not "answered" until confirmed
        if (ans is UserAnswer.MultiChoiceChoice) return ans.confirmed
        return true
    }

    fun setAutoAdvance(enabled: Boolean) {
        _uiState.update { it.copy(autoAdvance = enabled) }
    }

    fun toggleExplanation() {
        _uiState.update { it.copy(showExplanation = !it.showExplanation) }
    }

    private fun tryAutoAdvance() {
        if (!_uiState.value.autoAdvance) return
        nextCard()
    }

    fun nextCard() {
        if (!isAnswered()) return  // guard: must answer current card first
        val total = _uiState.value.session?.cards?.size ?: return
        val next = _uiState.value.currentCardIndex + 1
        if (next >= total) {
            _uiState.update { it.copy(isComplete = true) }
            // Save answers to DB
            val session = _uiState.value.session ?: return
            viewModelScope.launch {
                repository.saveAnswers(session.id, _uiState.value.userAnswers)
            }
        } else {
            _uiState.update { it.copy(currentCardIndex = next, showExplanation = false) }
        }
    }

    fun restart() {
        _uiState.update { it.copy(currentCardIndex = 0, userAnswers = emptyMap(), showExplanation = false, isComplete = false) }
        // Preserve attempt counts in DB — per-card correctness persists across restarts
    }

    fun dismissContinueDialog() {
        continueDialogHandled = true
        _uiState.update { it.copy(showContinueDialog = false) }
    }

    fun continueFromPrevious() {
        continueDialogHandled = true
        val session = _uiState.value.session ?: return
        val answers = _uiState.value.userAnswers
        val firstUnanswered = session.cards.indices.firstOrNull { it !in answers } ?: 0
        _uiState.update { it.copy(showContinueDialog = false, currentCardIndex = firstUnanswered, showExplanation = false) }
    }

    fun restartFromContinue() {
        continueDialogHandled = true
        // Preserve attempt counts in DB
        _uiState.update { it.copy(currentCardIndex = 0, userAnswers = emptyMap(), showExplanation = false, isComplete = false, showContinueDialog = false) }
    }

    fun exportWrongCardsToReviewNote() {
        val session = _uiState.value.session ?: return
        val wrongCardList = wrongCards
        if (wrongCardList.isEmpty()) return

        viewModelScope.launch {
            val md = buildWrongCardsMarkdown(session.cards.size, wrongCardList)
            reviewNoteRepository.save(
                ReviewNote(
                    logEntryId = logEntryId,
                    content = md,
                    sourceType = ReviewNoteSource.FLASHCARD,
                    flashcardSessionId = session.id,
                    createdDate = java.time.LocalDateTime.now(),
                    updatedDate = java.time.LocalDateTime.now()
                )
            )
            com.sunjk.sunjktool.data.sync.SyncTrigger.requestAutoSync()
            com.sunjk.sunjktool.data.sync.SyncTrigger.bumpEntity("review_notes")
            _uiState.update { it.copy(exportedToReviewNote = true) }
        }
    }

    private fun buildWrongCardsMarkdown(totalCards: Int, wrong: List<Pair<Int, com.sunjk.sunjktool.domain.model.Flashcard>>): String {
        val sb = StringBuilder()
        sb.appendLine("# 闪卡错题回顾")
        sb.appendLine()
        val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        sb.appendLine("> ${totalCards}道题 · $timeStr · ${wrong.size}道错题")
        sb.appendLine()

        wrong.forEach { (idx, card) ->
            val ans = _uiState.value.userAnswers[idx]
            sb.appendLine("## ${cardTypeLabel(card)} #${idx + 1}")
            sb.appendLine()

            when (card) {
                is com.sunjk.sunjktool.domain.model.Flashcard.TrueFalse -> {
                    sb.appendLine("**题目**: ${card.question}")
                    sb.appendLine()
                    val userAns = (ans as? com.sunjk.sunjktool.feature.learninglog.flashcard.UserAnswer.TrueFalse)?.userAnswer
                    sb.appendLine("❌ 你的答案: ${if (userAns == true) "正确" else "错误"}")
                    sb.appendLine("✅ 正确答案: ${if (card.answer) "正确" else "错误"}")
                }
                is com.sunjk.sunjktool.domain.model.Flashcard.SingleChoice -> {
                    sb.appendLine("**题目**: ${card.question}")
                    sb.appendLine()
                    card.options.forEachIndexed { optIdx, opt ->
                        val mark = when {
                            optIdx == card.answerIndex -> "✅"
                            (ans as? com.sunjk.sunjktool.feature.learninglog.flashcard.UserAnswer.SingleChoice)?.selectedIndex == optIdx -> "❌"
                            else -> "   "
                        }
                        sb.appendLine("$mark ${('A' + optIdx)}. ${com.sunjk.sunjktool.feature.learninglog.flashcard.cleanOption(opt)}")
                    }
                }
                is com.sunjk.sunjktool.domain.model.Flashcard.MultiChoice -> {
                    sb.appendLine("**题目**: ${card.question}")
                    sb.appendLine()
                    card.options.forEachIndexed { optIdx, opt ->
                        val userIndices = (ans as? com.sunjk.sunjktool.feature.learninglog.flashcard.UserAnswer.MultiChoiceChoice)?.selectedIndices ?: emptySet()
                        val mark = when {
                            optIdx in card.answerIndices && optIdx in userIndices -> "✅"
                            optIdx in card.answerIndices -> "✅"
                            optIdx in userIndices -> "❌"
                            else -> "   "
                        }
                        sb.appendLine("$mark ${('A' + optIdx)}. ${com.sunjk.sunjktool.feature.learninglog.flashcard.cleanOption(opt)}")
                    }
                }
                is com.sunjk.sunjktool.domain.model.Flashcard.Memory -> {
                    sb.appendLine("**正面**: ${card.front}")
                    sb.appendLine()
                    sb.appendLine("**背面**: ${card.back}")
                }
            }

            if (card.explanation.isNotBlank()) {
                sb.appendLine()
                sb.appendLine("**解析**: ${card.explanation}")
            }
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun cardTypeLabel(card: com.sunjk.sunjktool.domain.model.Flashcard): String = when (card) {
        is com.sunjk.sunjktool.domain.model.Flashcard.TrueFalse -> "判断题"
        is com.sunjk.sunjktool.domain.model.Flashcard.SingleChoice -> "单选题"
        is com.sunjk.sunjktool.domain.model.Flashcard.MultiChoice -> "多选题"
        is com.sunjk.sunjktool.domain.model.Flashcard.Memory -> "识记卡片"
    }

    private fun saveCurrentAttempt() {
        val sessionId = _uiState.value.session?.id ?: return
        val cardIndex = _uiState.value.currentCardIndex
        val answer = _uiState.value.userAnswers[cardIndex] ?: return
        viewModelScope.launch { repository.recordAttempt(sessionId, cardIndex, answer) }
    }

    /** Record KP stats for the just-answered card */
    private fun recordCurrentCardKP() {
        val card = currentCard ?: return
        if (card.knowledgePoint.isBlank()) return
        val ans = _uiState.value.userAnswers[_uiState.value.currentCardIndex] ?: return
        val isCorrect = when (ans) {
            is UserAnswer.TrueFalse -> ans.isCorrect
            is UserAnswer.SingleChoice -> ans.isCorrect
            is UserAnswer.MultiChoiceChoice -> ans.isCorrect
            is UserAnswer.Memory -> ans.known
        }
        viewModelScope.launch {
            knowledgePointStatsRepository.recordAnswer(logEntryId, card.knowledgePoint, isCorrect)
        }
    }

    fun prevCard() {
        if (_uiState.value.currentCardIndex > 0) {
            _uiState.update { it.copy(currentCardIndex = it.currentCardIndex - 1, showExplanation = false) }
        }
    }

    val correctCount: Int
        get() = _uiState.value.userAnswers.values.count {
            when (it) {
                is UserAnswer.TrueFalse -> it.isCorrect
                is UserAnswer.SingleChoice -> it.isCorrect
                is UserAnswer.MultiChoiceChoice -> it.isCorrect
                is UserAnswer.Memory -> false
            }
        }

    val wrongCards: List<Pair<Int, Flashcard>>
        get() = _uiState.value.session?.cards?.mapIndexedNotNull { idx, card ->
            val ans = _uiState.value.userAnswers[idx] ?: return@mapIndexedNotNull null
            val isWrong = when (ans) {
                is UserAnswer.TrueFalse -> !ans.isCorrect
                is UserAnswer.SingleChoice -> !ans.isCorrect
                is UserAnswer.MultiChoiceChoice -> !ans.isCorrect
                is UserAnswer.Memory -> false
            }
            if (isWrong) idx to card else null
        } ?: emptyList()
}
