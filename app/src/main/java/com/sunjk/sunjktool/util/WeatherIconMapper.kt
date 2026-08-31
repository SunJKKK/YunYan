package com.sunjk.sunjktool.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Map QWeather icon code to Material Icon. */
fun weatherIcon(code: String): ImageVector = when {
    code == "100" -> Icons.Default.WbSunny          // 晴
    code in listOf("101", "102", "103", "104") -> Icons.Default.Cloud  // 多云/阴
    code == "150" -> Icons.Default.WbSunny          // 夜晚晴
    code in listOf("151", "152", "153", "154") -> Icons.Default.Cloud  // 夜晚多云
    code.toIntOrNull() in 300..318 -> Icons.Default.WaterDrop   // 雨
    code.toIntOrNull() in 400..410 -> Icons.Default.AcUnit      // 雪
    code.toIntOrNull() in 500..515 -> Icons.Default.Cloud       // 雾/霾
    else -> Icons.Default.WbSunny
}

/** Simple Chinese label for weather icon codes. */
fun weatherIconDescription(code: String): String = when {
    code == "100" -> "晴"
    code == "101" -> "多云"
    code == "102" -> "少云"
    code == "103" -> "晴间多云"
    code == "104" -> "阴"
    code in "300".."318" -> "雨"
    code in "400".."410" -> "雪"
    code in "500".."515" -> "雾"
    else -> ""
}

/** Return a color for warning severity level. */
fun warningLevelColor(level: String): Color = when {
    level.contains("红") -> Color(0xFFD32F2F)
    level.contains("橙") -> Color(0xFFF57C00)
    level.contains("黄") -> Color(0xFFFBC02D)
    level.contains("蓝") -> Color(0xFF1976D2)
    else -> Color(0xFF757575)
}
