package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDate
import java.time.LocalDateTime

@Stable
data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorArgb: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Stable
data class HabitRecord(
    val date: LocalDate,
    val habitId: Long,
    val isCompleted: Boolean = false,
    val updatedAt: LocalDateTime
)
