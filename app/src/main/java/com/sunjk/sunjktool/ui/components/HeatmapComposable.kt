package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val CELL_SIZE = 14
private const val CELL_GAP = 3
private const val TOTAL_WEEKS = 12

private data class DayCell(val date: LocalDate, val count: Int)
private data class WeekData(val month: java.time.Month, val days: List<DayCell?>)

@Composable
fun LearningHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val startDate = remember(today) {
        today.minusWeeks((TOTAL_WEEKS - 1).toLong()).with(DayOfWeek.MONDAY)
    }
    val maxCount = remember(dailyCounts) { dailyCounts.values.maxOrNull() ?: 1 }

    // Build week-sorted data: each week has 7 days (Mon..Sun), future dates = null
    val weeks: List<WeekData> = remember(dailyCounts, today) {
        val list = mutableListOf<WeekData>()
        var cursor = startDate
        while (!cursor.isAfter(today)) {
            val days = DayOfWeek.entries.map { dow ->
                val date = cursor.plusDays(dow.ordinal.toLong())
                if (date.isAfter(today)) null
                else DayCell(date, dailyCounts[date] ?: 0)
            }
            list.add(WeekData(cursor.month, days))
            cursor = cursor.plusWeeks(1)
        }
        list
    }

    // Which week-columns each month spans (for month header alignment)
    val monthSpans: List<Triple<java.time.Month, Int, Int>> = remember(weeks) {
        val spans = mutableListOf<Triple<java.time.Month, Int, Int>>()
        weeks.forEachIndexed { idx, w ->
            val last = spans.lastOrNull()
            if (last == null || last.first != w.month) {
                spans.add(Triple(w.month, idx, idx))
            } else {
                spans[spans.lastIndex] = last.copy(third = idx)
            }
        }
        spans
    }

    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(modifier = modifier.padding(12.dp)) {

        // ---- Month header row ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CELL_GAP.dp)
        ) {
            Spacer(modifier = Modifier.width(22.dp)) // offset: 20dp labels + 2dp gap

            monthSpans.forEach { (month, start, _) ->
                // Find the first occurrence of this month span
                val span = monthSpans.first { it.first == month }
                val spanWeeks = span.third - span.second + 1
                val spanWidth = spanWeeks * (CELL_SIZE + CELL_GAP) - CELL_GAP
                Text(
                    text = month.getDisplayName(TextStyle.FULL, Locale.CHINESE),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(spanWidth.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ---- Grid body ----
        Row(verticalAlignment = Alignment.Top) {
            // Day-of-week labels
            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP.dp)) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(width = 20.dp, height = CELL_SIZE.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Cells grid
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP.dp)) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP.dp)) {
                        week.days.forEach { cell ->
                            if (cell != null) {
                                val intensity = if (maxCount > 0)
                                    cell.count.toFloat() / maxCount else 0f
                                HeatmapCell(intensity = intensity)
                            } else {
                                Spacer(modifier = Modifier.size(CELL_SIZE.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---- Legend ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "少",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEachIndexed { idx, intensity ->
                HeatmapCell(intensity = intensity)
                if (idx < 4) Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "多",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun HeatmapCell(intensity: Float) {
    val color = if (intensity == 0f) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + intensity * 0.88f)
    }
    Box(
        modifier = Modifier
            .size(CELL_SIZE.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
    )
}

// ---- Compact variant for home module card ----
private const val COMPACT_CELL_SIZE = 14
private const val COMPACT_CELL_GAP = 3
private const val COMPACT_TOTAL_WEEKS = 6
private const val LARGE_TOTAL_WEEKS = 16

@Composable
fun CompactLearningHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val totalWeeks = if (isLarge) LARGE_TOTAL_WEEKS else COMPACT_TOTAL_WEEKS
    val today = remember { LocalDate.now() }
    val startDate = remember(today, totalWeeks) {
        today.minusWeeks((totalWeeks - 1).toLong()).with(DayOfWeek.MONDAY)
    }
    val maxCount = remember(dailyCounts) { dailyCounts.values.maxOrNull() ?: 1 }

    val weeks: List<List<DayCell?>> = remember(dailyCounts, today) {
        val list = mutableListOf<List<DayCell?>>()
        var cursor = startDate
        while (!cursor.isAfter(today)) {
            val days = DayOfWeek.entries.map { dow ->
                val date = cursor.plusDays(dow.ordinal.toLong())
                if (date.isAfter(today)) null
                else DayCell(date, dailyCounts[date] ?: 0)
            }
            list.add(days)
            cursor = cursor.plusWeeks(1)
        }
        list
    }

    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    // Month spans for the large header row
    val monthSpans: List<Triple<java.time.Month, Int, Int>> = remember(weeks) {
        val spans = mutableListOf<Triple<java.time.Month, Int, Int>>()
        weeks.forEachIndexed { idx, w ->
            val month = startDate.plusWeeks(idx.toLong()).month
            val last = spans.lastOrNull()
            if (last == null || last.first != month) {
                spans.add(Triple(month, idx, idx))
            } else {
                spans[spans.lastIndex] = last.copy(third = idx)
            }
        }
        spans
    }

    // Large-only stats: this month's active days / total records
    val monthStats = remember(dailyCounts, today) {
        val monthCounts = dailyCounts.filterKeys { it.year == today.year && it.month == today.month }
        val activeDays = monthCounts.values.count { it > 0 }
        val totalRecords = monthCounts.values.sum()
        activeDays to totalRecords
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLarge) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月已学 ${monthStats.first} 天",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "共 ${monthStats.second} 条记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLarge) {
                // Month header row aligned with week columns
                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(COMPACT_CELL_GAP.dp)
                ) {
                    monthSpans.forEach { (month, _, end) ->
                        val spanWeeks = end - monthSpans.first { it.first == month }.second + 1
                        val spanWidth = spanWeeks * (COMPACT_CELL_SIZE + COMPACT_CELL_GAP) - COMPACT_CELL_GAP
                        Text(
                            text = "${month.value}月",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(spanWidth.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.Top) {
            // Day-of-week labels
            Column(verticalArrangement = Arrangement.spacedBy(COMPACT_CELL_GAP.dp)) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(width = 18.dp, height = COMPACT_CELL_SIZE.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Cells grid
            Row(horizontalArrangement = Arrangement.spacedBy(COMPACT_CELL_GAP.dp)) {
                    weeks.forEach { weekDays ->
                        Column(verticalArrangement = Arrangement.spacedBy(COMPACT_CELL_GAP.dp)) {
                            weekDays.forEach { cell ->
                                if (cell != null) {
                                    val intensity = if (maxCount > 0)
                                        cell.count.toFloat() / maxCount else 0f
                                    CompactHeatmapCell(intensity = intensity)
                                } else {
                                    Spacer(modifier = Modifier.size(COMPACT_CELL_SIZE.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLarge) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "少",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEachIndexed { idx, intensity ->
                    CompactHeatmapCell(intensity = intensity)
                    if (idx < 4) Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "多",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun CompactHeatmapCell(intensity: Float) {
    val color = if (intensity == 0f) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + intensity * 0.88f)
    }
    Box(
        modifier = Modifier
            .size(COMPACT_CELL_SIZE.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
    )
}

@Preview(showBackground = true)
@Composable
private fun LearningHeatmapPreview() {
    val today = LocalDate.now()
    val sampleData = (0..83).associate { offset ->
        val date = today.minusDays(offset.toLong())
        date to (0..10).random()
    }
    com.sunjk.sunjktool.ui.theme.SunJKToolTheme {
        LearningHeatmap(dailyCounts = sampleData)
    }
}

// ─── Habit heatmap: circular cells for habit check-in tracking ──────

private const val HABIT_HEATMAP_WEEKS = 6
private const val HABIT_CELL_SIZE = 14
private const val HABIT_CELL_GAP = 3

@Composable
fun HabitHeatmap(
    completedDates: Set<LocalDate>,
    habitColor: Color,
    weeks: Int = HABIT_HEATMAP_WEEKS,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val startDate = remember(today) {
        today.minusWeeks((weeks - 1).toLong()).with(DayOfWeek.MONDAY)
    }
    // Pre-read theme values outside loops for composable context
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val emptyCellColor = MaterialTheme.colorScheme.surfaceContainerLow

    Row(modifier = modifier) {
        // Day-of-week labels (Mon, Wed, Fri)
        Column(
            modifier = Modifier.padding(end = 4.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(HABIT_CELL_GAP.dp)
        ) {
            for (day in 0..6) {
                val label = when (day) {
                    0 -> "一"
                    2 -> "三"
                    4 -> "五"
                    else -> ""
                }
                Box(modifier = Modifier.size(HABIT_CELL_SIZE.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = labelStyle, fontSize = 9.sp, color = labelColor)
                }
            }
        }

        // Weeks columns
        Row(horizontalArrangement = Arrangement.spacedBy(HABIT_CELL_GAP.dp)) {
            for (col in 0 until weeks) {
                Column(verticalArrangement = Arrangement.spacedBy(HABIT_CELL_GAP.dp)) {
                    for (row in 0..6) {
                        val daysFromStart = col * 7 + row
                        val date = startDate.plusDays(daysFromStart.toLong())
                        val isCompleted = date in completedDates
                        val isFuture = date > today

                        Box(
                            modifier = Modifier
                                .size(HABIT_CELL_SIZE.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isFuture -> Color.Transparent
                                        isCompleted -> habitColor
                                        else -> emptyCellColor
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
