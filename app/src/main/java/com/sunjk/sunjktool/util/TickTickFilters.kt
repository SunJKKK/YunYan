package com.sunjk.sunjktool.util

import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.remote.TickTickTask
import java.time.LocalDate

/**
 * 待办任务通用过滤/排序工具，供待办页与首页"今日待办"共用。
 * 已完成任务显示模式见 [ApiPreferences.COMPLETED_MODE_*]。
 */
object TickTickFilters {

    /**
     * 按"已完成任务显示模式"过滤并排序（未完成任务优先，已完成靠后，各自按 sortOrder 排序）。
     */
    fun applyCompletedMode(tasks: List<TickTickTask>, mode: String): List<TickTickTask> {
        val today = LocalDate.now().toString()
        val filtered = when (mode) {
            ApiPreferences.COMPLETED_MODE_NONE -> tasks.filter { !it.isCompleted }
            ApiPreferences.COMPLETED_MODE_TODAY -> tasks.filter { !it.isCompleted || it.dueDate == today }
            else -> tasks // all
        }
        return filtered.sortedWith(
            compareBy({ if (it.isCompleted) 1 else 0 }, { it.sortOrder })
        )
    }
}
