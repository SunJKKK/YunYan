package com.sunjk.sunjktool.feature.reviewnote.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.ReviewNote
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

@Immutable
data class ReviewNoteDetailUiState(
    val note: ReviewNote? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val deleteConfirmVisible: Boolean = false
)

class ReviewNoteDetailViewModel(
    private val repository: ReviewNoteRepository,
    private val noteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewNoteDetailUiState())
    val uiState: StateFlow<ReviewNoteDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getById(noteId).collect { note ->
                _uiState.update { it.copy(note = note, isLoading = false) }
            }
        }
    }

    fun showDeleteConfirm() {
        _uiState.update { it.copy(deleteConfirmVisible = true) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(deleteConfirmVisible = false) }
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.note?.imagePaths?.forEach { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
            repository.delete(noteId)
            SyncTrigger.requestAutoSync()
            SyncTrigger.bumpEntity("review_notes")
            _uiState.update { it.copy(isDeleted = true, deleteConfirmVisible = false) }
        }
    }
}
