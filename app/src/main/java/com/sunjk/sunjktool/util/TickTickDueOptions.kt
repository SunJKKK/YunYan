package com.sunjk.sunjktool.util

import java.time.DayOfWeek
import java.time.LocalDate

/** 新建任务截止时间的预选选项。 */
enum class TickTickDueOption(val label: String) {
    TODAY("今天"),
    TOMORROW("明天"),
    DAY_AFTER_TOMORROW("后天"),
    THIS_WEEK("本周"),
    NONE("无")
}

/** 计算各预选截止日期对应的本地日期（本周为本周日/周末）。 */
fun TickTickDueOption.toDueDate(today: LocalDate = LocalDate.now()): String? =
    when (this) {
        TickTickDueOption.TODAY -> today.toString()
        TickTickDueOption.TOMORROW -> today.plusDays(1).toString()
        TickTickDueOption.DAY_AFTER_TOMORROW -> today.plusDays(2).toString()
        // 本周：截止本周日（本周的最后一天）
        TickTickDueOption.THIS_WEEK -> {
            val daysToSunday = (DayOfWeek.SUNDAY.value - today.dayOfWeek.value + 7) % 7
            today.plusDays(daysToSunday.toLong()).toString()
        }
        TickTickDueOption.NONE -> null
    }
