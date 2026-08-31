package com.sunjk.sunjktool.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Check


import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private data class ToolItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isAvailable: Boolean,
    val description: String
)

private val tools = listOf(
    ToolItem("learning_log", "学习记录", Icons.AutoMirrored.Filled.MenuBook, true, "记录每日学习内容"),
    ToolItem("countdown", "倒数日", Icons.Default.DateRange, true, "重要日子倒计时"),
    ToolItem("review", "复盘", Icons.Default.Loop, true, "学习记录复盘"),
    ToolItem("habit", "习惯", Icons.Default.Repeat, true, "追踪每日习惯"),
    ToolItem("pomodoro", "番茄钟", Icons.Default.Timer, true, "专注计时器"),
    ToolItem("weather", "天气", Icons.Default.Cloud, true, "查看当前天气与预报"),
    ToolItem("deepseek", "DeepSeek", Icons.Default.Cloud, true, "API 额度与用量"),
    ToolItem("todo", "待办", Icons.Default.Check, true, "滴答清单任务管理"),
    ToolItem("home_edit", "编辑首页", Icons.Default.Edit, true, "定制首页显示的模块"),
    ToolItem("life_log", "生活记录", Icons.Default.EditNote, true, "记录每日生活与心情")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToCountdown: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToLearningRecord: () -> Unit = {},
    onNavigateToPomodoro: () -> Unit = {},
    onNavigateToDeepSeek: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToHomeEdit: () -> Unit = {},
    onNavigateToLifeLog: () -> Unit = {},
    onNavigateToTodo: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("工具") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(tools, key = { it.id }) { tool ->
                Card(
                    onClick = {
                        when (tool.id) {
                            "learning_log" -> onNavigateToLearningRecord()
                            "countdown" -> onNavigateToCountdown()
                            "weather" -> onNavigateToWeather()
                            "pomodoro" -> onNavigateToPomodoro()
                            "deepseek" -> onNavigateToDeepSeek()
                            "review" -> onNavigateToReview()
                            "habit" -> onNavigateToHabits()
                            "home_edit" -> onNavigateToHomeEdit()
                            "life_log" -> onNavigateToLifeLog()
                            "todo" -> onNavigateToTodo()
                        }
                    },
                    enabled = tool.isAvailable,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tool.isAvailable)
                            MaterialTheme.colorScheme.surfaceContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = if (tool.isAvailable)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tool.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (tool.isAvailable)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (tool.isAvailable) tool.description else "即将推出",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ToolsScreenPreview() {
    com.sunjk.sunjktool.ui.theme.SunJKToolTheme {
        ToolsScreen()
    }
}
