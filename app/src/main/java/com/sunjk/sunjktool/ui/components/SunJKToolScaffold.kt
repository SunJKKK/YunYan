package com.sunjk.sunjktool.ui.components

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.FlashcardRepository
import com.sunjk.sunjktool.domain.repository.HabitRepository
import com.sunjk.sunjktool.domain.repository.HomeModuleRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.DeepSeekRepository
import com.sunjk.sunjktool.domain.repository.WeatherRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.sync.SyncEngine
import com.sunjk.sunjktool.util.PomodoroManager
import com.sunjk.sunjktool.util.ReviewHelper
import com.sunjk.sunjktool.navigation.Screen
import com.sunjk.sunjktool.navigation.SunJKToolNavHost
import com.sunjk.sunjktool.navigation.TopLevelDestination

@Composable
fun SunJKToolScaffold(
    logRepository: LogRepository,
    countdownRepository: CountdownRepository,
    homeModuleRepository: HomeModuleRepository,
    weatherRepository: WeatherRepository,
    pomodoroManager: PomodoroManager,
    deepSeekRepository: DeepSeekRepository,
    reviewHelper: ReviewHelper,
    reviewDao: ReviewStatusDao,
    logDao: LogEntryDao,
    deepSeekApi: DeepSeekApi,
    syncEngine: SyncEngine,
    flashcardRepository: FlashcardRepository,
    habitRepository: HabitRepository,
    reviewNoteRepository: com.sunjk.sunjktool.domain.repository.ReviewNoteRepository,
    notebookRepository: NotebookRepository,
    questionBankRepository: QuestionBankRepository,
    tickTickRepository: com.sunjk.sunjktool.domain.repository.TickTickRepository,
    lifeLogRepository: com.sunjk.sunjktool.domain.repository.LifeLogRepository,
    pomodoroRecordDao: com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao,
    knowledgePointStatsRepository: com.sunjk.sunjktool.domain.repository.KnowledgePointStatsRepository,
    apiPreferences: com.sunjk.sunjktool.data.local.ApiPreferences,
    modifier: Modifier = Modifier
) {
    val navController: NavHostController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = TopLevelDestination.entries.map { it.screen.route }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == dest.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.screen.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        SharedTransitionLayout {
        SunJKToolNavHost(
            navController = navController,
            logRepository = logRepository,
            countdownRepository = countdownRepository,
            homeModuleRepository = homeModuleRepository,
            weatherRepository = weatherRepository,
            pomodoroManager = pomodoroManager,
            deepSeekRepository = deepSeekRepository,
            reviewHelper = reviewHelper,
            reviewDao = reviewDao,
            logDao = logDao,
            deepSeekApi = deepSeekApi,
            syncEngine = syncEngine,
            flashcardRepository = flashcardRepository,
            habitRepository = habitRepository,
            reviewNoteRepository = reviewNoteRepository,
            notebookRepository = notebookRepository,
            questionBankRepository = questionBankRepository,
            tickTickRepository = tickTickRepository,
            lifeLogRepository = lifeLogRepository,
            pomodoroRecordDao = pomodoroRecordDao,
            knowledgePointStatsRepository = knowledgePointStatsRepository,
            apiPreferences = apiPreferences,
            sharedTransitionScope = this,
            modifier = Modifier.padding(innerPadding)
        )
        }
    }
}