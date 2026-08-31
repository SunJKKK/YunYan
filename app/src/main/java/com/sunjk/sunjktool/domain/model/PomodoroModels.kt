package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable

enum class PomodoroPhase { IDLE, FOCUS, BREAK }

@Stable
data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val remainingSecs: Int = 0,
    val totalSecs: Int = 0,
    val isRunning: Boolean = false,
    val workMinutes: Int = 30,
    val breakMinutes: Int = 20,
    val skipBreak: Boolean = false,
    val totalFocusSecs: Long = 0L,
    val completedCount: Int = 0,
    val lastCompletedDate: String = "", // yyyy-MM-dd for daily reset
    val pendingStopSecs: Int = 0, // >0 means awaiting user confirmation to count elapsed time
)
