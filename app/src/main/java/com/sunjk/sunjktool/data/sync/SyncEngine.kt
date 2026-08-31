package com.sunjk.sunjktool.data.sync

import android.content.Context
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.dao.BalanceRecordDao
import com.sunjk.sunjktool.data.local.dao.CountdownDao
import com.sunjk.sunjktool.data.local.dao.FlashcardSessionDao
import com.sunjk.sunjktool.data.local.dao.GreetingQuoteDao
import com.sunjk.sunjktool.data.local.dao.HomeModuleDao
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.KnowledgePointStatsDao
import com.sunjk.sunjktool.data.local.dao.LifeLogEntryDao
import com.sunjk.sunjktool.data.local.dao.NotebookDao
import com.sunjk.sunjktool.data.local.dao.QuestionBankCategoryDao
import com.sunjk.sunjktool.data.local.dao.QuestionDao
import com.sunjk.sunjktool.data.local.dao.HabitDao
import com.sunjk.sunjktool.data.local.dao.HabitRecordDao
import com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao
import com.sunjk.sunjktool.data.local.dao.ReviewNoteDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.data.model.BalanceRecordEntity
import com.sunjk.sunjktool.data.model.CountdownEntity
import com.sunjk.sunjktool.data.model.FlashcardSessionEntity
import com.sunjk.sunjktool.data.model.GreetingQuoteEntity
import com.sunjk.sunjktool.data.model.LogEntryEntity
import com.sunjk.sunjktool.data.model.NotebookEntity
import com.sunjk.sunjktool.data.model.QuestionBankCategoryEntity
import com.sunjk.sunjktool.data.model.QuestionEntity
import com.sunjk.sunjktool.data.model.HabitEntity
import com.sunjk.sunjktool.data.model.HabitRecordEntity
import com.sunjk.sunjktool.data.model.PomodoroRecordEntity
import com.sunjk.sunjktool.data.model.ReviewNoteEntity
import com.sunjk.sunjktool.data.model.ReviewStatusEntity
import com.sunjk.sunjktool.domain.model.LogEntry
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

class SyncEngine(
    private val logEntryDao: LogEntryDao,
    private val countdownDao: CountdownDao,
    private val homeModuleDao: HomeModuleDao,
    private val reviewStatusDao: ReviewStatusDao,
    private val greetingQuoteDao: GreetingQuoteDao,
    private val balanceRecordDao: BalanceRecordDao,
    private val flashcardSessionDao: FlashcardSessionDao,
    private val pomodoroRecordDao: PomodoroRecordDao,
    private val habitDao: HabitDao,
    private val habitRecordDao: HabitRecordDao,
    private val reviewNoteDao: ReviewNoteDao,
    private val notebookDao: NotebookDao,
    private val questionBankCategoryDao: QuestionBankCategoryDao,
    private val questionDao: QuestionDao,
    private val lifeLogEntryDao: LifeLogEntryDao,
    private val knowledgePointStatsDao: KnowledgePointStatsDao,
    private val context: Context,
    internal val syncPrefs: SyncPreferencesManager,
    private val apiPreferences: ApiPreferences,
    private val httpClient: OkHttpClient
) {
    /** Create a fresh WebDAV client from current credentials. */
    fun createWebDavClient(): WebDavClient =
        KtorWebDavClient(syncPrefs.getWebDavUrl(), syncPrefs.getUsername(), syncPrefs.getPassword(), httpClient)

    private fun requireClient(): WebDavClient =
        createWebDavClient()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var debounceJob: Job? = null
    private var syncInProgress = false

    companion object {
        private const val REMOTE_ROOT = "sunjk_toolbox"
        private const val LOG_ENTRIES_DIR = "$REMOTE_ROOT/log_entries"
        private const val COUNTDOWNS_DIR = "$REMOTE_ROOT/countdowns"
        private const val IMAGES_DIR = "$REMOTE_ROOT/images"
        private const val PREFS_DIR = "$REMOTE_ROOT/prefs"
        private const val SYNC_META_PATH = "$REMOTE_ROOT/sync_meta.json"
        private const val REVIEW_STATUS_PATH = "$REMOTE_ROOT/review_status.json"
        private const val GREETING_QUOTES_PATH = "$REMOTE_ROOT/greeting_quotes.json"
        private const val BALANCE_RECORDS_PATH = "$REMOTE_ROOT/balance_records.json"
        private const val FLASHCARD_SESSIONS_PATH = "$REMOTE_ROOT/flashcard_sessions.json"
        private const val POMODORO_RECORDS_PATH = "$REMOTE_ROOT/pomodoro_records.json"
        private const val HABITS_PATH = "$REMOTE_ROOT/habits.json"
        private const val HABIT_RECORDS_PATH = "$REMOTE_ROOT/habit_records.json"
        private const val REVIEW_NOTES_PATH = "$REMOTE_ROOT/review_notes.json"
        private const val NOTEBOOKS_DIR = "$REMOTE_ROOT/notebooks"
        private const val LIFE_LOG_DIR = "$REMOTE_ROOT/life_log_entries"
        private const val KP_STATS_PATH = "$REMOTE_ROOT/knowledge_point_stats.json"
        private const val QUESTION_BANK_CATEGORIES_PATH = "$REMOTE_ROOT/question_bank_categories.json"
        private const val QUESTIONS_PATH = "$REMOTE_ROOT/question_bank/questions.json"
        private const val POMODORO_PREFS_PATH = "$PREFS_DIR/pomodoro_prefs.json"
        private const val OVERLAY_TARGETS_PATH = "$PREFS_DIR/overlay_targets.json"
        private const val DEBOUNCE_MS = 3000L
    }

    // ─── Public API ──────────────────────────────────────────────────

    /** Manual sync: full bidirectional sync now. */
    fun triggerManualSync() {
        scope.launch { performSync() }
    }

    /** Called when any entity is mutated (created/updated/deleted).
     *  Ensures the next sync detects deletions even if max(updatedDate) is unchanged. */
    fun bumpEntityMutation(entity: String) {
        syncPrefs.bumpMutationCounter(entity)
    }

    /** Auto-sync with debounce: fires after DEBOUNCE_MS of inactivity. */
    fun requestAutoSync() {
        if (!syncPrefs.isAutoSyncEnabled()) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            performSync()
        }
    }

    // ─── Sync orchestration ──────────────────────────────────────────

    private suspend fun performSync() {
        if (syncInProgress) return
        if (!syncPrefs.isConfigured) return

        syncInProgress = true
        val startTime = System.currentTimeMillis()
        try {
            val readOnly = apiPreferences.isReadOnlySync()

            _syncStatus.value = SyncStatus.Syncing("准备中")
            ensureRemoteDirectories()

            val meta = downloadOrCreateSyncMeta()

            // Upload phase — skipped entirely in read-only mode
            _syncStatus.value = SyncStatus.Syncing("上传中")
            val uploaded = if (readOnly) 0 else uploadPhase(meta)

            // Download phase — skip heavy content (images) in read-only mode
            _syncStatus.value = SyncStatus.Syncing("下载中")
            val downloaded = downloadPhase(meta, skipHeavy = readOnly)

            // Update metadata — don't write back to server in read-only mode
            val newMeta = buildCurrentMeta()
            if (!readOnly) uploadSyncMeta(newMeta)
            syncPrefs.setSyncMetaData(newMeta)
            syncPrefs.setLastSyncTimestamp(startTime)

            _syncStatus.value = SyncStatus.Success(uploaded, downloaded, startTime)
        } catch (e: SyncException.AuthFailure) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "认证失败", recoverable = false)
        } catch (e: SyncException.NetworkError) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "网络连接失败", recoverable = true)
        } catch (e: SyncException.QuotaExceeded) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "存储空间不足", recoverable = false)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("同步异常: ${e.message}", recoverable = true)
        } finally {
            syncInProgress = false
        }
    }

    // ─── Remote directory setup ──────────────────────────────────────

    private suspend fun ensureRemoteDirectories() {
        val dirs = listOf(REMOTE_ROOT, LOG_ENTRIES_DIR, COUNTDOWNS_DIR, NOTEBOOKS_DIR, "$REMOTE_ROOT/question_bank", LIFE_LOG_DIR, IMAGES_DIR, PREFS_DIR)
        for (dir in dirs) {
            requireClient().createDirectory(dir)
        }
    }

    // ─── Upload phase ────────────────────────────────────────────────

    private suspend fun uploadPhase(meta: SyncMetaData): Int {
        var count = 0
        val cursors = meta.entityCursors

        // Log entries — incremental by updatedDate
        val allLogs = logEntryDao.getAllEntries().first()
        val changedLogs = allLogs.filter { it.updatedDate > cursors.logEntries }
        for (entity in changedLogs) {
            val imagePaths = LogEntry.decodePaths(entity.imagePath).map { absoluteToRelative(it) }
            val syncEntry = SyncLogEntry(
                localId = entity.id,
                subject = entity.subject,
                title = entity.title,
                timeSpent = entity.timeSpent,
                imagePaths = imagePaths,
                description = entity.description,
                aiSummary = entity.aiSummary,
                selfCheckContent = entity.selfCheckContent,
                mindMapJson = entity.mindMapJson,
                attachmentPaths = LogEntry.decodePaths(entity.attachmentPaths).map { absoluteToRelative(it) },
                attachmentText = entity.attachmentText,
                notebookId = entity.notebookId,
                createdDate = entity.createdDate,
                updatedDate = entity.updatedDate
            )
            val data = json.encodeToString(syncEntry).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile("$LOG_ENTRIES_DIR/${entity.id}.json", data, "application/json")
            count++
        }

        // Upload images referenced by changed log entries
        for (entity in changedLogs) {
            val paths = LogEntry.decodePaths(entity.imagePath)
            for (absPath in paths) {
                val relPath = absoluteToRelative(absPath)
                val remotePath = "$REMOTE_ROOT/$relPath"
                if (!requireClient().exists(remotePath)) {
                    val file = File(absPath)
                    if (file.exists()) {
                        val mime = if (absPath.endsWith(".png")) "image/png" else "image/jpeg"
                        requireClient().uploadFile(remotePath, file.readBytes(), mime)
                        count++
                    }
                }
            }
        }

        // Clean up log-entry orphan files on server (deleted locally)
        val localLogMax = allLogs.maxOfOrNull { it.updatedDate } ?: 0L
        if (localLogMax > cursors.logEntries || syncPrefs.getMutationCounter("log_entries") > cursors.logEntries) {
            try {
                val localIds = allLogs.map { it.id }.toSet()
                val serverLogFiles = requireClient().listDirectory(LOG_ENTRIES_DIR)
                for (res in serverLogFiles) {
                    if (res.isDirectory) continue
                    val serverId = res.name.removeSuffix(".json").toLongOrNull() ?: continue
                    if (serverId !in localIds) {
                        requireClient().deleteFile("$LOG_ENTRIES_DIR/${res.name}")
                        count++
                    }
                }
            } catch (_: SyncException.NetworkError) { }
        }

        // Countdowns — incremental by updatedDate
        val allCountdowns = countdownDao.getAll().first()
        val changedCountdowns = allCountdowns.filter { it.updatedDate > cursors.countdowns }
        for (entity in changedCountdowns) {
            val syncEntry = SyncCountdown(
                localId = entity.id,
                title = entity.title,
                targetDate = entity.targetDate,
                note = entity.note,
                createdDate = entity.createdDate,
                updatedDate = entity.updatedDate
            )
            val data = json.encodeToString(syncEntry).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile("$COUNTDOWNS_DIR/${entity.id}.json", data, "application/json")
            count++
        }

        // Clean up countdown orphan files on server (deleted locally)
        val localCdMax = allCountdowns.maxOfOrNull { it.updatedDate } ?: 0L
        if (localCdMax > cursors.countdowns || syncPrefs.getMutationCounter("countdowns") > cursors.countdowns) {
            try {
                val localCdIds = allCountdowns.map { it.id }.toSet()
                val serverCdFiles = requireClient().listDirectory(COUNTDOWNS_DIR)
                for (res in serverCdFiles) {
                    if (res.isDirectory) continue
                    val serverId = res.name.removeSuffix(".json").toLongOrNull() ?: continue
                    if (serverId !in localCdIds) {
                        requireClient().deleteFile("$COUNTDOWNS_DIR/${res.name}")
                        count++
                    }
                }
            } catch (_: SyncException.NetworkError) { }
        }

        // Notebooks — incremental by updatedDate
        val allNotebooks = notebookDao.getAll().first()
        val changedNotebooks = allNotebooks.filter { it.updatedDate > cursors.notebooks }
        for (entity in changedNotebooks) {
            val syncEntry = SyncNotebook(
                localId = entity.id,
                name = entity.name,
                parentId = entity.parentId,
                sortOrder = entity.sortOrder,
                icon = entity.icon,
                pinned = entity.pinned,
                createdDate = entity.createdDate,
                updatedDate = entity.updatedDate
            )
            val data = json.encodeToString(syncEntry).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile("$NOTEBOOKS_DIR/${entity.id}.json", data, "application/json")
            count++
        }

        // Clean up notebook orphan files on server (deleted locally)
        val localNbMax = allNotebooks.maxOfOrNull { it.updatedDate } ?: 0L
        if (localNbMax > cursors.notebooks || syncPrefs.getMutationCounter("notebooks") > cursors.notebooks) {
            try {
                val localNbIds = allNotebooks.map { it.id }.toSet()
                val serverNbFiles = requireClient().listDirectory(NOTEBOOKS_DIR)
                for (res in serverNbFiles) {
                    if (res.isDirectory) continue
                    val serverId = res.name.removeSuffix(".json").toLongOrNull() ?: continue
                    if (serverId !in localNbIds) {
                        requireClient().deleteFile("$NOTEBOOKS_DIR/${res.name}")
                        count++
                    }
                }
            } catch (_: SyncException.NetworkError) { }
        }

        // Review status — full sync by max id (with mutation counter for edits/deletions)
        // Compare against locally cached cursor (not server cursor) to ensure local
        // changes are always pushed even if another device advanced the server cursor.
        val reviews = reviewStatusDao.getAll().first()
        val rvDataMax = reviews.maxOfOrNull { it.id } ?: 0L
        val rvMutation = syncPrefs.getMutationCounter("review_status")
        val localRvCursor = syncPrefs.getEntityCursor("review_status")
        if (maxOf(rvDataMax, rvMutation) > localRvCursor) {
            val rvList = reviews.map { SyncReviewStatus(it.id, it.logEntryId, it.reviewDate, it.reviewType, it.isCompleted) }
            val data = json.encodeToString(rvList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(REVIEW_STATUS_PATH, data, "application/json")
            count++
        }

        // Greeting quotes — full sync by max id (with mutation counter)
        val quotes = greetingQuoteDao.getAll().first()
        val qDataMax = quotes.maxOfOrNull { it.id } ?: 0L
        val qMutation = syncPrefs.getMutationCounter("greeting_quotes")
        val localQCursor = syncPrefs.getEntityCursor("greeting_quotes")
        if (maxOf(qDataMax, qMutation) > localQCursor) {
            val qList = quotes.map { SyncGreetingQuote(it.id, it.text, it.createdAt) }
            val data = json.encodeToString(qList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(GREETING_QUOTES_PATH, data, "application/json")
            count++
        }

        // Balance records — full sync by max id (with mutation counter)
        val balances = balanceRecordDao.getSince(0L).first()
        val bDataMax = balances.maxOfOrNull { it.id } ?: 0L
        val bMutation = syncPrefs.getMutationCounter("balance_records")
        val localBCursor = syncPrefs.getEntityCursor("balance_records")
        if (maxOf(bDataMax, bMutation) > localBCursor) {
            val bList = balances.map { SyncBalanceRecord(it.id, it.totalBalance, it.grantedBalance, it.toppedUpBalance, it.timestamp) }
            val data = json.encodeToString(bList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(BALANCE_RECORDS_PATH, data, "application/json")
            count++
        }

        // Flashcard sessions — full sync by max createdDate (with mutation counter)
        val flashcardSessions = flashcardSessionDao.getAll().first()
        val fDataMax = flashcardSessions.maxOfOrNull { it.createdDate } ?: 0L
        val fMutation = syncPrefs.getMutationCounter("flashcard_sessions")
        val localFCursor = syncPrefs.getEntityCursor("flashcard_sessions")
        if (maxOf(fDataMax, fMutation) > localFCursor) {
            val fList = flashcardSessions.map { it.toSyncJson() }
            val data = json.encodeToString(fList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(FLASHCARD_SESSIONS_PATH, data, "application/json")
            count++
        }

        // Pomodoro records — full sync by max updatedDate (with mutation counter)
        val pomodoroRecords = pomodoroRecordDao.getAll().first()
        val prDataMax = pomodoroRecords.maxOfOrNull { it.updatedDate } ?: 0L
        val prMutation = syncPrefs.getMutationCounter("pomodoro_records")
        val localPrCursor = syncPrefs.getEntityCursor("pomodoro_records")
        if (maxOf(prDataMax, prMutation) > localPrCursor) {
            val prList = pomodoroRecords.map { SyncPomodoroRecord(it.date, it.focusSecs, it.completedCount, it.updatedDate) }
            val data = json.encodeToString(prList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(POMODORO_RECORDS_PATH, data, "application/json")
            count++
        }

        // Habits — full sync by max updatedAt (with mutation counter)
        val habits = habitDao.getAll().first()
        val hDataMax = habits.maxOfOrNull { it.updatedAt } ?: 0L
        val hMutation = syncPrefs.getMutationCounter("habits")
        val localHCursor = syncPrefs.getEntityCursor("habits")
        if (maxOf(hDataMax, hMutation) > localHCursor) {
            val hList = habits.map { SyncHabit(it.id, it.name, it.description, it.colorArgb, it.createdAt, it.updatedAt) }
            val data = json.encodeToString(hList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(HABITS_PATH, data, "application/json")
            count++
        }

        // Habit records — full sync by max updatedAt (with mutation counter)
        val habitRecords = habitRecordDao.getAll().first()
        val hrDataMax = habitRecords.maxOfOrNull { it.updatedAt } ?: 0L
        val hrMutation = syncPrefs.getMutationCounter("habit_records")
        val localHrCursor = syncPrefs.getEntityCursor("habit_records")
        if (maxOf(hrDataMax, hrMutation) > localHrCursor) {
            val hrList = habitRecords.map { SyncHabitRecord(it.date, it.habitId, it.isCompleted, it.updatedAt) }
            val data = json.encodeToString(hrList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(HABIT_RECORDS_PATH, data, "application/json")
            count++
        }

        // Review notes — full sync by max updatedDate (with mutation counter)
        val reviewNotes = reviewNoteDao.getAll().first()
        val rnDataMax = reviewNotes.maxOfOrNull { it.updatedDate } ?: 0L
        val rnMutation = syncPrefs.getMutationCounter("review_notes")
        val localRnCursor = syncPrefs.getEntityCursor("review_notes")
        if (maxOf(rnDataMax, rnMutation) > localRnCursor) {
            val rnList = reviewNotes.map { SyncReviewNote(it.id, it.logEntryId, it.content, decodeImagePaths(it.imagePaths), it.sourceType, it.flashcardSessionId, it.createdDate, it.updatedDate) }
            val data = json.encodeToString(rnList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(REVIEW_NOTES_PATH, data, "application/json")
            count++
        }

        // Knowledge point stats — full sync by max updatedDate (with mutation counter)
        val kpAll = knowledgePointStatsDao.getAll().first()
        val kpDataMax = kpAll.maxOfOrNull { it.updatedDate } ?: 0L
        val kpMutation = syncPrefs.getMutationCounter("knowledge_point_stats")
        val localKpCursor = syncPrefs.getEntityCursor("knowledge_point_stats")
        if (maxOf(kpDataMax, kpMutation) > localKpCursor) {
            val kpList = kpAll.map { SyncKnowledgePointStats(it.id, it.logEntryId, it.knowledgePoint, it.totalQuestions, it.correctAnswers, it.updatedDate) }
            val data = json.encodeToString(kpList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(KP_STATS_PATH, data, "application/json")
            count++
        }

        // Question bank categories — full sync by max updatedDate (with mutation counter)
        val qbCats = questionBankCategoryDao.getAll().first()
        val qbCatDataMax = qbCats.maxOfOrNull { it.updatedDate } ?: 0L
        val qbCatMutation = syncPrefs.getMutationCounter("question_bank_categories")
        val localQbCatCursor = syncPrefs.getEntityCursor("question_bank_categories")
        if (maxOf(qbCatDataMax, qbCatMutation) > localQbCatCursor) {
            val catList = qbCats.map { SyncQuestionBankCategory(it.id, it.name, it.parentId, it.sortOrder, it.createdDate, it.updatedDate) }
            val data = json.encodeToString(catList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(QUESTION_BANK_CATEGORIES_PATH, data, "application/json")
            count++
        }

        // Questions — full sync by max updatedDate (with mutation counter)
        val questions = questionDao.getAll().first()
        val qsDataMax = questions.maxOfOrNull { it.updatedDate } ?: 0L
        val qsMutation = syncPrefs.getMutationCounter("questions")
        val localQsCursor = syncPrefs.getEntityCursor("questions")
        if (maxOf(qsDataMax, qsMutation) > localQsCursor) {
            val qsList = questions.map { q ->
                val relPaths = q.imagePaths.let { raw ->
                    if (raw.isBlank()) emptyList()
                    else try { json.decodeFromString<List<String>>(raw).map { absoluteToRelative(it) } } catch (_: Exception) { emptyList() }
                }
                SyncQuestion(q.id, q.categoryId, q.content, relPaths, q.aiAnalysis, q.sortOrder, q.createdDate, q.updatedDate)
            }
            val data = json.encodeToString(qsList).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(QUESTIONS_PATH, data, "application/json")
            count++
        }

        // Pomodoro prefs
        val pomodoroPrefs = context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        val pomodoroHash = pomodoroPrefs.all.hashCode().toLong()
        if (pomodoroHash != cursors.pomodoroPrefs) {
            val entries = pomodoroPrefs.all.mapValues { it.value.toString() }
            val syncPref = SyncPrefs("pomodoro_prefs", entries)
            val data = json.encodeToString(syncPref).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(POMODORO_PREFS_PATH, data, "application/json")
            count++
        }

        // Overlay targets
        val overlayPrefs = context.getSharedPreferences("overlay_targets", Context.MODE_PRIVATE)
        val overlayHash = overlayPrefs.all.hashCode().toLong()
        if (overlayHash != cursors.overlayTargets) {
            val entries = overlayPrefs.all.mapValues { it.value.toString() }
            val syncPref = SyncPrefs("overlay_targets", entries)
            val data = json.encodeToString(syncPref).toByteArray(Charsets.UTF_8)
            requireClient().uploadFile(OVERLAY_TARGETS_PATH, data, "application/json")
            count++
        }

        return count
    }

    // ─── Download phase ──────────────────────────────────────────────

    private suspend fun downloadPhase(meta: SyncMetaData, skipHeavy: Boolean = false): Int {
        var count = 0
        val cursors = meta.entityCursors

        // Log entries — check remote files newer than local cursor
        try {
            val remoteLogFiles = requireClient().listDirectory(LOG_ENTRIES_DIR)
            for (res in remoteLogFiles) {
                if (res.isDirectory) continue
                val localId = res.name.removeSuffix(".json").toLongOrNull() ?: continue
                val localEntry = logEntryDao.getEntryById(localId).first()
                if (localEntry == null || res.modified > localEntry.updatedDate) {
                    val data = requireClient().downloadFile("$LOG_ENTRIES_DIR/${res.name}")
                    val syncEntry = json.decodeFromString<SyncLogEntry>(String(data, Charsets.UTF_8))
                    val entity = syncEntry.toEntity(context)
                    if (localEntry == null) {
                        logEntryDao.insert(entity)
                    } else if (syncEntry.updatedDate > localEntry.updatedDate) {
                        logEntryDao.update(entity)
                    }
                    // Download missing images (skipped in read-only tablet mode)
                    if (!skipHeavy) {
                        for (imgPath in syncEntry.imagePaths) {
                            val remoteImgPath = "$REMOTE_ROOT/$imgPath"
                            val localImgPath = relativeToAbsolute(imgPath)
                            if (!File(localImgPath).exists()) {
                                try {
                                    val imgData = requireClient().downloadFile(remoteImgPath)
                                    File(localImgPath).parentFile?.mkdirs()
                                    File(localImgPath).writeBytes(imgData)
                                } catch (_: SyncException.NotFound) { /* image may have been deleted */ }
                            }
                        }
                    }
                    count++
                }
            }
        } catch (_: SyncException.NetworkError) {
            // Directory might not exist yet — that's fine
        }

        // Countdowns
        try {
            val remoteCountdownFiles = requireClient().listDirectory(COUNTDOWNS_DIR)
            for (res in remoteCountdownFiles) {
                if (res.isDirectory) continue
                val localId = res.name.removeSuffix(".json").toLongOrNull() ?: continue
                val localEntry = countdownDao.getById(localId).first()
                if (localEntry == null || res.modified > localEntry.updatedDate) {
                    val data = requireClient().downloadFile("$COUNTDOWNS_DIR/${res.name}")
                    val syncEntry = json.decodeFromString<SyncCountdown>(String(data, Charsets.UTF_8))
                    val entity = syncEntry.toEntity()
                    if (localEntry == null) {
                        countdownDao.insert(entity)
                    } else if (syncEntry.updatedDate > localEntry.updatedDate) {
                        countdownDao.update(entity)
                    }
                    count++
                }
            }

        } catch (_: SyncException.NetworkError) { }

        // Notebooks
        try {
            val remoteNotebookFiles = requireClient().listDirectory(NOTEBOOKS_DIR)
            for (res in remoteNotebookFiles) {
                if (res.isDirectory) continue
                val localId = res.name.removeSuffix(".json").toLongOrNull() ?: continue
                val localEntry = notebookDao.getById(localId).first()
                if (localEntry == null || res.modified > localEntry.updatedDate) {
                    val data = requireClient().downloadFile("$NOTEBOOKS_DIR/${res.name}")
                    val syncEntry = json.decodeFromString<SyncNotebook>(String(data, Charsets.UTF_8))
                    val entity = syncEntry.toEntity()
                    if (localEntry == null) {
                        notebookDao.insert(entity)
                    } else if (syncEntry.updatedDate > localEntry.updatedDate) {
                        notebookDao.update(entity)
                    }
                    count++
                }
            }

        } catch (_: SyncException.NetworkError) { }

        // Review status
        val rvAll = reviewStatusDao.getAll().first()
        val localRvMax = rvAll.maxOfOrNull { it.id } ?: 0L
        if (cursors.reviewStatus > localRvMax) {
            try {
                if (requireClient().exists(REVIEW_STATUS_PATH)) {
                    val data = requireClient().downloadFile(REVIEW_STATUS_PATH)
                    val remoteReviews = json.decodeFromString<List<SyncReviewStatus>>(String(data, Charsets.UTF_8))
                    val localReviews = rvAll.associateBy { it.id }
                    for (rr in remoteReviews) {
                        val local = localReviews[rr.localId]
                        if (local == null) {
                            reviewStatusDao.upsert(ReviewStatusEntity(rr.localId, rr.logEntryId, rr.reviewDate, rr.reviewType, rr.isCompleted))
                            count++
                        } else if (rr.isCompleted != local.isCompleted) {
                            reviewStatusDao.setCompleted(rr.localId, rr.isCompleted)
                            count++
                        }
                    }
                    // Delete local records not in remote (deleted on other device)
                    if (remoteReviews.isNotEmpty()) {
                        val remoteRvIds = remoteReviews.map { it.localId }.toSet()
                        for (localId in localReviews.keys) {
                            if (localId !in remoteRvIds) {
                                reviewStatusDao.deleteById(localId)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Greeting quotes
        val localQMax = greetingQuoteDao.getAll().first().maxOfOrNull { it.id } ?: 0L
        if (cursors.greetingQuotes > localQMax) {
            try {
                if (requireClient().exists(GREETING_QUOTES_PATH)) {
                    val data = requireClient().downloadFile(GREETING_QUOTES_PATH)
                    val remoteQuotes = json.decodeFromString<List<SyncGreetingQuote>>(String(data, Charsets.UTF_8))
                    val localQuotes = greetingQuoteDao.getAll().first().associateBy { it.id }
                    for (rq in remoteQuotes) {
                        if (rq.localId !in localQuotes) {
                            greetingQuoteDao.insert(GreetingQuoteEntity(rq.localId, rq.text, rq.createdAt))
                            count++
                        }
                    }
                    // Delete local quotes not in remote (deleted on other device)
                    if (remoteQuotes.isNotEmpty()) {
                        val remoteQIds = remoteQuotes.map { it.localId }.toSet()
                        for (localId in localQuotes.keys) {
                            if (localId !in remoteQIds) {
                                greetingQuoteDao.deleteById(localId)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Balance records
        val localBMax = balanceRecordDao.getSince(0L).first().maxOfOrNull { it.id } ?: 0L
        if (cursors.balanceRecords > localBMax) {
            try {
                if (requireClient().exists(BALANCE_RECORDS_PATH)) {
                    val data = requireClient().downloadFile(BALANCE_RECORDS_PATH)
                    val remoteBalances = json.decodeFromString<List<SyncBalanceRecord>>(String(data, Charsets.UTF_8))
                    val localBalances = balanceRecordDao.getSince(0L).first().associateBy { it.id }
                    for (rb in remoteBalances) {
                        if (rb.localId !in localBalances) {
                            balanceRecordDao.insert(BalanceRecordEntity(rb.localId, rb.totalBalance, rb.grantedBalance, rb.toppedUpBalance, rb.timestamp))
                            count++
                        }
                    }
                    // Delete local records not in remote (deleted on other device)
                    if (remoteBalances.isNotEmpty()) {
                        val remoteBIds = remoteBalances.map { it.localId }.toSet()
                        for (localId in localBalances.keys) {
                            if (localId !in remoteBIds) {
                                balanceRecordDao.deleteById(localId)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Flashcard sessions
        val localFMax = flashcardSessionDao.getAll().first().maxOfOrNull { it.createdDate } ?: 0L
        if (cursors.flashcardSessions > localFMax) {
            try {
                if (requireClient().exists(FLASHCARD_SESSIONS_PATH)) {
                    val data = requireClient().downloadFile(FLASHCARD_SESSIONS_PATH)
                    val remoteList = json.decodeFromString<List<SyncFlashcardSession>>(String(data, Charsets.UTF_8))
                    val localIds = flashcardSessionDao.getAll().first().map { it.id }.toSet()
                    for (rf in remoteList) {
                        if (rf.localId !in localIds) {
                            flashcardSessionDao.insert(FlashcardSessionEntity(
                                id = rf.localId,
                                logEntryId = rf.logEntryId,
                                cardsJson = rf.cardsJson,
                                answersJson = rf.answersJson,
                                style = rf.style,
                                createdDate = rf.createdDate
                            ))
                            count++
                        }
                    }
                    // Delete local sessions not in remote (deleted on other device)
                    if (remoteList.isNotEmpty()) {
                        val remoteFIds = remoteList.map { it.localId }.toSet()
                        for (localId in localIds) {
                            if (localId !in remoteFIds) {
                                flashcardSessionDao.deleteById(localId)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Review notes
        val localRnMax = reviewNoteDao.getAll().first().maxOfOrNull { it.updatedDate } ?: 0L
        if (cursors.reviewNotes > localRnMax) {
            try {
                if (requireClient().exists(REVIEW_NOTES_PATH)) {
                    val data = requireClient().downloadFile(REVIEW_NOTES_PATH)
                    val remoteList = json.decodeFromString<List<SyncReviewNote>>(String(data, Charsets.UTF_8))
                    val localIds = reviewNoteDao.getAll().first().map { it.id }.toSet()
                    for (rn in remoteList) {
                        if (rn.localId !in localIds) {
                            reviewNoteDao.insert(ReviewNoteEntity(
                                id = rn.localId,
                                logEntryId = rn.logEntryId,
                                content = rn.content,
                                imagePaths = encodeImagePaths(rn.imagePaths),
                                sourceType = rn.sourceType,
                                flashcardSessionId = rn.flashcardSessionId,
                                createdDate = rn.createdDate,
                                updatedDate = rn.updatedDate
                            ))
                            count++
                        }
                    }
                    // Delete local notes not in remote (deleted on other device)
                    if (remoteList.isNotEmpty()) {
                        val remoteIds = remoteList.map { it.localId }.toSet()
                        for (localId in localIds) {
                            if (localId !in remoteIds) {
                                reviewNoteDao.deleteById(localId)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Pomodoro records
        val localPrMax = pomodoroRecordDao.getAll().first().maxOfOrNull { it.updatedDate } ?: 0L
        if (cursors.pomodoroRecords > localPrMax) {
            try {
                if (requireClient().exists(POMODORO_RECORDS_PATH)) {
                    val data = requireClient().downloadFile(POMODORO_RECORDS_PATH)
                    val remoteRecords = json.decodeFromString<List<SyncPomodoroRecord>>(String(data, Charsets.UTF_8))
                    val localRecords = pomodoroRecordDao.getAll().first().associateBy { it.date }
                    for (rr in remoteRecords) {
                        val local = localRecords[rr.date]
                        if (local == null || rr.updatedDate > local.updatedDate) {
                            pomodoroRecordDao.upsert(PomodoroRecordEntity(rr.date, rr.focusSecs, rr.completedCount, rr.updatedDate))
                            count++
                        }
                    }
                    // Delete local records not in remote (deleted on other device)
                    if (remoteRecords.isNotEmpty()) {
                        val remoteDates = remoteRecords.map { it.date }.toSet()
                        for (date in localRecords.keys) {
                            if (date !in remoteDates) {
                                pomodoroRecordDao.deleteByDate(date)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Habits
        val localHMax = habitDao.getAll().first().maxOfOrNull { it.updatedAt } ?: 0L
        if (cursors.habits > localHMax) {
            try {
                if (requireClient().exists(HABITS_PATH)) {
                    val data = requireClient().downloadFile(HABITS_PATH)
                    val remoteHabits = json.decodeFromString<List<SyncHabit>>(String(data, Charsets.UTF_8))
                    val localHabits = habitDao.getAll().first().associateBy { it.id }
                    for (rh in remoteHabits) {
                        val local = localHabits[rh.localId]
                        if (local == null || rh.updatedAt > local.updatedAt) {
                            habitDao.insert(HabitEntity(rh.localId, rh.name, rh.description, rh.colorArgb, rh.createdAt, rh.updatedAt))
                            count++
                        }
                    }
                    // Delete local habits not in remote (deleted on other device)
                    if (remoteHabits.isNotEmpty()) {
                        val remoteHIds = remoteHabits.map { it.localId }.toSet()
                        for (localId in localHabits.keys) {
                            if (localId !in remoteHIds) {
                                habitDao.delete(HabitEntity(id = localId, name = "", description = "", colorArgb = 0, createdAt = 0, updatedAt = 0))
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Habit records
        val localHrMax = habitRecordDao.getAll().first().maxOfOrNull { it.updatedAt } ?: 0L
        if (cursors.habitRecords > localHrMax) {
            try {
                if (requireClient().exists(HABIT_RECORDS_PATH)) {
                    val data = requireClient().downloadFile(HABIT_RECORDS_PATH)
                    val remoteRecords = json.decodeFromString<List<SyncHabitRecord>>(String(data, Charsets.UTF_8))
                    val localRecords = habitRecordDao.getAll().first().associateBy { it.date }
                    for (rr in remoteRecords) {
                        val local = localRecords[rr.date]
                        if (local == null || rr.updatedAt > local.updatedAt) {
                            habitRecordDao.upsert(HabitRecordEntity(rr.date, rr.habitId, rr.isCompleted, rr.updatedAt))
                            count++
                        }
                    }
                    // Delete local records not in remote (deleted on other device)
                    if (remoteRecords.isNotEmpty()) {
                        val remoteDates = remoteRecords.map { it.date }.toSet()
                        for (date in localRecords.keys) {
                            if (date !in remoteDates) {
                                habitRecordDao.deleteByDate(date)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Question bank categories
        val localQbCatMax = questionBankCategoryDao.getAll().first().maxOfOrNull { it.updatedDate } ?: 0L
        if (cursors.questionBankCategories > localQbCatMax) {
            try {
                if (requireClient().exists(QUESTION_BANK_CATEGORIES_PATH)) {
                    val data = requireClient().downloadFile(QUESTION_BANK_CATEGORIES_PATH)
                    val remoteList = json.decodeFromString<List<SyncQuestionBankCategory>>(String(data, Charsets.UTF_8))
                    val localCats = questionBankCategoryDao.getAll().first().associateBy { it.id }
                    for (rc in remoteList) {
                        val local = localCats[rc.localId]
                        if (local == null || rc.updatedDate > local.updatedDate) {
                            questionBankCategoryDao.insert(QuestionBankCategoryEntity(rc.localId, rc.name, rc.parentId, rc.sortOrder, rc.createdDate, rc.updatedDate))
                            count++
                        }
                    }
                    // Delete local categories not in remote
                    if (remoteList.isNotEmpty()) {
                        val remoteIds = remoteList.map { it.localId }.toSet()
                        for (localId in localCats.keys) {
                            if (localId !in remoteIds) {
                                questionBankCategoryDao.deleteById(localId)
                                questionDao.deleteByCategoryId(localId)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Questions
        val localQsMax = questionDao.getAll().first().maxOfOrNull { it.updatedDate } ?: 0L
        if (cursors.questions > localQsMax) {
            try {
                if (requireClient().exists(QUESTIONS_PATH)) {
                    val data = requireClient().downloadFile(QUESTIONS_PATH)
                    val remoteList = json.decodeFromString<List<SyncQuestion>>(String(data, Charsets.UTF_8))
                    val localQs = questionDao.getAll().first().associateBy { it.id }
                    for (rq in remoteList) {
                        val local = localQs[rq.localId]
                        if (local == null || rq.updatedDate > local.updatedDate) {
                            val absPaths = rq.imagePaths.map { rel ->
                                val imagesDir = File(context.filesDir, "images")
                                File(imagesDir, rel.removePrefix("images/")).absolutePath
                            }
                            val imagePathsJson = if (absPaths.isEmpty()) "" else json.encodeToString(absPaths)
                            questionDao.insert(QuestionEntity(rq.localId, rq.categoryId, rq.content, imagePathsJson, rq.aiAnalysis, rq.sortOrder, rq.createdDate, rq.updatedDate))
                            count++
                        }
                    }
                    // Delete local questions not in remote
                    if (remoteList.isNotEmpty()) {
                        val remoteIds = remoteList.map { it.localId }.toSet()
                        for (localId in localQs.keys) {
                            if (localId !in remoteIds) {
                                questionDao.deleteById(localId)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Pomodoro prefs
        try {
            if (requireClient().exists(POMODORO_PREFS_PATH)) {
                val data = requireClient().downloadFile(POMODORO_PREFS_PATH)
                val remotePrefs = json.decodeFromString<SyncPrefs>(String(data, Charsets.UTF_8))
                val localPrefs = context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
                for ((key, value) in remotePrefs.entries) {
                    val current = localPrefs.all[key]?.toString()
                    if (current != value) {
                        localPrefs.edit().putString(key, value).apply()
                        count++
                    }
                }
            }
        } catch (_: Exception) { }

        // Overlay targets
        try {
            if (requireClient().exists(OVERLAY_TARGETS_PATH)) {
                val data = requireClient().downloadFile(OVERLAY_TARGETS_PATH)
                val remotePrefs = json.decodeFromString<SyncPrefs>(String(data, Charsets.UTF_8))
                val localPrefs = context.getSharedPreferences("overlay_targets", Context.MODE_PRIVATE)
                for ((key, value) in remotePrefs.entries) {
                    val current = localPrefs.all[key]?.toString()
                    if (current != value) {
                        localPrefs.edit().putString(key, value).apply()
                        count++
                    }
                }
            }
        } catch (_: Exception) { }

        return count
    }

    // ─── Sync metadata helpers ───────────────────────────────────────

    private suspend fun downloadOrCreateSyncMeta(): SyncMetaData {
        return try {
            if (requireClient().exists(SYNC_META_PATH)) {
                val data = requireClient().downloadFile(SYNC_META_PATH)
                json.decodeFromString<SyncMetaData>(String(data, Charsets.UTF_8))
            } else {
                SyncMetaData(deviceId = syncPrefs.getDeviceId())
            }
        } catch (_: Exception) {
            SyncMetaData(deviceId = syncPrefs.getDeviceId())
        }
    }

    private suspend fun uploadSyncMeta(meta: SyncMetaData) {
        val data = json.encodeToString(meta).toByteArray(Charsets.UTF_8)
        requireClient().uploadFile(SYNC_META_PATH, data, "application/json")
    }

    private suspend fun buildCurrentMeta(): SyncMetaData {
        val allLogs = logEntryDao.getAllEntries().first()
        val allCountdowns = countdownDao.getAll().first()
        val rvAll = reviewStatusDao.getAll().first()
        val qAll = greetingQuoteDao.getAll().first()
        val bAll = balanceRecordDao.getSince(0L).first()
        val nbAll = notebookDao.getAll().first()
        val fAll = flashcardSessionDao.getAll().first()
        val prAll = pomodoroRecordDao.getAll().first()
        val hAll = habitDao.getAll().first()
        val hrAll = habitRecordDao.getAll().first()
        val rnAll = reviewNoteDao.getAll().first()

        val pomodoroPrefs = context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        val pomodoroHash = pomodoroPrefs.all.hashCode().toLong()
        val overlayPrefs = context.getSharedPreferences("overlay_targets", Context.MODE_PRIVATE)
        val overlayHash = overlayPrefs.all.hashCode().toLong()

        return SyncMetaData(
            deviceId = syncPrefs.getDeviceId(),
            lastSyncEpochMs = System.currentTimeMillis(),
            entityCursors = EntityCursors(
                logEntries = maxOf(allLogs.maxOfOrNull { it.updatedDate } ?: 0L, syncPrefs.getMutationCounter("log_entries")),
                countdowns = maxOf(allCountdowns.maxOfOrNull { it.updatedDate } ?: 0L, syncPrefs.getMutationCounter("countdowns")),
                reviewStatus = maxOf((rvAll.maxOfOrNull { it.id } ?: 0L), syncPrefs.getMutationCounter("review_status")),
                greetingQuotes = maxOf((qAll.maxOfOrNull { it.id } ?: 0L), syncPrefs.getMutationCounter("greeting_quotes")),
                balanceRecords = maxOf((bAll.maxOfOrNull { it.id } ?: 0L), syncPrefs.getMutationCounter("balance_records")),
                flashcardSessions = maxOf(fAll.maxOfOrNull { it.createdDate } ?: 0L, syncPrefs.getMutationCounter("flashcard_sessions")),
                pomodoroRecords = maxOf(prAll.maxOfOrNull { it.updatedDate } ?: 0L, syncPrefs.getMutationCounter("pomodoro_records")),
                habits = maxOf(hAll.maxOfOrNull { it.updatedAt } ?: 0L, syncPrefs.getMutationCounter("habits")),
                habitRecords = maxOf(hrAll.maxOfOrNull { it.updatedAt } ?: 0L, syncPrefs.getMutationCounter("habit_records")),
                reviewNotes = maxOf(rnAll.maxOfOrNull { it.updatedDate } ?: 0L, syncPrefs.getMutationCounter("review_notes")),
                notebooks = maxOf(nbAll.maxOfOrNull { it.updatedDate } ?: 0L, syncPrefs.getMutationCounter("notebooks")),
                lifeLogEntries = maxOf((lifeLogEntryDao.getAllEntries().first().maxOfOrNull { it.updatedDate } ?: 0L), syncPrefs.getMutationCounter("life_log_entries")),
                knowledgePointStats = maxOf((knowledgePointStatsDao.getAll().first().maxOfOrNull { it.updatedDate } ?: 0L), syncPrefs.getMutationCounter("knowledge_point_stats")),
                questionBankCategories = maxOf((questionBankCategoryDao.getAll().first().maxOfOrNull { it.updatedDate } ?: 0L), syncPrefs.getMutationCounter("question_bank_categories")),
                questions = maxOf((questionDao.getAll().first().maxOfOrNull { it.updatedDate } ?: 0L), syncPrefs.getMutationCounter("questions")),
                pomodoroPrefs = pomodoroHash,
                overlayTargets = overlayHash
            )
        )
    }

    // ─── Path translation ────────────────────────────────────────────

    private fun absoluteToRelative(absPath: String): String {
        val imagesDir = "${context.filesDir.absolutePath}/images/"
        return if (absPath.startsWith(imagesDir)) "images/" + absPath.substringAfter(imagesDir)
        else "images/" + File(absPath).name
    }

    private fun relativeToAbsolute(relPath: String): String {
        val imagesDir = File(context.filesDir, "images")
        return File(imagesDir, relPath.removePrefix("images/")).absolutePath
    }

    private fun FlashcardSessionEntity.toSyncJson(): SyncFlashcardSession = SyncFlashcardSession(
        localId = id,
        logEntryId = logEntryId,
        cardsJson = cardsJson,
        answersJson = answersJson,
        style = style,
        createdDate = createdDate
    )

    private fun decodeImagePaths(jsonStr: String?): List<String> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(String.serializer()), jsonStr)
        } catch (_: Exception) { emptyList() }
    }

    private fun encodeImagePaths(paths: List<String>): String? {
        if (paths.isEmpty()) return null
        return try {
            json.encodeToString(ListSerializer(String.serializer()), paths)
        } catch (_: Exception) { null }
    }
}

// ─── Entity → Sync model converters ──────────────────────────────────

private fun SyncLogEntry.toEntity(context: Context): LogEntryEntity {
    val absPaths = imagePaths.map { rel ->
        val imagesDir = File(context.filesDir, "images")
        File(imagesDir, rel.removePrefix("images/")).absolutePath
    }
    val imagePathJson = LogEntry.encodePaths(absPaths)
    val absAttachmentPaths = attachmentPaths.map { rel ->
        val attachmentsDir = File(context.filesDir, "attachments")
        File(attachmentsDir, rel.removePrefix("attachments/")).absolutePath
    }
    return LogEntryEntity(
        id = localId,
        subject = subject,
        title = title,
        timeSpent = timeSpent,
        imagePath = imagePathJson,
        description = description,
        aiSummary = aiSummary,
        selfCheckContent = selfCheckContent,
        mindMapJson = mindMapJson,
        attachmentPaths = LogEntry.encodePaths(absAttachmentPaths),
        attachmentText = attachmentText,
        notebookId = notebookId,
        createdDate = createdDate,
        updatedDate = updatedDate
    )
}

private fun SyncCountdown.toEntity(): CountdownEntity =
    CountdownEntity(
        id = localId,
        title = title,
        targetDate = targetDate,
        note = note,
        createdDate = createdDate,
        updatedDate = updatedDate
    )

private fun SyncNotebook.toEntity(): NotebookEntity =
    NotebookEntity(
        id = localId,
        name = name,
        parentId = parentId,
        sortOrder = sortOrder,
        icon = icon,
        pinned = pinned,
        createdDate = createdDate,
        updatedDate = updatedDate
    )
