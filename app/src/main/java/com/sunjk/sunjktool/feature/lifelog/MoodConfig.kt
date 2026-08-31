package com.sunjk.sunjktool.feature.lifelog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class MoodItem(val key: String, val label: String, val icon: ImageVector)

object MoodConfig {
    val allMoods = listOf(
        MoodItem("happy", "开心", Icons.Default.SentimentSatisfied),
        MoodItem("calm", "平静", Icons.Default.WaterDrop),
        MoodItem("energetic", "精力充沛", Icons.Default.Bolt),
        MoodItem("relaxed", "放松", Icons.Default.Air),
        MoodItem("focused", "专注", Icons.Default.CenterFocusStrong),
        MoodItem("grateful", "感恩", Icons.Default.VolunteerActivism),
        MoodItem("sad", "难过", Icons.Default.SentimentDissatisfied),
        MoodItem("anxious", "焦虑", Icons.Default.Thunderstorm),
        MoodItem("tired", "疲惫", Icons.Default.Bedtime),
        MoodItem("stressed", "紧张", Icons.Default.Compress),
        MoodItem("distracted", "分心", Icons.Default.FilterNone),
        MoodItem("irritable", "烦躁", Icons.Default.MoodBad)
    )

    val moodMap: Map<String, String> = allMoods.associate { it.key to it.label }
}
