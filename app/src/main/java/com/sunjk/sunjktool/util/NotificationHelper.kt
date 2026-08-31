package com.sunjk.sunjktool.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sunjk.sunjktool.MainActivity
import com.sunjk.sunjktool.R

object NotificationHelper {
    const val CHANNEL_TIMER = "pomodoro_timer"
    const val CHANNEL_ALERT = "pomodoro_alert"
    const val NOTIFICATION_TIMER_ID = 1001
    const val NOTIFICATION_DONE_ID = 1002

    const val ACTION_PAUSE = "com.sunjk.sunjktool.POMODORO_PAUSE"
    const val ACTION_STOP = "com.sunjk.sunjktool.POMODORO_STOP"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER, "番茄钟计时", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "显示番茄钟倒计时"
            setShowBadge(false)
        }
        nm.createNotificationChannel(timerChannel)

        val alertChannel = NotificationChannel(
            CHANNEL_ALERT, "番茄钟提醒", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "番茄钟完成提醒"
        }
        nm.createNotificationChannel(alertChannel)
    }

    /**
     * Build the pomodoro timer notification.
     *
     * 文本为静态倒计时（"剩余 mm:ss"），由 PomodoroService 每秒重新 startForeground 刷新，
     * 从而在各类设备上都稳定显示实时进度。@param countdown 为 false（暂停）时附加 " · 已暂停"。
     */
    fun buildTimerNotification(
        context: Context,
        phase: String,
        remainingSecs: Int,
        progressMax: Int,
        countdown: Boolean = true
    ): android.app.Notification {
        val mins = remainingSecs / 60
        val secs = remainingSecs % 60
        val timeText = String.format("%02d:%02d", mins, secs)
        val pausedSuffix = if (countdown) "" else " · 已暂停"

        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("番茄钟 · $phase")
            .setContentText("剩余 $timeText$pausedSuffix")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_pause_action, "暂停", pausePendingIntent(context))
            .addAction(R.drawable.ic_stop_action, "结束", stopPendingIntent(context))
            .setProgress(progressMax, progressMax - remainingSecs, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun pausePendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(ACTION_PAUSE).setPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun stopPendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 2,
        Intent(ACTION_STOP).setPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun buildDoneNotification(context: Context, minutes: Int): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("番茄钟完成！")
            .setContentText("专注了 $minutes 分钟，休息一下吧 ☕")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
