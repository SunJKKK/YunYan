package com.sunjk.sunjktool.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatDateTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val date = dateTime.toLocalDate()
    val today = now.toLocalDate()
    val yesterday = today.minusDays(1)

    return when {
        date == today -> "今天 ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        date == yesterday -> "昨天 ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        date.year == today.year ->
            dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        else ->
            dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}

fun formatDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "今天"
        date == today.minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("MM-dd"))
    }
}
