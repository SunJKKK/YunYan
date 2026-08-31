package com.sunjk.sunjktool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.ui.components.SunJKToolScaffold
import com.sunjk.sunjktool.ui.theme.LocalAnimationEnabled
import com.sunjk.sunjktool.ui.theme.SunJKToolTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SunJKToolApp
        setContent {
            var ready by remember { mutableStateOf(app.container != null) }
            if (!ready) {
                LaunchedEffect(Unit) {
                    while (app.container == null) delay(16) // ~1 frame
                    ready = true
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载中…", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val c = app.container!!
                val themeMode by c.apiPreferences.themeModeFlow.collectAsStateWithLifecycle(
                    initialValue = c.apiPreferences.getThemeMode()
                )
                val darkTheme = when (themeMode) {
                    ApiPreferences.THEME_MODE_LIGHT -> false
                    ApiPreferences.THEME_MODE_DARK -> true
                    else -> isSystemInDarkTheme()
                }
                SunJKToolTheme(darkTheme = darkTheme) {
                    val animationsEnabled = c.apiPreferences.isAnimationEnabled()
                    CompositionLocalProvider(LocalAnimationEnabled provides animationsEnabled) {
                    SunJKToolScaffold(
                        logRepository = c.logRepository,
                        countdownRepository = c.countdownRepository,
                        homeModuleRepository = c.homeModuleRepository,
                        weatherRepository = c.weatherRepository,
                        pomodoroManager = c.pomodoroManager,
                        deepSeekRepository = c.deepSeekRepository,
                        reviewHelper = c.reviewHelper,
                        reviewDao = c.reviewStatusDao,
                        logDao = c.logEntryDao,
                        deepSeekApi = c.deepSeekApi,
                        syncEngine = c.syncEngine,
                        flashcardRepository = c.flashcardRepository,
                        habitRepository = c.habitRepository,
                        reviewNoteRepository = c.reviewNoteRepository,
                        notebookRepository = c.notebookRepository,
                        questionBankRepository = c.questionBankRepository,
                        tickTickRepository = c.tickTickRepository,
                        lifeLogRepository = c.lifeLogRepository,
                        pomodoroRecordDao = c.pomodoroRecordDao,
                        knowledgePointStatsRepository = c.knowledgePointStatsRepository,
                        apiPreferences = c.apiPreferences
                    )
                    }
                }
            }
        }
    }
}
