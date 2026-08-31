package com.sunjk.sunjktool.data.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class SyncPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ─── Credentials ─────────────────────────────────────────────────

    fun getWebDavUrl(): String =
        prefs.getString("webdav_url", "") ?: ""

    fun setWebDavUrl(url: String) =
        prefs.edit().putString("webdav_url", url).apply()

    fun getUsername(): String =
        prefs.getString("username", "") ?: ""

    fun setUsername(username: String) =
        prefs.edit().putString("username", username).apply()

    fun getPassword(): String =
        prefs.getString("password", "") ?: ""

    fun setPassword(password: String) =
        prefs.edit().putString("password", password).apply()

    val isConfigured: Boolean
        get() = getWebDavUrl().isNotBlank() && getUsername().isNotBlank() && getPassword().isNotBlank()

    // ─── Auto-sync ───────────────────────────────────────────────────

    fun isAutoSyncEnabled(): Boolean =
        prefs.getBoolean("auto_sync", false)

    fun setAutoSyncEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("auto_sync", enabled).apply()

    // ─── Sync metadata (local cache of remote sync_meta.json) ────────

    fun getSyncMetaData(): SyncMetaData {
        val raw = prefs.getString("sync_meta", null) ?: return SyncMetaData()
        return try { json.decodeFromString<SyncMetaData>(raw) }
        catch (_: Exception) { SyncMetaData() }
    }

    fun setSyncMetaData(meta: SyncMetaData) {
        prefs.edit().putString("sync_meta", json.encodeToString(meta)).apply()
    }

    fun getEntityCursor(entity: String): Long =
        getSyncMetaData().entityCursors.run {
            when (entity) {
                "log_entries" -> logEntries
                "countdowns" -> countdowns
                "review_status" -> reviewStatus
                "greeting_quotes" -> greetingQuotes
                "balance_records" -> balanceRecords
                "flashcard_sessions" -> flashcardSessions
                "pomodoro_records" -> pomodoroRecords
                "habits" -> habits
                "habit_records" -> habitRecords
                "review_notes" -> reviewNotes
                "notebooks" -> notebooks
                "knowledge_point_stats" -> knowledgePointStats
                "pomodoro_prefs" -> pomodoroPrefs
                "overlay_targets" -> overlayTargets
                else -> 0L
            }
        }

    fun setLastSyncTimestamp(ts: Long) {
        val meta = getSyncMetaData().copy(lastSyncEpochMs = ts)
        setSyncMetaData(meta)
    }

    fun getLastSyncTimestamp(): Long =
        getSyncMetaData().lastSyncEpochMs

    fun getDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    /** Per-entity mutation counter — bumped on any create/update/delete.
     *  Ensures deletions are detected by the sync engine. */
    fun bumpMutationCounter(entity: String) {
        val key = "mutation_$entity"
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }

    fun getMutationCounter(entity: String): Long =
        prefs.getLong("mutation_$entity", 0L)
}
