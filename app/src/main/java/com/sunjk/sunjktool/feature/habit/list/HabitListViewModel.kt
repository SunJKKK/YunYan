package com.sunjk.sunjktool.feature.habit.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunjk.sunjktool.domain.model.Habit
import com.sunjk.sunjktool.domain.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitWithStatus(
    val habit: Habit,
    val isCompleted: Boolean
)

@Immutable
data class HabitListUiState(
    val isLoading: Boolean = true,
    val items: List<HabitWithStatus> = emptyList()
)

class HabitListViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitListUiState())
    val uiState: StateFlow<HabitListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAll(),
                repository.getAllRecords()
            ) { habits, _ ->
                val today = LocalDate.now()
                val items = habits.map { habit ->
                    HabitWithStatus(
                        habit = habit,
                        isCompleted = repository.isCompleted(habit.id, today)
                    )
                }
                HabitListUiState(isLoading = false, items = items)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            val today = LocalDate.now()
            repository.toggleRecord(habitId, today)
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.delete(habitId)
        }
    }
}
