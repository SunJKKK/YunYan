package com.sunjk.sunjktool.feature.pomodoro

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.sunjk.sunjktool.SunJKToolApp
import com.sunjk.sunjktool.util.NotificationHelper
import com.sunjk.sunjktool.util.PomodoroManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 前台服务：作为番茄钟常驻通知的唯一拥有者。
 *
 * 启动后立即 startForeground 弹出通知，并由内部协程每秒基于 PomodoroManager 的最新状态
 * 重新 startForeground 刷新，实时更新倒计时文本与进度条，同时确保"运行/已暂停"状态及时反映。
 * 由 PomodoroManager 在开始/恢复/停止时启动或停止本服务。
 */
class PomodoroService : Service() {

    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val manager: PomodoroManager?
        get() = (application as? SunJKToolApp)?.container?.pomodoroManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mgr = manager
        if (mgr == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()

        // 立即弹出前台通知（不等待首次 tick）
        startForeground(NotificationHelper.NOTIFICATION_TIMER_ID, mgr.buildTimerNotification(this))

        // 每秒刷新一次，实时更新倒计时与进度；暂停/恢复状态也随 state 自动反映
        if (tickerJob == null) {
            tickerJob = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                while (isActive) {
                    delay(1000)
                    startForeground(
                        NotificationHelper.NOTIFICATION_TIMER_ID,
                        mgr.buildTimerNotification(this@PomodoroService)
                    )
                }
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock != null) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SunJKTool:PomodoroTimer"
        ).apply { acquire() }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        tickerJob = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
