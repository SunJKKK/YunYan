package com.sunjk.sunjktool.feature.questionbank.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.QuestionBankCategory
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class QuestionBankListUiState(
    val rootCategories: List<QuestionBankCategory> = emptyList(),
    val isLoading: Boolean = true,
    val deleteConfirmId: Long? = null
)

class QuestionBankListViewModel(
    private val repository: QuestionBankRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionBankListUiState())
    val uiState: StateFlow<QuestionBankListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRootCategories().collect { roots ->
                _uiState.update { it.copy(rootCategories = roots, isLoading = false) }
            }
        }
    }

    fun requestDelete(categoryId: Long) {
        _uiState.update { it.copy(deleteConfirmId = categoryId) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            repository.deleteCategory(id)
            _uiState.update { it.copy(deleteConfirmId = null) }
            SyncTrigger.bumpEntity("question_bank_categories")
            SyncTrigger.requestAutoSync()
        }
    }
}
