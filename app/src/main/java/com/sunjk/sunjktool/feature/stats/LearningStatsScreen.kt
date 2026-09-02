package com.sunjk.sunjktool.feature.stats

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.ui.components.CompactLearningHeatmap
import com.sunjk.sunjktool.ui.components.HomeSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningStatsScreen(
    viewModel: LearningStatsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ① 核心指标（累计值）
            item(key = "stats_cards") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(Icons.AutoMirrored.Filled.MenuBook, "${uiState.totalLogs}", "学习记录", Modifier.weight(1f))
                    StatCard(Icons.Default.Timer, formatMinutes(uiState.totalFocusMinutes), "累计专注", Modifier.weight(1f))
                    StatCard(Icons.Default.LocalFireDepartment, "${uiState.streakDays}", "连续天数", Modifier.weight(1f))
                    StatCard(Icons.Default.Quiz, "${uiState.totalQuestions}", "累计题目", Modifier.weight(1f))
                }
            }

            // ② 全局时间范围切换
            item(key = "range_selector") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    StatsTimeRange.entries.forEachIndexed { index, range ->
                        SegmentedButton(
                            selected = uiState.timeRange == range,
                            onClick = { viewModel.setTimeRange(range) },
                            shape = SegmentedButtonDefaults.itemShape(index, StatsTimeRange.entries.size),
                            label = { Text(range.label) }
                        )
                    }
                }
            }

            // ③ 专注时长（番茄钟）
            item(key = "focus_card") {
                HomeSection(title = "专注时长（番茄钟）") {
                    Column(Modifier.padding(16.dp)) {
                        BarChart(
                            bars = uiState.focusBars,
                            valueOf = { it.minutes },
                            valueLabel = { "${it.minutes}" },
                            xLabel = { weekLabel(it) }
                        )
                        Spacer(Modifier.height(10.dp))
                        val summary = when (uiState.timeRange) {
                            StatsTimeRange.WEEK -> "本周合计 ${formatMinutes(uiState.focusTotalMinutes)} · 日均 ${formatMinutes(uiState.focusTotalMinutes / 7)}"
                            StatsTimeRange.MONTH -> "近30天合计 ${formatMinutes(uiState.focusTotalMinutes)}"
                            StatsTimeRange.YEAR -> "近一年合计 ${formatMinutes(uiState.focusTotalMinutes)} · 月均 ${formatMinutes(uiState.focusTotalMinutes / 12)}"
                            StatsTimeRange.ALL -> "累计专注 ${formatMinutes(uiState.focusTotalMinutes)}"
                        }
                        Text(
                            summary,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ④ 笔记本分布（环形图 + 图例）
            item(key = "notebook_card") {
                HomeSection(
                    title = "笔记本分布",
                    trailing = {
                        IconButton(
                            onClick = {
                                viewModel.setNotebookMode(
                                    if (uiState.notebookMode == NotebookGroupMode.LEAF) NotebookGroupMode.TOP
                                    else NotebookGroupMode.LEAF
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = if (uiState.notebookMode == NotebookGroupMode.LEAF) "切换为最顶级笔记本" else "切换为最低级笔记本",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (uiState.notebookMode == NotebookGroupMode.LEAF) "按最低级笔记本" else "按最顶级笔记本",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(12.dp))
                        if (uiState.notebookSlices.isEmpty()) {
                            Text(
                                "该范围内暂无学习记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val slices = uiState.notebookSlices.take(6)
                            val colors = sliceColors(slices.size)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(
                                    slices = slices,
                                    colors = colors,
                                    modifier = Modifier.size(120.dp)
                                )
                                Spacer(Modifier.width(20.dp))
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    slices.forEachIndexed { i, slice ->
                                        LegendRow(slice, colors[i])
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ⑤ 新增题目
            item(key = "question_card") {
                HomeSection(title = "新增题目") {
                    Column(Modifier.padding(16.dp)) {
                        BarChart(
                            bars = uiState.questionBars,
                            valueOf = { it.count.toLong() },
                            valueLabel = { "${it.count}" },
                            xLabel = { weekLabel(it) }
                        )
                        Spacer(Modifier.height(10.dp))
                        val summary = when (uiState.timeRange) {
                            StatsTimeRange.WEEK -> "本周共 ${uiState.questionTotal} 道 · 日均 ${"%.1f".format(uiState.questionTotal / 7.0)} 道"
                            StatsTimeRange.MONTH -> "近30天共 ${uiState.questionTotal} 道 · 日均 ${"%.1f".format(uiState.questionTotal / 30.0)} 道"
                            StatsTimeRange.YEAR -> "近一年共 ${uiState.questionTotal} 道"
                            StatsTimeRange.ALL -> "累计 ${uiState.totalQuestions} 道"
                        }
                        Text(
                            summary,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ⑥ 年度热力图（固定最近一年，不随时间范围切换）
            item(key = "heatmap_card") {
                HomeSection(title = "年度热力图") {
                    CompactLearningHeatmap(
                        dailyCounts = uiState.heatmapData,
                        isLarge = true,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                }
            }
        }
    }
}

// ── 子组件 ───────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            Modifier.padding(vertical = 14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun weekLabel(bar: StatsBar): String = when (bar.label) {
    "1" -> "一"; "2" -> "二"; "3" -> "三"; "4" -> "四"; "5" -> "五"; "6" -> "六"; "7" -> "日"
    else -> bar.label
}

/** 柱状图：柱子底部对齐共享基线，非周视图横向可滚动 */
@Composable
private fun BarChart(
    bars: List<StatsBar>,
    valueOf: (StatsBar) -> Long,
    valueLabel: (StatsBar) -> String,
    xLabel: (StatsBar) -> String
) {
    if (bars.isEmpty()) {
        Text(
            "暂无数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val max = bars.maxOf { valueOf(it) }
    val scrollable = bars.size > 8
    val chartHeight = 120.dp
    Row(
        modifier = if (scrollable) {
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        } else Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        bars.forEachIndexed { index, bar ->
            val value = valueOf(bar)
            val fillFraction = if (max <= 0) 0f else value.toFloat() / max
            val barHeight = (fillFraction * 84).dp.coerceAtLeast(4.dp)
            val isLast = index == bars.lastIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (scrollable) Modifier.width(36.dp) else Modifier.weight(1f)
            ) {
                // 柱子区域固定高度，底部对齐，柱子从基线向上生长
                Box(
                    Modifier
                        .height(chartHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (bars.size <= 8 && value > 0) {
                            Text(
                                valueLabel(bar),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(barHeight)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    when {
                                        isLast -> MaterialTheme.colorScheme.primary
                                        value > 0 -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = xLabel(bar),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun sliceColors(count: Int): List<Color> {
    if (count == 0) return emptyList()
    val scheme = MaterialTheme.colorScheme
    // 跟随主题的和谐色板：primary / tertiary / secondary 优先，超出部分用 container 色补足
    val palette = listOf(
        scheme.primary,
        scheme.tertiary,
        scheme.secondary,
        scheme.primaryContainer,
        scheme.tertiaryContainer,
        scheme.secondaryContainer
    )
    return palette.take(count)
}

/** 环形图：多段圆弧 + 中心总数 */
@Composable
private fun DonutChart(
    slices: List<NotebookSlice>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.entryCount }.coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var startAngle = -90f
            slices.forEachIndexed { i, slice ->
                val sweep = (slice.entryCount.toFloat() / total) * 360f
                drawArc(
                    color = colors[i],
                    startAngle = startAngle,
                    sweepAngle = sweep - 2f.coerceAtMost(sweep / 4),  // 留出段间缝隙
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
            if (slices.isEmpty()) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
            }
        }
        Text(
            "$total",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LegendRow(slice: NotebookSlice, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            slice.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${(slice.fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${slice.entryCount}篇",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMinutes(minutes: Long): String = when {
    minutes >= 60 -> {
        val h = minutes / 60.0
        val rounded = (h * 10).toLong() / 10.0
        if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()}小时"
        else "${"%.1f".format(rounded)}小时"
    }
    minutes > 0 -> "${minutes}分钟"
    else -> "0"
}
