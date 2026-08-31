package com.sunjk.sunjktool.data.sync

import kotlinx.serialization.Serializable

// ─── Sync exceptions ────────────────────────────────────────────────

sealed class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class AuthFailure(message: String, cause: Throwable? = null) : SyncException(message, cause)
    class NetworkError(message: String, cause: Throwable? = null) : SyncException(message, cause)
    class QuotaExceeded(message: String) : SyncException(message)
    class NotFound(message: String) : SyncException(message)
}

// ─── Sync metadata (stored on server and locally) ────────────────────

@Serializable
data class SyncMetaData(
    val deviceId: String = "",
    val lastSyncEpochMs: Long = 0L,
    val entityCursors: EntityCursors = EntityCursors()
)

@Serializable
data class EntityCursors(
    val logEntries: Long = 0L,
    val countdowns: Long = 0L,
    val reviewStatus: Long = 0L,
    val greetingQuotes: Long = 0L,
    val balanceRecords: Long = 0L,
    val flashcardSessions: Long = 0L,  // max createdDate or count
    val pomodoroRecords: Long = 0L,  // max updatedDate
    val habits: Long = 0L,           // max updatedAt
    val habitRecords: Long = 0L,     // max updatedAt
    val reviewNotes: Long = 0L,      // max updatedDate
    val notebooks: Long = 0L,        // max updatedDate
    val lifeLogEntries: Long = 0L,   // max updatedDate
    val knowledgePointStats: Long = 0L, // max updatedDate
    val questionBankCategories: Long = 0L, // max updatedDate
    val questions: Long = 0L,            // max updatedDate
    val pomodoroPrefs: Long = 0L,
    val overlayTargets: Long = 0L
)

// ─── Wire-format models (serialized to JSON on WebDAV) ──────────────

@Serializable
data class SyncLogEntry(
    val localId: Long,
    val subject: String,
    val title: String,
    val timeSpent: Int,
    val imagePaths: List<String>,  // relative paths, e.g. "images/img_xxx.jpg"
    val description: String,
    val aiSummary: String,
    val selfCheckContent: String = "",
    val mindMapJson: String = "",
    val attachmentPaths: List<String> = emptyList(),
    val attachmentText: String = "",
    val notebookId: Long? = null,
    val createdDate: Long,
    val updatedDate: Long
)

@Serializable
data class SyncCountdown(
    val localId: Long,
    val title: String,
    val targetDate: Long,
    val note: String,
    val createdDate: Long,
    val updatedDate: Long
)

@Serializable
data class SyncHomeModule(
    val moduleKey: String,
    val enabled: Boolean,
    val sortOrder: Int,
    val selectedCountdownId: Long? = null
)

@Serializable
data class SyncReviewStatus(
    val localId: Long,
    val logEntryId: Long,
    val reviewDate: Long,
    val reviewType: String,
    val isCompleted: Boolean
)

@Serializable
data class SyncGreetingQuote(
    val localId: Long,
    val text: String,
    val createdAt: Long
)

@Serializable
data class SyncBalanceRecord(
    val localId: Long,
    val totalBalance: Double,
    val grantedBalance: Double,
    val toppedUpBalance: Double,
    val timestamp: Long
)

@Serializable
data class SyncPrefs(
    val prefName: String,
    val entries: Map<String, String>
)

@Serializable
data class SyncFlashcardSession(
    val localId: Long,
    val logEntryId: Long,
    val cardsJson: String,
    val answersJson: String = "{}",
    val style: String = "",
    val createdDate: Long
)

@Serializable
data class SyncPomodoroRecord(
    val date: String,
    val focusSecs: Long,
    val completedCount: Int,
    val updatedDate: Long
)

@Serializable
data class SyncHabit(
    val localId: Long,
    val name: String,
    val description: String = "",
    val colorArgb: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncHabitRecord(
    val date: String,        // "{habitId}_yyyy-MM-dd"
    val habitId: Long,
    val isCompleted: Boolean,
    val updatedAt: Long
)

@Serializable
data class SyncReviewNote(
    val localId: Long,
    val logEntryId: Long,
    val content: String,
    val imagePaths: List<String> = emptyList(),
    val sourceType: String = "manual",
    val flashcardSessionId: Long? = null,
    val createdDate: Long,
    val updatedDate: Long
)

@Serializable
data class SyncKnowledgePointStats(
    val localId: Long,
    val logEntryId: Long,
    val knowledgePoint: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val updatedDate: Long
)

@Serializable
data class SyncLifeLogEntry(
    val localId: Long,
    val content: String,
    val mood: String,
    val imagePaths: List<String>,
    val createdDate: Long,
    val updatedDate: Long
)

@Serializable
data class SyncNotebook(
    val localId: Long,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val icon: String = "folder",
    val pinned: Boolean = false,
    val createdDate: Long,
    val updatedDate: Long
)

@Serializable
data class SyncQuestionBankCategory(
    val localId: Long,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val createdDate: Long,
    val updatedDate: Long
)

@Serializable
data class SyncQuestion(
    val localId: Long,
    val categoryId: Long,
    val content: String,
    val imagePaths: List<String> = emptyList(),
    val aiAnalysis: String = "",
    val sortOrder: Int = 0,
    val createdDate: Long,
    val updatedDate: Long
)

// ─── Sync status (UI state) ─────────────────────────────────────────

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data class Syncing(val phase: String, val progress: Int = 0, val total: Int = 0) : SyncStatus()
    data class Success(val uploaded: Int, val downloaded: Int, val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
    data class Error(val message: String, val recoverable: Boolean = true) : SyncStatus()
}
