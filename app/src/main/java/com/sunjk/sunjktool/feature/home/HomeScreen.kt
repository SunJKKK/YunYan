package com.sunjk.sunjktool.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.ui.components.EmptyState
import com.sunjk.sunjktool.ui.components.ExpandableFAB
import com.sunjk.sunjktool.ui.components.HomeSection
import com.sunjk.sunjktool.ui.components.LearningHeatmap
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import com.sunjk.sunjktool.ui.components.LogEntryCard
import com.sunjk.sunjktool.ui.components.ConfirmDialog

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEdit: (Long?) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showDeleteUndo = remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.entries.isEmpty() -> {
                // Still show heatmap even when empty
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        GreetingBanner()
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HomeSection(title = "学习热力图") {
                            LearningHeatmap(dailyCounts = uiState.heatmapData)
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        EmptyState(
                            title = "还没有学习日志",
                            subtitle = "点击右下角 + 按钮，开始记录你的学习吧"
                        )
                    }
                }
            }
            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Greeting banner
                    item(span = StaggeredGridItemSpan.FullLine) {
                        GreetingBanner()
                    }

                    // Heatmap section
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HomeSection(title = "学习热力图") {
                            LearningHeatmap(dailyCounts = uiState.heatmapData)
                        }
                    }

                    // Log entry cards in waterfall
                    items(
                        items = uiState.entries,
                        key = { it.id }
                    ) { entry ->
                        LogEntryCard(
                            entry = entry,
                            onClick = { onNavigateToDetail(entry.id) }
                        )
                    }
                }
            }
        }

        // Expandable FAB
        ExpandableFAB(onAddLog = { onNavigateToEdit(null) })
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

    Column(modifier = modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "今天想做些什么？",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}
