package com.sunjk.sunjktool.feature.notebook.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.Notebook
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class NotebookListUiState(
    val rootNotebooks: List<Notebook> = emptyList(),
    val isLoading: Boolean = true,
    val deleteConfirmId: Long? = null
)

class NotebookListViewModel(
    private val notebookRepository: NotebookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotebookListUiState())
    val uiState: StateFlow<NotebookListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notebookRepository.getRoots().collect { roots ->
                _uiState.update { it.copy(rootNotebooks = roots, isLoading = false) }
            }
        }
    }

    fun requestDelete(notebookId: Long) {
        _uiState.update { it.copy(deleteConfirmId = notebookId) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            notebookRepository.delete(id)
            _uiState.update { it.copy(deleteConfirmId = null) }
            SyncTrigger.bumpEntity("notebooks")
            SyncTrigger.requestAutoSync()
        }
    }
}
