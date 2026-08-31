package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime

@Stable
data class LogEntry(
    val id: Long = 0,
    val subject: String = "",
    val title: String,
    val timeSpent: Int = 0,
    val imagePaths: List<String> = emptyList(),
    val description: String = "",
    val aiSummary: String = "",
    val selfCheckContent: String = "",
    val mindMapJson: String = "",
    val attachmentPaths: List<String> = emptyList(),
    val attachmentText: String = "",
    val notebookId: Long? = null,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encodePaths(paths: List<String>): String =
            json.encodeToString(paths)

        fun decodePaths(raw: String?): List<String> =
            if (raw.isNullOrBlank()) emptyList()
            else try { json.decodeFromString<List<String>>(raw) } catch (_: Exception) { emptyList() }
    }
}

@Stable
data class TimelineDay(
    val date: LocalDate,
    val entries: List<LogEntry>
)
