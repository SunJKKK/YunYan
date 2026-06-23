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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            .clip(RoundedCornerShape(3.dp))
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
