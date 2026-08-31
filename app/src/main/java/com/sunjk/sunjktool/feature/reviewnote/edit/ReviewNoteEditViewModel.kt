package com.sunjk.sunjktool.feature.reviewnote.edit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.ReviewNote
import com.sunjk.sunjktool.domain.model.ReviewNoteSource
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

@Immutable
data class ReviewNoteEditUiState(
    val content: String = "",
    val imagePaths: List<String> = emptyList(),
    val sourceType: ReviewNoteSource = ReviewNoteSource.MANUAL,
    val flashcardSessionId: Long? = null,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val contentError: Boolean = false
)

class ReviewNoteEditViewModel(
    private val repository: ReviewNoteRepository,
    private val logEntryId: Long,
    private val noteId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewNoteEditUiState())
    val uiState: StateFlow<ReviewNoteEditUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null) {
            viewModelScope.launch {
                repository.getById(noteId).collect { note ->
                    if (note != null) {
                        _uiState.update {
                            it.copy(
                                content = note.content,
                                imagePaths = note.imagePaths,
                                sourceType = note.sourceType,
                                flashcardSessionId = note.flashcardSessionId,
                                isEditMode = true,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateContent(text: String) {
        _uiState.update { it.copy(content = text, contentError = false) }
    }

    fun addImagePath(path: String) {
        _uiState.update { it.copy(imagePaths = it.imagePaths + path) }
    }

    fun removeImagePath(index: Int) {
        val paths = _uiState.value.imagePaths.toMutableList()
        if (index in paths.indices) {
            val removed = paths.removeAt(index)
            viewModelScope.launch(Dispatchers.IO) {
                try { File(removed).delete() } catch (_: Exception) {}
            }
        }
        _uiState.update { it.copy(imagePaths = paths) }
    }

    fun save() {
        val state = _uiState.value
        if (state.content.isBlank()) {
            _uiState.update { it.copy(contentError = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val note = if (noteId != null) {
                val existing = repository.getById(noteId).first()
                (existing ?: ReviewNote(
                    logEntryId = logEntryId,
                    content = state.content,
                    imagePaths = state.imagePaths,
                    sourceType = state.sourceType
                )).copy(
                    content = state.content,
                    imagePaths = state.imagePaths,
                    updatedDate = LocalDateTime.now()
                )
            } else {
                ReviewNote(
                    logEntryId = logEntryId,
                    content = state.content,
                    imagePaths = state.imagePaths,
                    sourceType = state.sourceType,
                    createdDate = LocalDateTime.now(),
                    updatedDate = LocalDateTime.now()
                )
            }
            repository.save(note)
            SyncTrigger.requestAutoSync()
            SyncTrigger.bumpEntity("review_notes")
            _uiState.update { it.copy(isSaving = false, saveComplete = true) }
        }
    }
}
