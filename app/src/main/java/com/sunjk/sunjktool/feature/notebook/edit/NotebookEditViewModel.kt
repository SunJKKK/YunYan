package com.sunjk.sunjktool.feature.notebook.edit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.Notebook
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
data class NotebookEditUiState(
    val name: String = "",
    val parentId: Long? = null,
    val parentName: String = "无（根目录）",
    val icon: String = "folder",
    val pinned: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val nameError: String? = null,
    val showParentPicker: Boolean = false,
    val allNotebooks: List<Notebook> = emptyList(),
    val excludeNotebookIds: Set<Long> = emptySet()
)

class NotebookEditViewModel(
    private val notebookRepository: NotebookRepository,
    private val notebookId: Long?,
    private val parentId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotebookEditUiState())
    val uiState: StateFlow<NotebookEditUiState> = _uiState.asStateFlow()

    private var originalName = ""

    init {
        // Load all notebooks for the picker
        viewModelScope.launch {
            notebookRepository.getAll().collect { notebooks ->
                _uiState.update { it.copy(allNotebooks = notebooks) }
            }
        }

        if (notebookId != null) {
            loadExisting(notebookId)
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
            notebookRepository.getById(id).collect { notebook ->
                if (notebook != null) {
                    originalName = notebook.name
                    _uiState.update {
                        it.copy(
                            name = notebook.name,
                            parentId = notebook.parentId,
                            icon = notebook.icon,
                            pinned = notebook.pinned,
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                    if (notebook.parentId != null) {
                        loadParentName(notebook.parentId)
                    }
                    // Compute exclude IDs (self + all descendants)
                    val descendants = notebookRepository.getDescendantIds(id)
                    _uiState.update {
                        it.copy(excludeNotebookIds = descendants + id)
                    }
                }
            }
        }
    }

    private fun loadParentName(pid: Long) {
        viewModelScope.launch {
            notebookRepository.getById(pid).collect { parent ->
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

    fun updateIcon(name: String) {
        _uiState.update { it.copy(icon = name) }
    }

    fun togglePinned() {
        _uiState.update { it.copy(pinned = !it.pinned) }
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
                val notebook = Notebook(
                    id = notebookId ?: 0L,
                    name = state.name.trim(),
                    parentId = state.parentId,
                    icon = state.icon,
                    pinned = state.pinned,
                    createdDate = LocalDateTime.now(),
                    updatedDate = LocalDateTime.now()
                )
                notebookRepository.save(notebook)
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
                SyncTrigger.bumpEntity("notebooks")
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
