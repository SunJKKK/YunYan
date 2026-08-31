package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun OverviewCalendar(
    selectedDate: LocalDate,
    markedDates: Set<LocalDate>,
    today: LocalDate = LocalDate.now(),
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val firstDay = displayMonth.atDay(1)
    val daysInMonth = displayMonth.lengthOfMonth()
    val startDayOfWeek = firstDay.dayOfWeek.value // Mon=1, Sun=7

    Column(modifier = modifier) {
        // Month header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月")
            }
            Text(
                "${displayMonth.year}年 ${displayMonth.monthValue}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月")
            }
        }

        // Weekday header
        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            weekDays.forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Day grid
        val totalCells = ((startDayOfWeek - 1 + daysInMonth + 6) / 7) * 7
        var cell = 0
        repeat(totalCells / 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    val dayNum = cell - (startDayOfWeek - 1) + 1
                    val isCurrentMonth = dayNum in 1..daysInMonth
                    val date = if (isCurrentMonth) displayMonth.atDay(dayNum) else null
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val hasRecords = date != null && date in markedDates

                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                            .then(if (date != null) Modifier.clickable { onDateSelected(date) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            when {
                                isToday -> Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                    Text(dayNum.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                                }
                                isSelected -> Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Text(dayNum.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(dayNum.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    if (hasRecords) Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                    }
                    cell++
                }
            }
        }
    }
}
