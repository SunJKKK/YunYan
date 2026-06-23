package com.sunjk.sunjktool.feature.learninglog.detail

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

@Immutable
data class LogDetailUiState(
    val entry: LogEntry? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null
)

class LogDetailViewModel(
    private val repository: LogRepository,
    private val logId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogDetailUiState())
    val uiState: StateFlow<LogDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getEntryById(logId).collect { entry ->
                _uiState.update { it.copy(entry = entry, isLoading = false) }
            }
        }
    }

    fun deleteEntry() {
        viewModelScope.launch {
            try {
                _uiState.value.entry?.imagePath?.let {
                    com.sunjk.sunjktool.util.ImageUtil.deleteInternal(it)
                }
                repository.deleteEntry(logId)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
