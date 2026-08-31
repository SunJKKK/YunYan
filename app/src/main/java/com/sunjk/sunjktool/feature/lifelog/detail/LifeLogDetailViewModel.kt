package com.sunjk.sunjktool.feature.lifelog.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.LifeLogEntry
import com.sunjk.sunjktool.domain.repository.LifeLogRepository
import com.sunjk.sunjktool.util.ImageUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class LifeLogDetailUiState(
    val entry: LifeLogEntry? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val deleteConfirmId: Long? = null,
    val error: String? = null
)

class LifeLogDetailViewModel(
    private val repository: LifeLogRepository,
    private val entryId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifeLogDetailUiState())
    val uiState: StateFlow<LifeLogDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getEntryById(entryId).collect { entry ->
                _uiState.update { it.copy(entry = entry, isLoading = false) }
            }
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(deleteConfirmId = entryId) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _uiState.value.entry?.imagePaths?.forEach { ImageUtil.deleteInternal(it) }
            repository.deleteEntry(entryId)
            _uiState.update { it.copy(isDeleted = true) }
            SyncTrigger.bumpEntity("life_log_entries")
            SyncTrigger.requestAutoSync()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
