package com.sunjk.sunjktool.feature.notebook.detail

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.Notebook
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class BreadcrumbItem(
    val notebookId: Long,
    val name: String
)

@Immutable
data class NotebookDetailUiState(
    val notebook: Notebook? = null,
    val subNotebooks: List<Notebook> = emptyList(),
    val logEntries: List<LogEntry> = emptyList(),
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val isLoading: Boolean = true,
    val deleteConfirmId: Long? = null
)

class NotebookDetailViewModel(
    private val notebookRepository: NotebookRepository,
    private val logRepository: LogRepository,
    private val notebookId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotebookDetailUiState())
    val uiState: StateFlow<NotebookDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load notebook
            launch {
                notebookRepository.getById(notebookId).collect { notebook ->
                    _uiState.update { it.copy(notebook = notebook, isLoading = false) }
                }
            }
            // Load children
            launch {
                notebookRepository.getChildren(notebookId).collect { children ->
                    _uiState.update { it.copy(subNotebooks = children) }
                }
            }
            // Load log entries
            launch {
                logRepository.getEntriesByNotebookId(notebookId).collect { entries ->
                    _uiState.update { it.copy(logEntries = entries) }
                }
            }
            // Load breadcrumbs
            launch {
                val crumbs = notebookRepository.getBreadcrumbs(notebookId)
                    .map { BreadcrumbItem(it.first, it.second) }
                _uiState.update { it.copy(breadcrumbs = crumbs) }
            }
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(deleteConfirmId = notebookId) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirmId = null) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            notebookRepository.delete(notebookId)
            _uiState.update { it.copy(deleteConfirmId = null) }
            SyncTrigger.bumpEntity("notebooks")
            SyncTrigger.requestAutoSync()
        }
    }
}
