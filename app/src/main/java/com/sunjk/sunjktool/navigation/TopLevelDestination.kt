package com.sunjk.sunjktool.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    HOME(Screen.Home, "首页", Icons.Default.Home),
    TOOLS(Screen.Tools, "工具", Icons.Default.Build),
    MINE(Screen.Mine, "我的", Icons.Default.Person)
}
