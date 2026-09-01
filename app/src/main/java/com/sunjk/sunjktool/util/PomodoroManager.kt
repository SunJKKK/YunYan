package com.sunjk.sunjktool.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import com.sunjk.sunjktool.domain.model.PomodoroPhase
import com.sunjk.sunjktool.domain.model.PomodoroState
import com.sunjk.sunjktool.feature.pomodoro.PomodoroService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PomodoroManager(private val appContext: Context) {

    /** Set by AppContainer after SyncEngine is created. */
    var onSyncRequested: (() -> Unit)? = null


    var recordDao: com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao? = null

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)

    private var tickCount = 0

    /**
     * 兼容类型读取偏好值：某些历史版本把 Boolean / Int / Long 值以字符串形式写入，
     * 直接调用 [SharedPreferences.getBoolean]/[SharedPreferences.getInt]/[SharedPreferences.getLong]
     * 会抛 ClassCastException，导致在 Application.onCreate 阶段直接闪退。
     * 这里通过 [SharedPreferences.all] 读取原始存储对象再按需转换，兼容真实类型与字符串表示。
     */
    private fun getBool(key: String, default: Boolean): Boolean =
        when (val v = prefs.all[key]) {
            is Boolean -> v
            is String -> v.toBooleanStrictOrNull() ?: default
            is Number -> v != 0
            else -> default
        }

    private fun getInt(key: String, default: Int): Int =
        when (val v = prefs.all[key]) {
            is Int -> v
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }

    private fun getLong(key: String, default: Long): Long =
        when (val v = prefs.all[key]) {
            is Long -> v
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            else -> default
        }

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                NotificationHelper.ACTION_PAUSE -> {
                    val s = _state.value
                    if (s.isRunning) pause() else if (s.remainingSecs > 0) resume()
                }
                NotificationHelper.ACTION_STOP -> stop()
            }
        }
    }

    private var receiverRegistered = false

    init {
        restoreState()
    }

    private fun restoreState() {
        if (!getBool("has_saved_state", false)) return

        val workMinutes = getInt("workMinutes", 30)
        val breakMinutes = getInt("breakMinutes", 20)
        val skipBreak = getBool("skipBreak", false)
        val totalFocusSecs = getLong("totalFocusSecs", 0L)
        val completedCount = getInt("completedCount", 0)
        val lastCompletedDate = prefs.getString("lastCompletedDate", "") ?: ""

        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val effectiveFocusSecs = if (lastCompletedDate != today) 0L else totalFocusSecs
        val effectiveCount = if (lastCompletedDate != today) 0 else completedCount
        val effectiveDate = if (lastCompletedDate != today) today else lastCompletedDate

        val isRunning = getBool("isRunning", false)
        if (!isRunning) {
            // 暂停后被杀进程：恢复统计，并保留未完成的倒计时进度（不回放暂停期间流逝的时间）
            val phaseName = prefs.getString("phase", "IDLE") ?: "IDLE"
            val phase = try { PomodoroPhase.valueOf(phaseName) } catch (_: Exception) { PomodoroPhase.IDLE }
            val remainingSecs = getInt("remainingSecs", 0)
            val totalSecs = getInt("totalSecs", 0)
            _state.value = PomodoroState(
                phase = if (remainingSecs > 0 && phase != PomodoroPhase.IDLE) phase else PomodoroPhase.IDLE,
                remainingSecs = remainingSecs,
                totalSecs = totalSecs,
                isRunning = false,
                workMinutes = workMinutes, breakMinutes = breakMinutes, skipBreak = skipBreak,
                totalFocusSecs = effectiveFocusSecs,
                completedCount = effectiveCount,
                lastCompletedDate = effectiveDate,
            )
            return
        }

        val phaseName = prefs.getString("phase", "IDLE") ?: "IDLE"
        val phase = try { PomodoroPhase.valueOf(phaseName) } catch (_: Exception) { PomodoroPhase.IDLE }
        val remainingSecs = getInt("remainingSecs", 0)
        val totalSecs = getInt("totalSecs", 0)
        val snapshotTime = getLong("snapshotTime", 0L)

        // Calculate elapsed wall-clock time since last save
        val elapsed = if (snapshotTime > 0L) ((System.currentTimeMillis() - snapshotTime) / 1000).toInt().coerceAtLeast(0) else 0
        val adjustedRemaining = (remainingSecs - elapsed).coerceAtLeast(0)

        if (adjustedRemaining <= 0) {
            // Timer already expired while app was dead
            val s = PomodoroState(
                phase = phase, remainingSecs = 0, totalSecs = totalSecs,
                isRunning = false, workMinutes = workMinutes, breakMinutes = breakMinutes,
                skipBreak = skipBreak, totalFocusSecs = effectiveFocusSecs,
                completedCount = effectiveCount, lastCompletedDate = effectiveDate,
            )
            _state.value = s
            onTimerFinished()
            return
        }

        _state.value = PomodoroState(
            phase = phase, remainingSecs = adjustedRemaining, totalSecs = totalSecs,
            isRunning = true, workMinutes = workMinutes, breakMinutes = breakMinutes,
            skipBreak = skipBreak, totalFocusSecs = effectiveFocusSecs,
            completedCount = effectiveCount, lastCompletedDate = effectiveDate,
        )
        registerReceiver()
        startForegroundService()
        startTimer()
    }

    private fun saveState(s: PomodoroState = _state.value) {
        scope.launch(Dispatchers.IO) {
            prefs.edit()
                .putBoolean("has_saved_state", true)
                .putString("phase", s.phase.name)
                .putInt("remainingSecs", s.remainingSecs)
                .putInt("totalSecs", s.totalSecs)
                .putBoolean("isRunning", s.isRunning)
                .putInt("workMinutes", s.workMinutes)
                .putInt("breakMinutes", s.breakMinutes)
                .putBoolean("skipBreak", s.skipBreak)
                .putLong("totalFocusSecs", s.totalFocusSecs)
                .putInt("completedCount", s.completedCount)
                .putString("lastCompletedDate", s.lastCompletedDate)
                .putLong("snapshotTime", System.currentTimeMillis())
                .apply()
        }
    }

    fun start(workMinutes: Int, breakMinutes: Int, skipBreak: Boolean) {
        val s = _state.value
        if (s.isRunning) return

        val totalSecs = workMinutes * 60
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val totalFocus = if (s.lastCompletedDate != today) 0L else s.totalFocusSecs
        val count = if (s.lastCompletedDate != today) 0 else s.completedCount

        _state.value = PomodoroState(
            phase = PomodoroPhase.FOCUS,
            remainingSecs = totalSecs,
            totalSecs = totalSecs,
            isRunning = true,
            workMinutes = workMinutes,
            breakMinutes = breakMinutes,
            skipBreak = skipBreak,
            totalFocusSecs = totalFocus,
            completedCount = count,
            lastCompletedDate = today,
        )

        saveState(_state.value)
        startForegroundService()
        registerReceiver()
        startTimer()
    }

    private fun registerReceiver() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(NotificationHelper.ACTION_PAUSE)
                addAction(NotificationHelper.ACTION_STOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(controlReceiver, filter)
            }
            receiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (receiverRegistered) {
            try { appContext.unregisterReceiver(controlReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    fun pause() {
        val s = _state.value
        if (!s.isRunning) return
        timerJob?.cancel()
        val newState = s.copy(isRunning = false)
        _state.value = newState
        saveState(newState)
        // 通知由 PomodoroService 每秒基于 state 刷新，会自动显示"已暂停"
    }

    fun resume() {
        val s = _state.value
        if (s.isRunning || s.remainingSecs <= 0) return
        val newState = s.copy(isRunning = true)
        _state.value = newState
        saveState(newState)
        startTimer()
    }

    fun preStop() {
        val s = _state.value
        if (!s.isRunning && s.remainingSecs <= 0) return
        timerJob?.cancel()
        val elapsed = s.totalSecs - s.remainingSecs
        val newState = s.copy(isRunning = false, pendingStopSecs = elapsed.coerceAtLeast(0))
        _state.value = newState
        saveState(newState)
    }

    fun confirmStop(keepProgress: Boolean) {
        val s = _state.value
        val pending = s.pendingStopSecs
        unregisterReceiver()
        stopForegroundService()
        val newTotal = if (keepProgress && s.phase == PomodoroPhase.FOCUS) s.totalFocusSecs + pending else s.totalFocusSecs
        val newState = PomodoroState(
            phase = PomodoroPhase.IDLE,
            workMinutes = s.workMinutes, breakMinutes = s.breakMinutes, skipBreak = s.skipBreak,
            totalFocusSecs = newTotal,
            completedCount = s.completedCount,
            lastCompletedDate = s.lastCompletedDate,
        )
        _state.value = newState
        saveState(newState)
        nm.cancel(NotificationHelper.NOTIFICATION_TIMER_ID)
        if (keepProgress) {
            onSyncRequested?.invoke()
            upsertTodayRecord(newTotal, newState.completedCount)
        }
    }

    fun stop() {
        val s = _state.value
        val elapsed = s.totalSecs - s.remainingSecs
        // Default: keep focus progress
        val newTotal = if (s.phase == PomodoroPhase.FOCUS) s.totalFocusSecs + elapsed.coerceAtLeast(0) else s.totalFocusSecs
        timerJob?.cancel()
        unregisterReceiver()
        stopForegroundService()
        val newState = PomodoroState(
            phase = PomodoroPhase.IDLE,
            workMinutes = s.workMinutes, breakMinutes = s.breakMinutes, skipBreak = s.skipBreak,
            totalFocusSecs = newTotal,
            completedCount = s.completedCount,
            lastCompletedDate = s.lastCompletedDate,
        )
        _state.value = newState
        saveState(newState)
        nm.cancel(NotificationHelper.NOTIFICATION_TIMER_ID)
        upsertTodayRecord(newTotal, s.completedCount)
    }

    private fun startTimer() {
        timerJob?.cancel()
        tickCount = 0
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (!s.isRunning) return@launch
                val newRemaining = s.remainingSecs - 1
                if (newRemaining <= 0) {
                    onTimerFinished()
                    return@launch
                }
                tickCount++
                _state.value = s.copy(remainingSecs = newRemaining)
                // 通知由 PomodoroService 每秒刷新；这里仅每 30 秒持久化一次以降低写入开销。
                if (tickCount % 30 == 0) {
                    saveState(_state.value)
                }
            }
        }
    }

    private fun onTimerFinished() {
        val s = _state.value
        val completedSecs = s.totalSecs
        val newTotal = s.totalFocusSecs + completedSecs
        val newCount = s.completedCount + 1

        // Show done notification
        val doneNotif = NotificationHelper.buildDoneNotification(appContext, s.workMinutes)
        nm.notify(NotificationHelper.NOTIFICATION_DONE_ID, doneNotif)

        if (s.skipBreak) {
            stopForegroundService()
            val newState = PomodoroState(
                phase = PomodoroPhase.IDLE,
                workMinutes = s.workMinutes,
                breakMinutes = s.breakMinutes,
                skipBreak = s.skipBreak,
                totalFocusSecs = newTotal,
                completedCount = newCount,
                lastCompletedDate = s.lastCompletedDate,
            )
            _state.value = newState
            saveState(newState)
            nm.cancel(NotificationHelper.NOTIFICATION_TIMER_ID)
            onSyncRequested?.invoke()
            upsertTodayRecord(newTotal, newCount)
        } else {
            // Start break
            val breakSecs = s.breakMinutes * 60
            val newState = PomodoroState(
                phase = PomodoroPhase.BREAK,
                remainingSecs = breakSecs,
                totalSecs = breakSecs,
                isRunning = true,
                workMinutes = s.workMinutes,
                breakMinutes = s.breakMinutes,
                skipBreak = s.skipBreak,
                totalFocusSecs = newTotal,
                completedCount = newCount,
                lastCompletedDate = s.lastCompletedDate,
            )
            _state.value = newState
            saveState(newState)
            startTimer()
            onSyncRequested?.invoke()
            upsertTodayRecord(newTotal, newCount)
        }
    }

    /** 供 PomodoroService 构建/刷新前台通知，基于当前 state 反映运行/暂停与实时倒计时。 */
    fun buildTimerNotification(context: Context): android.app.Notification {
        val s = _state.value
        val pausedSuffix = if (!s.isRunning) " · 已暂停" else ""
        val phaseText = if (s.phase == PomodoroPhase.FOCUS) "工作中$pausedSuffix" else "休息中$pausedSuffix"
        return NotificationHelper.buildTimerNotification(
            context, phaseText, s.remainingSecs, s.totalSecs,
            countdown = s.isRunning
        )
    }


    fun setSkipBreak(skip: Boolean) {
        val s = _state.value.copy(skipBreak = skip)
        _state.value = s
        saveState(s)
    }

    private fun startForegroundService() {
        val intent = Intent(appContext, PomodoroService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    private fun stopForegroundService() {
        appContext.stopService(Intent(appContext, PomodoroService::class.java))
    }


    private fun upsertTodayRecord(focusSecs: Long, completedCount: Int) {
        val dao = recordDao ?: return
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        scope.launch(Dispatchers.IO) {
            val existing = dao.getByDate(today)
            val newSecs = if (existing != null) maxOf(existing.focusSecs, focusSecs) else focusSecs
            val newCount = if (existing != null) maxOf(existing.completedCount, completedCount) else completedCount
            dao.upsert(
                com.sunjk.sunjktool.data.model.PomodoroRecordEntity(
                    date = today,
                    focusSecs = newSecs,
                    completedCount = newCount,
                    updatedDate = System.currentTimeMillis()
                )
            )
        }
    }

}
