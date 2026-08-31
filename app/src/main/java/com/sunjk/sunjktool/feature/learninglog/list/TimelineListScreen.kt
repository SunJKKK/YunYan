package com.sunjk.sunjktool.feature.learninglog.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sunjk.sunjktool.domain.model.LogEntry
import com.sunjk.sunjktool.domain.model.TimelineDay
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import java.io.File
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineListScreen(
    viewModel: TimelineListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val topSharedModifier = sharedTransitionScope?.let { s ->
        with(s) {
            animatedVisibilityScope?.let { scope ->
                Modifier.sharedBounds(rememberSharedContentState("home_to_learning_list"), scope)
            } ?: Modifier
        }
    } ?: Modifier

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = isSearchActive,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut()
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            placeholder = { Text("搜索标题、科目、描述…") },
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.updateSearchQuery("")
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                                }
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, "清除")
                                    }
                                }
                            },
                            modifier = Modifier.focusRequester(focusRequester)
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier.fillMaxWidth()
                ) { }
            }

            AnimatedVisibility(
                visible = !isSearchActive,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut()
            ) {
                TopAppBar(
                    modifier = topSharedModifier,
                    windowInsets = WindowInsets(0.dp),
                    title = { Text("学习记录") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "搜索")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else if (uiState.days.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "没有匹配的记录" else "还没有学习记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "换个关键词试试" else "点击首页右下角 + 添加第一条记录吧",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(end = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(uiState.days) { dayIdx, day ->
                    TimelineDayItem(
                        day = day,
                        isFirst = dayIdx == 0,
                        isLast = dayIdx == uiState.days.lastIndex,
                        isToday = day.date == java.time.LocalDate.now(),
                        todayFocusSecs = uiState.todayFocusSecs,
                        onEntryClick = onNavigateToDetail,
                        animatedVisibilityScope = animatedVisibilityScope,
                        sharedTransitionScope = sharedTransitionScope
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineDayItem(
    day: TimelineDay,
    isFirst: Boolean,
    isLast: Boolean,
    isToday: Boolean,
    todayFocusSecs: Long,
    onEntryClick: (Long) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) {
        val dotColor = MaterialTheme.colorScheme.primary
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(
                    color = dotColor,
                    radius = 4.dp.toPx(),
                    center = center
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = day.date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(2.dp))
        val taskText = buildString {
            append("共完成 ${day.entries.size} 个任务")
            if (isToday && todayFocusSecs > 0) {
                val mins = todayFocusSecs / 60
                val h = mins / 60
                val m = mins % 60
                append(" · 今日专注 ")
                if (h > 0) append("${h}小时")
                append("${m}分钟")
            }
        }
        Text(
            text = taskText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        day.entries.forEach { entry ->
            TimelineEntryCard(
                entry = entry,
                onClick = { onEntryClick(entry.id) },
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = sharedTransitionScope
            )
            Spacer(Modifier.height(6.dp))
        }

        if (!isLast) {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TimelineEntryCard(
    entry: LogEntry,
    onClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null
) {
    val sharedModifier = sharedTransitionScope?.let { s ->
        with(s) {
            animatedVisibilityScope?.let { scope ->
                Modifier.sharedBounds(rememberSharedContentState("log_card_${entry.id}"), scope)
            } ?: Modifier
        }
    } ?: Modifier
    Card(
        onClick = onClick,
        modifier = sharedModifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Text content row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (entry.subject.isNotBlank()) {
                        Text(
                            text = entry.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.timeSpent > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime, null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = formatMinutes(entry.timeSpent),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Thumbnail row
            if (entry.imagePaths.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ImageThumbnailRow(imagePaths = entry.imagePaths)
            }
        }
    }
}

@Composable
private fun ImageThumbnailRow(imagePaths: List<String>) {
    val thumbSize = 72.dp
    val thumbGap = 4.dp
    val thumbRadius = 8.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val thumbWithGap = thumbSize + thumbGap
        val visibleCount = (availableWidth / thumbWithGap).toInt().coerceAtLeast(1).coerceAtMost(imagePaths.size)
        val overflow = imagePaths.size - visibleCount

        Row {
            imagePaths.take(visibleCount).forEachIndexed { index, path ->
                val file = remember(path) { File(path).takeIf { it.exists() } }
                if (index > 0) Spacer(Modifier.width(thumbGap))
                Box(modifier = Modifier.size(thumbSize).clip(RoundedCornerShape(thumbRadius))) {
                    if (file != null) {
                        AsyncImage(
                            model = file,
                            contentDescription = "图片 ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // "+N" overlay on the last visible slot if overflow
                    if (index == visibleCount - 1 && overflow > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$overflow",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}分钟"
    else -> {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "${h}小时" else "${h}小时${m}分钟"
    }
}
