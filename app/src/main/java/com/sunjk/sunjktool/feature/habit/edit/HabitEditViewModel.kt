package com.sunjk.sunjktool.feature.habit.edit

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
data class HabitEditUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val name: String = "",
    val description: String = "",
    val colorArgb: Int = 0xFF6750A4.toInt(),  // M3 primary default
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false
)

class HabitEditViewModel(
    private val repository: HabitRepository,
    private val habitId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitEditUiState())
    val uiState: StateFlow<HabitEditUiState> = _uiState.asStateFlow()

    init {
        if (habitId != null) {
            viewModelScope.launch {
                repository.getById(habitId).collect { habit ->
                    if (habit != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEditMode = true,
                                name = habit.name,
                                description = habit.description,
                                colorArgb = habit.colorArgb
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun updateColor(argb: Int) {
        _uiState.update { it.copy(colorArgb = argb) }
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
                val now = LocalDateTime.now()
                val habit = com.sunjk.sunjktool.domain.model.Habit(
                    id = habitId ?: 0L,
                    name = state.name.trim(),
                    description = state.description.trim(),
                    colorArgb = state.colorArgb,
                    createdAt = if (habitId != null) now else now,
                    updatedAt = now
                )
                repository.save(habit)
                _uiState.update { it.copy(isSaving = false, saveComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
