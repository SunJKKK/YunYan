package com.sunjk.sunjktool.feature.learninglog.edit

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
data class LogEditUiState(
    val subject: String = "",
    val title: String = "",
    val timeSpent: String = "",
    val imageUri: Uri? = null,
    val imagePath: String? = null,

    // Validation
    val titleError: String? = null,

    // Status
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val error: String? = null
)

class LogEditViewModel(
    private val repository: LogRepository,
    private val logId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogEditUiState())
    val uiState: StateFlow<LogEditUiState> = _uiState.asStateFlow()

    // Track original loaded values for "unsaved changes" detection
    private var originalTitle = ""
    private var originalSubject = ""
    private var originalTimeSpent = ""
    private var originalImagePath: String? = null

    init {
        if (logId != null) {
            loadExisting(logId)
        } else {
            _uiState.update { it.copy(isEditMode = false, isLoading = false) }
        }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { entry ->
                if (entry != null) {
                    originalTitle = entry.title
                    originalSubject = entry.subject
                    originalTimeSpent = if (entry.timeSpent > 0) entry.timeSpent.toString() else ""
                    originalImagePath = entry.imagePath

                    _uiState.update {
                        it.copy(
                            subject = entry.subject,
                            title = entry.title,
                            timeSpent = if (entry.timeSpent > 0) entry.timeSpent.toString() else "",
                            imagePath = entry.imagePath,
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "日志不存在", isLoading = false) }
                }
            }
        }
    }

    fun updateSubject(value: String) {
        _uiState.update { it.copy(subject = value) }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun updateTimeSpent(value: String) {
        // Only allow digits
        if (value.all { it.isDigit() } || value.isEmpty()) {
            _uiState.update { it.copy(timeSpent = value) }
        }
    }

    fun updateImageUri(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    fun updateImagePath(path: String?) {
        _uiState.update { it.copy(imagePath = path) }
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "标题不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val now = LocalDateTime.now()
                val entry = LogEntry(
                    id = logId ?: 0L,
                    subject = state.subject.trim(),
                    title = state.title.trim(),
                    timeSpent = state.timeSpent.toIntOrNull() ?: 0,
                    imagePath = state.imagePath,
                    createdDate = now,
                    updatedDate = now
                )
                repository.saveEntry(entry)
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "保存失败: ${e.message}") }
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val s = _uiState.value
        if (s.isSaving || s.saveComplete) return false
        return s.title != originalTitle ||
                s.subject != originalSubject ||
                s.timeSpent != originalTimeSpent ||
                s.imagePath != originalImagePath
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
