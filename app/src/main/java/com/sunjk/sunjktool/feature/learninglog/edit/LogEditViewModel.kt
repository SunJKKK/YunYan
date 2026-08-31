package com.sunjk.sunjktool.feature.learninglog.edit
import android.content.Context

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.Notebook
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.util.AttachmentTextExtractor
import com.sunjk.sunjktool.util.ReviewHelper
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
    val description: String = "",
    val timeSpent: String = "",
    val imagePaths: List<String> = emptyList(),
    val attachmentPaths: List<String> = emptyList(),
    val attachmentText: String = "",
    val notebookId: Long? = null,
    val notebookName: String? = null,
    val allNotebooks: List<Notebook> = emptyList(),

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
    private val reviewHelper: ReviewHelper,
    private val notebookRepository: NotebookRepository,
    private val logId: Long?,
    private val preSelectedNotebookId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogEditUiState())
    val uiState: StateFlow<LogEditUiState> = _uiState.asStateFlow()

    private var originalTitle = ""
    private var originalSubject = ""
    private var originalDescription = ""
    private var originalTimeSpent = ""
    private var originalImagePaths: List<String> = emptyList()
    private var originalAttachmentPaths: List<String> = emptyList()
    private var originalAttachmentText: String = ""
    private var originalNotebookId: Long? = null
    private var originalAiSummary = ""
    private var originalCreatedDate = LocalDateTime.now()

    init {
        // Load all notebooks for the picker
        viewModelScope.launch {
            notebookRepository.getAll().collect { notebooks ->
                _uiState.update { it.copy(allNotebooks = notebooks) }
            }
        }

        // Pre-select notebook if provided
        if (preSelectedNotebookId != null) {
            viewModelScope.launch {
                notebookRepository.getById(preSelectedNotebookId).collect { notebook ->
                    if (notebook != null) {
                        _uiState.update {
                            it.copy(notebookId = notebook.id, notebookName = notebook.name)
                        }
                    }
                }
            }
        }

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
                    originalDescription = entry.description
                    originalTimeSpent = if (entry.timeSpent > 0) entry.timeSpent.toString() else ""
                    originalImagePaths = entry.imagePaths
                    originalAttachmentPaths = entry.attachmentPaths
                    originalAttachmentText = entry.attachmentText
                    originalCreatedDate = entry.createdDate
                    originalNotebookId = entry.notebookId
                    originalAiSummary = entry.aiSummary

                    _uiState.update {
                        it.copy(
                            subject = entry.subject,
                            title = entry.title,
                            description = entry.description,
                            timeSpent = if (entry.timeSpent > 0) entry.timeSpent.toString() else "",
                            imagePaths = entry.imagePaths,
                            attachmentPaths = entry.attachmentPaths,
                            attachmentText = entry.attachmentText,
                            notebookId = entry.notebookId,
                            isEditMode = true,
                            isLoading = false
                        )
                    }

                    // Load notebook name if set
                    if (entry.notebookId != null) {
                        notebookRepository.getById(entry.notebookId).collect { nb ->
                            if (nb != null) {
                                _uiState.update { it.copy(notebookName = nb.name) }
                            }
                        }
                    }
                } else {
                    _uiState.update { it.copy(error = "记录不存在", isLoading = false) }
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
        if (value.all { it.isDigit() } || value.isEmpty()) {
            _uiState.update { it.copy(timeSpent = value) }
        }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun addImagePath(path: String) {
        _uiState.update { it.copy(imagePaths = it.imagePaths + path) }
    }

    fun selectNotebook(id: Long?, name: String?) {
        _uiState.update { it.copy(notebookId = id, notebookName = name) }
    }

    fun removeImagePath(index: Int) {
        _uiState.update { state ->
            state.copy(imagePaths = state.imagePaths.toMutableList().also { it.removeAt(index) })
        }
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
                    description = state.description.trim(),
                    timeSpent = state.timeSpent.toIntOrNull() ?: 0,
                    imagePaths = state.imagePaths,
                    attachmentPaths = state.attachmentPaths,
                    attachmentText = state.attachmentText,
                    notebookId = state.notebookId,
                    aiSummary = if (logId != null) originalAiSummary else "",
                    createdDate = if (logId != null) originalCreatedDate else now,
                    updatedDate = now
                )
                val savedId = repository.saveEntry(entry)
                val savedEntry = entry.copy(id = savedId)
                // 编辑时先清除旧复盘任务，再重新生成
                if (logId != null) {
                    reviewHelper.deleteByEntryId(logId)
                }
                reviewHelper.generateFor(savedEntry, skipExisting = false)
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
                SyncTrigger.bumpEntity("review_status")
                SyncTrigger.requestAutoSync()
                SyncTrigger.bumpEntity("log_entries")
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
                s.description != originalDescription ||
                s.timeSpent != originalTimeSpent ||
                s.imagePaths != originalImagePaths ||
                s.attachmentPaths != originalAttachmentPaths ||
                s.attachmentText != originalAttachmentText ||
                s.notebookId != originalNotebookId
    }


    fun addAttachmentPaths(context: Context, paths: List<String>) {
        _uiState.update { it.copy(attachmentPaths = (it.attachmentPaths + paths).distinct()) }
        viewModelScope.launch {
            val extracted = paths.mapNotNull { path ->
                AttachmentTextExtractor.extract(context, path).takeIf { it.isNotBlank() }
            }.joinToString("\n\n")
            if (extracted.isNotBlank()) {
                val current = _uiState.value.attachmentText
                val merged = if (current.isBlank()) extracted else current + "\n\n" + extracted
                _uiState.update { it.copy(attachmentText = merged) }
            }
        }
    }

    fun removeAttachmentPath(index: Int) {
        _uiState.update { state ->
            state.copy(attachmentPaths = state.attachmentPaths.toMutableList().also { it.removeAt(index) })
        }
    }

    fun setAttachmentText(text: String) {
        _uiState.update { it.copy(attachmentText = text) }
    }
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
