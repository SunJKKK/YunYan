package com.sunjk.sunjktool.feature.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sunjk.sunjktool.domain.model.Countdown
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.Notebook
import com.sunjk.sunjktool.data.remote.TickTickTask
import com.sunjk.sunjktool.data.remote.TickTickProject
import com.sunjk.sunjktool.util.toDueDate
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Add
import com.sunjk.sunjktool.ui.components.HomeGenerationProgressBar
import com.sunjk.sunjktool.ui.components.HomeSection
import com.sunjk.sunjktool.ui.components.NotebookIcons
import com.sunjk.sunjktool.ui.components.CompactLearningHeatmap
import com.sunjk.sunjktool.ui.components.HabitHeatmap
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import com.sunjk.sunjktool.ui.theme.LocalAnimationEnabled
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEdit: (Long?) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToWeatherDetail: () -> Unit,
    onNavigateToCountdownList: () -> Unit,
    onNavigateToLearningRecord: () -> Unit,
    onNavigateToLearningStats: () -> Unit = {},
    onNavigateToPomodoro: () -> Unit,
    onNavigateToDeepSeek: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToHabits: () -> Unit = {},
    onNavigateToNotebook: (Long) -> Unit = {},
    onNavigateToTodo: () -> Unit = {},
    onOpenAiTask: (com.sunjk.sunjktool.di.GenerationTask) -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showAddTodoDialog by remember { mutableStateOf(false) }

    // ── Stagger entrance: release one item every 60ms on cold start ──
    var initialAppearFinished by rememberSaveable { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(0) }
    val animEnabled = LocalAnimationEnabled.current

    LaunchedEffect(uiState.enabledModules.size) {
        if (!animEnabled) {
            visibleCount = Int.MAX_VALUE
            initialAppearFinished = true
            return@LaunchedEffect
        }
        if (!initialAppearFinished && uiState.enabledModules.isNotEmpty()) {
            while (visibleCount < uiState.enabledModules.size) {
                delay(60)
                visibleCount++
            }
            initialAppearFinished = true
        } else {
            visibleCount = Int.MAX_VALUE // show all immediately on subsequent loads
        }
    }

    // ── Refresh data every time the home screen becomes visible ──
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshAll()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingIndicator()
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshAllHome() },
                    modifier = Modifier.fillMaxSize()
                ) {
                val modules = uiState.enabledModules
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Greeting banner — always shown, full width

                    item(span = StaggeredGridItemSpan.FullLine) {
                        GreetingBanner()
                    }

                    // AI 生成进度 — 位于问候语之下
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HomeGenerationProgressBar(
                            onTaskClick = onOpenAiTask,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // If no modules enabled, show guidance
                    if (modules.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "首页还没有模块",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "点击右下角 + → 编辑首页，选择要展示的模块",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Render each enabled module as waterfall items
                    items(
                        count = modules.size,
                        key = { modules[it].moduleKey },
                        span = { idx ->
                            if (modules[idx].size == "large") StaggeredGridItemSpan.FullLine
                            else StaggeredGridItemSpan.SingleLane
                        }
                    ) { idx ->
                        val module = modules[idx]
                        // Stagger: only render items that have been "released"
                        if (initialAppearFinished || idx < visibleCount) {
                            Column(
                                modifier = if (animEnabled) Modifier.animateItem(
                                    fadeInSpec = tween(300, easing = FastOutSlowInEasing),
                                    placementSpec = tween(300, easing = FastOutSlowInEasing),
                                    fadeOutSpec = tween(250, easing = FastOutLinearInEasing)
                                ) else Modifier
                            ) {
                                when {
                            module.moduleKey == "heatmap" -> HomeSection(title = "学习热力图") {
                                Box(modifier = Modifier.clickable { onNavigateToLearningStats() }) {
                                    CompactLearningHeatmap(
                                        dailyCounts = uiState.heatmapData,
                                        isLarge = module.size == "large"
                                    )
                                }
                            }
                            module.moduleKey == "today_logs" -> {
                                val learningListShared = sharedTransitionScope?.let { s ->
                                    with(s) {
                                        animatedVisibilityScope?.let { scope ->
                                            Modifier.sharedBounds(rememberSharedContentState("home_to_learning_list"), scope)
                                        } ?: Modifier
                                    }
                                } ?: Modifier
                                HomeSection(title = "今日学习记录", modifier = learningListShared) {
                                    Box(modifier = Modifier.clickable { onNavigateToLearningRecord() }) {
                                        TodayLogsModule(entries = uiState.todayLogs)
                                    }
                                }
                            }
                            module.moduleKey.startsWith("countdown_") -> {
                                val id = module.moduleKey.removePrefix("countdown_").toLongOrNull()
                                val cd = uiState.countdownModules[id]
                                if (id != null && cd != null) {
                                    HomeSection(title = "倒数日") {
                                        CountdownHomeModule(
                                            countdown = cd,
                                            isLarge = module.size == "large",
                                            onClick = onNavigateToCountdownList
                                        )
                                    }
                                }
                            }
                            module.moduleKey == "weather" -> HomeSection(title = "天气") {
                                com.sunjk.sunjktool.feature.weather.home.WeatherHomeModule(
                                    weatherResult = uiState.weatherResult,
                                    onRefresh = { viewModel.refreshWeather() },
                                    onNavigateToDetail = onNavigateToWeatherDetail,
                                    isLarge = module.size == "large",
                                    modifier = sharedTransitionScope?.let { s ->
                                        with(s) {
                                            animatedVisibilityScope?.let { scope ->
                                                Modifier.sharedBounds(rememberSharedContentState("weather_home_card"), scope)
                                            } ?: Modifier
                                        }
                                    } ?: Modifier
                                )
                            }
                            module.moduleKey == "deepseek" -> HomeSection(title = "DeepSeek 额度") {
                                com.sunjk.sunjktool.feature.deepseek.home.DeepSeekHomeModule(
                                    balance = uiState.deepSeekBalance,
                                    history = uiState.deepSeekHistory,
                                    onRefresh = { viewModel.refreshDeepSeek() },
                                    onNavigateToDetail = onNavigateToDeepSeek
                                )
                            }
                            module.moduleKey == "review" -> HomeSection(title = "今日复盘任务") {
                                val items = uiState.reviewItems
                                if (items.isEmpty()) {
                                    Text("暂无复盘任务", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(16.dp))
                                } else {
                                    Column(Modifier.padding(12.dp)) {
                                        val pending = items.count { !it.isCompleted }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${pending}项待复盘", style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.weight(1f))
                                            Text("查看全部", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigateToReview() })
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        items.forEach { item ->
                                            val label = buildString {
                                                if (item.subject.isNotBlank()) append(item.subject).append(" ")
                                                append(item.title)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (item.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                    contentDescription = if (item.isCompleted) "取消完成" else "标记完成",
                                                    modifier = Modifier.size(20.dp).clickable { viewModel.toggleReviewItem(item.statusId, !item.isCompleted) },
                                                    tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.clickable { onNavigateToDetail(item.logEntryId) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            module.moduleKey == "notebook_shortcuts" -> {
                                HomeSection(title = "笔记本快捷方式") {
                                    NotebookShortcutsModule(
                                        notebooks = uiState.pinnedNotebooks,
                                        isLarge = module.size == "large",
                                        onOpenNotebook = onNavigateToNotebook
                                    )
                                }
                            }
                            module.moduleKey == "todo_today" -> {
                                HomeSection(
                                    title = "今日待办",
                                    trailing = {
                                        Surface(
                                            onClick = { showAddTodoDialog = true },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "新建任务",
                                                modifier = Modifier.padding(2.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                ) {
                                    TodayTodoModule(
                                        tasks = uiState.todayTasks,
                                        projects = uiState.todoProjects,
                                        isLarge = module.size == "large",
                                        onOpenTodo = onNavigateToTodo,
                                        onToggle = { viewModel.toggleTodoTask(it) }
                                    )
                                }
                            }
                            module.moduleKey == "pomodoro" -> HomeSection(title = "番茄钟") {
                                val pomodoroShared = sharedTransitionScope?.let { s ->
                                    with(s) {
                                        animatedVisibilityScope?.let { scope ->
                                            Modifier.sharedBounds(rememberSharedContentState("pomodoro_home_card"), scope)
                                        } ?: Modifier
                                    }
                                } ?: Modifier
                                com.sunjk.sunjktool.feature.pomodoro.home.PomodoroHomeModule(
                                    state = uiState.pomodoroState,
                                    onPause = { viewModel.pausePomodoro() },
                                    onResume = { viewModel.resumePomodoro() },
                                    onStop = { viewModel.stopPomodoro() },
                                    onNavigateToDetail = onNavigateToPomodoro,
                                    modifier = pomodoroShared,
                                    isLarge = module.size == "large"
                                )
                            }
                            module.moduleKey.startsWith("habit_") -> {
                                val habitId = module.moduleKey.removePrefix("habit_").toLongOrNull()
                                val item = habitId?.let { uiState.habitItems[it] }
                                if (habitId != null && item != null) {
                                    HomeSection(title = item.habit.name) {
                                        HabitHomeCard(
                                            item = item,
                                            isLarge = module.size == "large",
                                            onToggle = { viewModel.toggleHabitCheckIn(habitId) },
                                            onClick = onNavigateToHabits
                                        )
                                    }
                                }
                            }
                                }
                            } // when
                        } // Column
                        } // if visible
                    } // items
                }
            }
        }

        // FAB — directly opens add learning record
        val fabShared = sharedTransitionScope?.let { s ->
            with(s) {
                animatedVisibilityScope?.let { scope ->
                    Modifier.sharedBounds(rememberSharedContentState("home_add_fab"), scope)
                } ?: Modifier
            }
        } ?: Modifier
        FloatingActionButton(
            onClick = { onNavigateToEdit(null) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = fabShared
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加学习记录"
            )
        }
    }

    if (showAddTodoDialog) {
        AddTodoTaskDialog(
            projects = uiState.todoProjects,
            onDismiss = { showAddTodoDialog = false },
            onConfirm = { title, projectId, dueDate ->
                viewModel.createTodoTask(title, projectId, dueDate)
                showAddTodoDialog = false
            }
        )
    }
}

@Composable
private fun TodayLogsModule(entries: List<LogEntry>) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "今天还没有学习记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    } else {
        Column(modifier = Modifier.padding(12.dp)) {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (entry.subject.isNotBlank()) {
                            Text(
                                text = entry.subject,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownHomeModule(countdown: Countdown?, isLarge: Boolean, onClick: () -> Unit) {
    if (countdown == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无限期倒数日",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    } else {
        val today = remember { LocalDate.now() }
        val daysRemaining = ChronoUnit.DAYS.between(today, countdown.targetDate)
        val countColor = if (daysRemaining >= 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error

        if (isLarge) {
            // 大卡片：大数字居中 + 目标日期 + 时间进度
            val totalDays = ChronoUnit.DAYS.between(countdown.createdDate.toLocalDate(), countdown.targetDate)
                .coerceAtLeast(1)
            val passedDays = ChronoUnit.DAYS.between(countdown.createdDate.toLocalDate(), today)
                .coerceIn(0, totalDays)
            val progress = passedDays.toFloat() / totalDays

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = countdown.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = when {
                            daysRemaining > 0 -> "$daysRemaining"
                            daysRemaining == 0L -> "0"
                            else -> "${-daysRemaining}"
                        },
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = countColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            daysRemaining >= 0 -> "天"
                            else -> "天前"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = countColor,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = countColor
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "目标 ${countdown.targetDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "已过 ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (countdown.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = countdown.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = countColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = countdown.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            daysRemaining > 0 -> "还有${daysRemaining}天"
                            daysRemaining == 0L -> "今天"
                            else -> "已过${-daysRemaining}天"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = countColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitHomeCard(
    item: HomeHabitItem,
    isLarge: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val habitColor = androidx.compose.ui.graphics.Color(item.habit.colorArgb)

    // Stats: current streak + check-ins this month
    val (streak, monthCount) = remember(item.completedDates) {
        val today = LocalDate.now()
        var s = 0
        var cursor = today
        // allow streak starting from yesterday if today not yet checked in
        if (today !in item.completedDates) cursor = today.minusDays(1)
        while (cursor in item.completedDates) {
            s++
            cursor = cursor.minusDays(1)
        }
        val month = item.completedDates.count {
            it.year == today.year && it.month == today.month
        }
        s to month
    }

    Column(
        modifier = Modifier
            .padding(12.dp)
            .clickable { onClick() }
    ) {
        // Header: check-in circle + name + streak badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 打卡圆钮：与习惯列表页一致的样式
            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = if (item.isCompleted) habitColor
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(if (isLarge) 38.dp else 34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (item.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "取消打卡",
                            modifier = Modifier.size(if (isLarge) 20.dp else 18.dp),
                            tint = Color.White
                        )
                    } else {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(habitColor.copy(alpha = 0.55f))
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.habit.name,
                    style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (streak > 0) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                        Text(
                            "$streak 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "本月 $monthCount 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (item.isCompleted) {
                Text(
                    "已完成",
                    style = MaterialTheme.typography.labelMedium,
                    color = habitColor
                )
            }
        }

        Spacer(Modifier.height(if (isLarge) 14.dp else 10.dp))

        // Heatmap (circular cells; large cards show more weeks)
        HabitHeatmap(
            completedDates = item.completedDates,
            habitColor = habitColor,
            weeks = if (isLarge) 10 else 6,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GreetingBanner(modifier: Modifier = Modifier) {
    val greeting = remember {
        val hour = java.time.LocalTime.now().hour
        when (hour) {
            in 5..8 -> "早上好"
            in 9..11 -> "上午好"
            in 12..13 -> "中午好"
            in 14..17 -> "下午好"
            else -> "晚上好"
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NotebookShortcutsModule(
    notebooks: List<Notebook>,
    isLarge: Boolean,
    onOpenNotebook: (Long) -> Unit
) {
    if (notebooks.isEmpty()) {
        Text(
            text = "暂无固定笔记本",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    val innerSpacing = 8.dp
    if (isLarge) {
        // 大卡片：两列，外层套内层
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(innerSpacing)
        ) {
            notebooks.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(innerSpacing)
                ) {
                    row.forEach { nb ->
                        NotebookShortcutCard(nb, onOpenNotebook, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
        // 小卡片：单列，外层套内层，行数随固定数量增加
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(innerSpacing)
        ) {
            notebooks.forEach { nb ->
                NotebookShortcutCard(nb, onOpenNotebook, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun NotebookShortcutCard(
    notebook: Notebook,
    onOpenNotebook: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onOpenNotebook(notebook.id) },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                NotebookIcons.resolve(notebook.icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = notebook.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TodayTodoModule(
    tasks: List<TickTickTask>,
    projects: List<TickTickProject>,
    isLarge: Boolean,
    onOpenTodo: () -> Unit,
    onToggle: (TickTickTask) -> Unit
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "今日暂无任务",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }
    val shown = if (isLarge) tasks.take(8) else tasks.take(5)
    val done = tasks.count { it.isCompleted }
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (isLarge) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "已完成 $done/${tasks.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            val target = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
            val animated by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(durationMillis = 500),
                label = "todoProgress"
            )
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }
        shown.forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle(task) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                               else MaterialTheme.colorScheme.onSurface
                    )
                    // 标注任务所属清单（分组），样式同学习记录的科目
                    val groupName = projects.firstOrNull { it.id == task.projectId }?.name
                    if (!groupName.isNullOrBlank()) {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        if (isLarge && tasks.size > shown.size) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "还有 ${tasks.size - shown.size} 项，查看全部 →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenTodo() }
            )
        }
    }
}

@Composable
private fun AddTodoTaskDialog(
    projects: List<TickTickProject>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var dueOption by remember { mutableStateOf(com.sunjk.sunjktool.util.TickTickDueOption.TODAY) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建任务") },
        text = {
            Column {
                androidx.compose.material3.TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("清单", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    projects.forEach { p ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedProjectId == p.id,
                            onClick = { selectedProjectId = p.id },
                            label = { Text(p.name, maxLines = 1) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("截止时间", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.sunjk.sunjktool.util.TickTickDueOption.entries.forEach { opt ->
                        androidx.compose.material3.FilterChip(
                            selected = dueOption == opt,
                            onClick = { dueOption = opt },
                            label = { Text(opt.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(title, selectedProjectId, dueOption.toDueDate()) },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
