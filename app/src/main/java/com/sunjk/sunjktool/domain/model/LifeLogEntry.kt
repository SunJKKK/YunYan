package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime

@Stable
data class LifeLogEntry(
    val id: Long = 0,
    val content: String = "",
    val moods: List<String> = emptyList(),
    val imagePaths: List<String> = emptyList(),
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

        fun encodeMoods(moods: List<String>): String =
            moods.joinToString(",")

        fun decodeMoods(raw: String): List<String> =
            raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Stable
data class LifeLogTimelineDay(
    val date: LocalDate,
    val entries: List<LifeLogEntry>
)
