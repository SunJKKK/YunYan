package com.sunjk.sunjktool.feature.reviewnote.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.ReviewNote
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ReviewNoteListUiState(
    val notes: List<ReviewNote> = emptyList(),
    val isLoading: Boolean = true,
    val deleteConfirmId: Long? = null
)

class ReviewNoteListViewModel(
    private val repository: ReviewNoteRepository,
    private val logEntryId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewNoteListUiState())
    val uiState: StateFlow<ReviewNoteListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getByLogEntryId(logEntryId).collect { notes ->
                _uiState.update { it.copy(notes = notes, isLoading = false) }
            }
        }
    }

    fun requestDelete(noteId: Long) {
        _uiState.update { it.copy(deleteConfirmId = noteId) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            repository.delete(id)
            SyncTrigger.requestAutoSync()
            SyncTrigger.bumpEntity("review_notes")
            _uiState.update { it.copy(deleteConfirmId = null) }
        }
    }
}
