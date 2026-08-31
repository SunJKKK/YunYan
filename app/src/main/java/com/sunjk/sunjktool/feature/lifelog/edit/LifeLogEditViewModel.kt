package com.sunjk.sunjktool.feature.lifelog.edit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.LifeLogEntry
import com.sunjk.sunjktool.domain.repository.LifeLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
data class LifeLogEditUiState(
    val content: String = "",
    val selectedMoods: List<String> = emptyList(),
    val imagePaths: List<String> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val contentError: String? = null,
    val error: String? = null
)

class LifeLogEditViewModel(
    private val repository: LifeLogRepository,
    private val entryId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifeLogEditUiState())
    val uiState: StateFlow<LifeLogEditUiState> = _uiState.asStateFlow()

    private var originalContent = ""
    private var originalMoods: List<String> = emptyList()
    private var originalImagePaths: List<String> = emptyList()
    private var originalCreatedDate = LocalDateTime.now()

    init {
        if (entryId != null) {
            loadExisting(entryId)
        } else {
            _uiState.update { it.copy(isEditMode = false, isLoading = false) }
        }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { entry ->
                if (entry != null) {
                    originalContent = entry.content
                    originalMoods = entry.moods
                    originalImagePaths = entry.imagePaths
                    originalCreatedDate = entry.createdDate

                    _uiState.update {
                        it.copy(
                            content = entry.content,
                            selectedMoods = entry.moods,
                            imagePaths = entry.imagePaths,
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "记录不存在", isLoading = false) }
                }
            }
        }
    }

    fun updateContent(value: String) {
        _uiState.update { it.copy(content = value, contentError = null) }
    }

    fun toggleMood(moodKey: String) {
        _uiState.update { state ->
            val current = state.selectedMoods.toMutableList()
            if (current.contains(moodKey)) current.remove(moodKey)
            else current.add(moodKey)
            state.copy(selectedMoods = current)
        }
    }

    fun addImagePath(path: String) {
        _uiState.update { it.copy(imagePaths = it.imagePaths + path) }
    }

    fun removeImagePath(index: Int) {
        _uiState.update { state ->
            state.copy(imagePaths = state.imagePaths.toMutableList().also { it.removeAt(index) })
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.content.isBlank()) {
            _uiState.update { it.copy(contentError = "内容不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val now = LocalDateTime.now()
                val entry = LifeLogEntry(
                    id = entryId ?: 0L,
                    content = state.content.trim(),
                    moods = state.selectedMoods,
                    imagePaths = state.imagePaths,
                    createdDate = if (entryId != null) originalCreatedDate else now,
                    updatedDate = now
                )
                repository.saveEntry(entry)
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
                SyncTrigger.bumpEntity("life_log_entries")
                SyncTrigger.requestAutoSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "保存失败: ${e.message}") }
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val s = _uiState.value
        if (s.isSaving || s.saveComplete) return false
        return s.content != originalContent ||
                s.selectedMoods != originalMoods ||
                s.imagePaths != originalImagePaths
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
