package com.sunjk.sunjktool.feature.pomodoro

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.sunjk.sunjktool.data.model.PomodoroRecordEntity
import com.sunjk.sunjktool.domain.model.PomodoroState
import com.sunjk.sunjktool.util.PomodoroManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class PomodoroUiState(
    val pomodoroState: PomodoroState = PomodoroState(),
    val workMinutes: Int = 30,
    val breakMinutes: Int = 20,
    val skipBreak: Boolean = false,
)

class PomodoroViewModel(
    private val pomodoroManager: PomodoroManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    val managerState: StateFlow<PomodoroState> = pomodoroManager.state
    val records: Flow<List<PomodoroRecordEntity>>? = pomodoroManager.recordDao?.getAll()

    init {
        _uiState.value = _uiState.value.copy(skipBreak = pomodoroManager.state.value.skipBreak)
    }

    fun setWorkMinutes(min: Int) {
        _uiState.value = _uiState.value.copy(workMinutes = min.coerceIn(5, 120))
    }

    fun setBreakMinutes(min: Int) {
        _uiState.value = _uiState.value.copy(breakMinutes = min.coerceIn(5, 60))
    }

    fun setSkipBreak(skip: Boolean) {
        _uiState.value = _uiState.value.copy(skipBreak = skip)
        pomodoroManager.setSkipBreak(skip)
    }

    fun start() {
        val s = _uiState.value
        pomodoroManager.start(s.workMinutes, s.breakMinutes, s.skipBreak)
    }

    fun pause() = pomodoroManager.pause()
    fun resume() = pomodoroManager.resume()
    fun preStop() = pomodoroManager.preStop()
    fun confirmStop(keep: Boolean) = pomodoroManager.confirmStop(keep)
    fun stop() = pomodoroManager.stop()
}
