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
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class HabitWithStatus(
    val habit: Habit,
    val isCompleted: Boolean,
    /** 当前连续打卡天数（含今天，若今天未打卡则从昨天起算） */
    val streakDays: Int = 0,
    /** 近7天打卡情况，index 0 = 6天前，index 6 = 今天 */
    val last7Days: List<Boolean> = List(7) { false },
    /** 累计打卡总次数 */
    val totalCheckIns: Int = 0
)

@Immutable
data class HabitListUiState(
    val isLoading: Boolean = true,
    val items: List<HabitWithStatus> = emptyList()
) {
    val completedToday: Int get() = items.count { it.isCompleted }
}

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
            ) { habits, records ->
                val today = LocalDate.now()
                val byHabit = records.filter { it.isCompleted }
                    .groupBy { it.habitId }
                    .mapValues { (_, list) ->
                        list.mapNotNull { runCatching { LocalDate.parse(it.date.substringAfter('_')) }.getOrNull() }.toSet()
                    }
                val items = habits.map { habit ->
                    val dates = byHabit[habit.id] ?: emptySet()
                    HabitWithStatus(
                        habit = habit,
                        isCompleted = today in dates,
                        streakDays = computeStreak(dates, today),
                        last7Days = (6 downTo 0).map { d -> today.minusDays(d.toLong()) in dates },
                        totalCheckIns = dates.size
                    )
                }
                HabitListUiState(isLoading = false, items = items)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun computeStreak(dates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in dates) today else today.minusDays(1)
        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            repository.toggleRecord(habitId, LocalDate.now())
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.delete(habitId)
        }
    }
}
