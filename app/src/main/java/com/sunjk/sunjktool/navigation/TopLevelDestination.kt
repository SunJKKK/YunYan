package com.sunjk.sunjktool.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    HOME(Screen.Home, "首页", Icons.Default.Home),
    NOTEBOOK(Screen.NotebookList, "笔记本", Icons.Default.Folder),
    QUESTION_BANK(Screen.QuestionBankList, "题集", Icons.Default.Quiz),
    OVERVIEW(Screen.Overview, "概览", Icons.Default.DateRange),
    TOOLS(Screen.Tools, "工具", Icons.Default.Build),
}
