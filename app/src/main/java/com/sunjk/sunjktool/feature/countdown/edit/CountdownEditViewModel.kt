package com.sunjk.sunjktool.feature.countdown.edit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.Countdown
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@Immutable
data class CountdownEditUiState(
    val title: String = "",
    val targetDate: LocalDate? = null,
    val note: String = "",
    val titleError: String? = null,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false
)

class CountdownEditViewModel(
    private val repository: CountdownRepository,
    private val countdownId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CountdownEditUiState())
    val uiState: StateFlow<CountdownEditUiState> = _uiState.asStateFlow()

    init {
        if (countdownId != null) {
            loadExisting(countdownId)
        } else {
            _uiState.update { it.copy(isEditMode = false, isLoading = false) }
        }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            repository.getById(id).collect { countdown ->
                if (countdown != null) {
                    _uiState.update {
                        it.copy(
                            title = countdown.title,
                            targetDate = countdown.targetDate,
                            note = countdown.note,
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun updateTargetDate(date: LocalDate) {
        _uiState.update { it.copy(targetDate = date) }
    }

    fun updateNote(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "标题不能为空") }
            return
        }
        if (state.targetDate == null) {
            _uiState.update { it.copy(titleError = "请选择目标日期") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val now = LocalDateTime.now()
                val countdown = Countdown(
                    id = countdownId ?: 0L,
                    title = state.title.trim(),
                    targetDate = state.targetDate!!,
                    note = state.note.trim(),
                    createdDate = now,
                    updatedDate = now
                )
                repository.save(countdown)
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
                SyncTrigger.requestAutoSync()
                SyncTrigger.bumpEntity("countdowns")
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
