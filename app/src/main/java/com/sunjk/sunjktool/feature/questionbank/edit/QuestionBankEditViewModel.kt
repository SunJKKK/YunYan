package com.sunjk.sunjktool.feature.questionbank.edit

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
import java.time.LocalDateTime

@Immutable
data class QuestionBankEditUiState(
    val name: String = "",
    val parentId: Long? = null,
    val parentName: String = "无（根目录）",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val nameError: String? = null,
    val showParentPicker: Boolean = false,
    val allCategories: List<QuestionBankCategory> = emptyList(),
    val excludeCategoryIds: Set<Long> = emptySet()
)

class QuestionBankEditViewModel(
    private val repository: QuestionBankRepository,
    private val categoryId: Long?,
    private val parentId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionBankEditUiState())
    val uiState: StateFlow<QuestionBankEditUiState> = _uiState.asStateFlow()

    private var originalName = ""

    init {
        viewModelScope.launch {
            repository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(allCategories = categories) }
            }
        }

        if (categoryId != null) {
            loadExisting(categoryId)
        } else {
            _uiState.update {
                it.copy(
                    parentId = parentId,
                    isEditMode = false,
                    isLoading = false
                )
            }
            if (parentId != null) {
                loadParentName(parentId)
            }
        }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            repository.getCategoryById(id).collect { category ->
                if (category != null) {
                    originalName = category.name
                    _uiState.update {
                        it.copy(
                            name = category.name,
                            parentId = category.parentId,
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                    category.parentId?.let { loadParentName(it) }
                    val descendants = repository.getDescendantIds(id)
                    _uiState.update { it.copy(excludeCategoryIds = descendants + id) }
                }
            }
        }
    }

    private fun loadParentName(pid: Long) {
        viewModelScope.launch {
            repository.getCategoryById(pid).collect { parent ->
                if (parent != null) {
                    _uiState.update { it.copy(parentName = parent.name) }
                }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun selectParent(id: Long?, name: String) {
        _uiState.update { it.copy(parentId = id, parentName = name, showParentPicker = false) }
    }

    fun showParentPicker() {
        _uiState.update { it.copy(showParentPicker = true) }
    }

    fun hideParentPicker() {
        _uiState.update { it.copy(showParentPicker = false) }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "名称不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val category = QuestionBankCategory(
                    id = categoryId ?: 0L,
                    name = state.name.trim(),
                    parentId = state.parentId,
                    createdDate = LocalDateTime.now(),
                    updatedDate = LocalDateTime.now()
                )
                repository.saveCategory(category)
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
                SyncTrigger.bumpEntity("question_bank_categories")
                SyncTrigger.requestAutoSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, nameError = "保存失败: ${e.message}") }
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val s = _uiState.value
        if (s.isSaving || s.saveComplete) return false
        return s.name != originalName
    }
}
