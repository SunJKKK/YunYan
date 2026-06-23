package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDate
import java.time.LocalDateTime

@Stable
data class Countdown(
    val id: Long = 0,
    val title: String,
    val targetDate: LocalDate,
    val note: String = "",
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)
