package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

@Stable
data class LogEntry(
    val id: Long = 0,
    val subject: String = "",
    val title: String,
    val timeSpent: Int = 0,
    val imagePath: String? = null,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)
