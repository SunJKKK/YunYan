package com.sunjk.sunjktool.feature.questionbank.link

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.Question
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.util.QuestionLinkRef
import com.sunjk.sunjktool.util.SummaryLinkHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class QuestionLinkListUiState(
    val isLoading: Boolean = true,
    val items: List<QuestionLinkRef> = emptyList(),
    val error: String? = null
)

/**
 * 反向关联列表页：展示"引用当前学习记录某一章节"的所有题目。
 * 数据来自对全部题目解析正文的运行时扫描（无需新建表/同步）。
 */
class QuestionLinkListViewModel(
    private val repository: QuestionBankRepository,
    private val logId: Long,
    private val headingId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionLinkListUiState())
    val uiState: StateFlow<QuestionLinkListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val categories = repository.getAllCategories().first()
                val nameOf = { categoryId: Long ->
                    categories.firstOrNull { it.id == categoryId }?.name ?: "题集"
                }
                val allQuestions = repository.getAllQuestions().first()
                val items = allQuestions.mapNotNull { q: Question ->
                    val referenced = SummaryLinkHelper.extractInternalLinks(q.aiAnalysis)
                        .any { it.logEntryId == logId && it.headingId == headingId }
                    if (referenced) {
                        QuestionLinkRef(
                            questionId = q.id,
                            categoryId = q.categoryId,
                            categoryName = nameOf(q.categoryId),
                            content = q.content
                        )
                    } else null
                }
                _uiState.update { it.copy(isLoading = false, items = items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
