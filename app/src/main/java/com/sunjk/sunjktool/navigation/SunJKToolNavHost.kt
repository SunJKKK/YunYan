package com.sunjk.sunjktool.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.sunjk.sunjktool.di.CountdownEditVMF
import com.sunjk.sunjktool.di.CountdownListVMF
import com.sunjk.sunjktool.di.FlashcardHubVMF
import com.sunjk.sunjktool.di.FlashcardVMF
import com.sunjk.sunjktool.di.HabitEditVMF
import com.sunjk.sunjktool.di.HabitListVMF
import com.sunjk.sunjktool.di.HomeEditVMF
import com.sunjk.sunjktool.di.HomeVMF
import com.sunjk.sunjktool.di.LogDetailVMF
import com.sunjk.sunjktool.di.LogEditVMF
import com.sunjk.sunjktool.di.PomodoroVMF
import com.sunjk.sunjktool.di.DeepSeekVMF
import com.sunjk.sunjktool.di.ReviewHistoryVMF
import com.sunjk.sunjktool.di.ReviewListVMF
import com.sunjk.sunjktool.di.ReviewNoteListVMF
import com.sunjk.sunjktool.di.ReviewNoteEditVMF
import com.sunjk.sunjktool.di.ReviewNoteDetailVMF
import com.sunjk.sunjktool.di.NotebookListVMF
import com.sunjk.sunjktool.di.NotebookDetailVMF
import com.sunjk.sunjktool.di.NotebookEditVMF
import com.sunjk.sunjktool.di.QuestionBankListVMF
import com.sunjk.sunjktool.di.QuestionLinkListVMF
import com.sunjk.sunjktool.di.QuestionBankDetailVMF
import com.sunjk.sunjktool.di.QuestionBankEditVMF
import com.sunjk.sunjktool.di.SettingsVMF
import com.sunjk.sunjktool.di.SyncSettingsVMF
import com.sunjk.sunjktool.di.TimelineListVMF
import com.sunjk.sunjktool.di.WeatherDetailVMF
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.FlashcardRepository
import com.sunjk.sunjktool.domain.repository.HabitRepository
import com.sunjk.sunjktool.domain.repository.HomeModuleRepository
import com.sunjk.sunjktool.domain.repository.DeepSeekRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.WeatherRepository
import com.sunjk.sunjktool.feature.countdown.edit.CountdownEditScreen
import com.sunjk.sunjktool.feature.countdown.list.CountdownListScreen
import com.sunjk.sunjktool.feature.home.HomeScreen
import com.sunjk.sunjktool.feature.home.edit.HomeEditScreen
import com.sunjk.sunjktool.feature.learninglog.detail.LogDetailScreen
import com.sunjk.sunjktool.feature.learninglog.edit.LogEditScreen
import com.sunjk.sunjktool.feature.learninglog.flashcard.FlashcardHubScreen
import com.sunjk.sunjktool.feature.learninglog.flashcard.FlashcardScreen
import com.sunjk.sunjktool.feature.reviewnote.list.ReviewNoteListScreen
import com.sunjk.sunjktool.feature.reviewnote.edit.ReviewNoteEditScreen
import com.sunjk.sunjktool.feature.reviewnote.detail.ReviewNoteDetailScreen
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.ui.theme.LocalAnimationEnabled
import com.sunjk.sunjktool.feature.learninglog.list.TimelineListScreen
import com.sunjk.sunjktool.feature.deepseek.DeepSeekScreen
import com.sunjk.sunjktool.feature.pomodoro.PomodoroHistoryScreen
import com.sunjk.sunjktool.feature.pomodoro.PomodoroScreen
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.sync.SyncEngine
import com.sunjk.sunjktool.data.sync.SyncPreferencesManager
import com.sunjk.sunjktool.feature.notebook.list.NotebookListScreen
import com.sunjk.sunjktool.feature.notebook.edit.NotebookEditScreen
import com.sunjk.sunjktool.feature.notebook.detail.NotebookDetailScreen
import com.sunjk.sunjktool.feature.questionbank.list.QuestionBankListScreen
import com.sunjk.sunjktool.feature.questionbank.edit.QuestionBankEditScreen
import com.sunjk.sunjktool.feature.questionbank.detail.QuestionBankDetailScreen
import com.sunjk.sunjktool.feature.questionbank.link.QuestionLinkListScreen
import com.sunjk.sunjktool.feature.questionbank.link.QuestionLinkListViewModel
import com.sunjk.sunjktool.feature.onboarding.OnboardingScreen
import com.sunjk.sunjktool.feature.settings.SettingsScreen
import com.sunjk.sunjktool.feature.settings.SettingsSection
import com.sunjk.sunjktool.feature.settings.SettingsViewModel
import com.sunjk.sunjktool.feature.settings.SettingsAboutScreen
import com.sunjk.sunjktool.feature.settings.SettingsAiScreen
import com.sunjk.sunjktool.feature.settings.SettingsApiKeysScreen
import com.sunjk.sunjktool.feature.settings.SettingsDisplayScreen
import com.sunjk.sunjktool.feature.settings.SettingsTickTickScreen
import com.sunjk.sunjktool.feature.settings.SettingsWebDavScreen
import com.sunjk.sunjktool.feature.sync.SyncSettingsScreen
import com.sunjk.sunjktool.util.PomodoroManager
import com.sunjk.sunjktool.util.ReviewHelper
import com.sunjk.sunjktool.feature.weather.detail.WeatherDetailScreen

internal const val TRANSITION_DURATION = 300

private fun tabIndex(route: String?): Int =
    TopLevelDestination.entries.indexOfFirst { it.screen.route == route }

// ---- 统一动画：前进 ----
private val forwardEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing))
}
private val forwardExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing)) { -it } +
        fadeOut(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing))
}

// ---- 统一动画：返回 ----
private val backEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)) { -it } +
        fadeIn(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing))
}
private val backExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing)) { it } +
        fadeOut(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(
    from: NavBackStackEntry,
    to: NavBackStackEntry
): Boolean {
    val fromIdx = tabIndex(from.destination.route)
    val toIdx = tabIndex(to.destination.route)
    if (fromIdx == -1) return false
    if (toIdx == -1) return true
    return toIdx >= fromIdx
}

@Composable
fun SunJKToolNavHost(
    navController: NavHostController,
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
    reviewNoteRepository: ReviewNoteRepository,
    notebookRepository: NotebookRepository,
    questionBankRepository: QuestionBankRepository,
    tickTickRepository: com.sunjk.sunjktool.domain.repository.TickTickRepository,
    lifeLogRepository: com.sunjk.sunjktool.domain.repository.LifeLogRepository,
    pomodoroRecordDao: com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao,
    knowledgePointStatsRepository: com.sunjk.sunjktool.domain.repository.KnowledgePointStatsRepository,
    apiPreferences: ApiPreferences,
    syncPreferencesManager: SyncPreferencesManager,
    sharedTransitionScope: SharedTransitionScope? = null,
    modifier: Modifier = Modifier
) {
    val animEnabled = LocalAnimationEnabled.current

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // ===== 首页 (bottom nav) =====
        composable(
            route = Screen.Home.route,
            enterTransition = if (animEnabled) {
                {
                    if (tabDirection(initialState, targetState)) forwardEnter(this)
                    else backEnter(this)
                }
            } else null,
            exitTransition = if (animEnabled) {
                {
                    if (tabDirection(initialState, targetState)) forwardExit(this)
                    else backExit(this)
                }
            } else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            HomeScreen(
                viewModel = viewModel(
                    factory = HomeVMF(logRepository, homeModuleRepository, countdownRepository, weatherRepository, pomodoroManager, deepSeekRepository, reviewHelper, reviewDao, habitRepository, notebookRepository, tickTickRepository)
                ),
                onNavigateToNotebook = { notebookId ->
                    navController.navigate(Screen.NotebookDetail.createRoute(notebookId))
                },
                onNavigateToTodo = {
                    navController.navigate(Screen.Todo.route)
                },
                onNavigateToEdit = { logId ->
                    navController.navigate(Screen.LogEdit.createRoute(logId))
                },
                onNavigateToDetail = { logId ->
                    navController.navigate(Screen.LogDetail.createRoute(logId))
                },
                onNavigateToWeatherDetail = {
                    navController.navigate(Screen.WeatherDetail.route)
                },
                onNavigateToCountdownList = {
                    navController.navigate(Screen.CountdownList.route)
                },
                onNavigateToLearningRecord = {
                    navController.navigate(Screen.LearningRecordList.route)
                },
                onNavigateToPomodoro = {
                    navController.navigate(Screen.Pomodoro.route)
                },
                onNavigateToReview = {
                    navController.navigate(Screen.ReviewList.route)
                },
                onNavigateToHabits = {
                    navController.navigate(Screen.HabitList.route)
                },
                onNavigateToDeepSeek = {
                    navController.navigate(Screen.DeepSeekBalance.route)
                },
                animatedVisibilityScope = this,
                sharedTransitionScope = sharedTransitionScope,
                onOpenAiTask = { task ->
                    when (task.type) {
                        com.sunjk.sunjktool.di.AiTaskType.SUMMARY,
                        com.sunjk.sunjktool.di.AiTaskType.SELF_CHECK -> navController.navigate(Screen.LogDetail.createRoute(task.logId))
                        com.sunjk.sunjktool.di.AiTaskType.FLASHCARDS -> navController.navigate(Screen.FlashcardHub.createRoute(task.logId))
                        com.sunjk.sunjktool.di.AiTaskType.QUESTION_BANK -> navController.navigate(Screen.QuestionBankDetail.createRoute(task.logId))
                    }
                }
            )
        }

        // ===== 工具 (bottom nav) =====
        composable(
            route = Screen.Tools.route,
            enterTransition = if (animEnabled) {
                {
                    if (tabDirection(initialState, targetState)) forwardEnter(this)
                    else backEnter(this)
                }
            } else null,
            exitTransition = if (animEnabled) {
                {
                    if (tabDirection(initialState, targetState)) forwardExit(this)
                    else backExit(this)
                }
            } else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            com.sunjk.sunjktool.feature.tools.ToolsScreen(
                onNavigateToCountdown = {
                    navController.navigate(Screen.CountdownList.route)
                },
                onNavigateToWeather = {
                    navController.navigate(Screen.WeatherDetail.route)
                },
                onNavigateToLearningRecord = {
                    navController.navigate(Screen.LearningRecordList.route)
                },
                onNavigateToPomodoro = {
                    navController.navigate(Screen.Pomodoro.route)
                },
                onNavigateToDeepSeek = {
                    navController.navigate(Screen.DeepSeekBalance.route)
                },
                onNavigateToReview = {
                    navController.navigate(Screen.ReviewList.route)
                },
                onNavigateToHabits = {
                    navController.navigate(Screen.HabitList.route)
                },
                onNavigateToHomeEdit = {
                    navController.navigate(Screen.HomeEdit.route)
                },
                onNavigateToLifeLog = {
                    navController.navigate(Screen.LifeLogList.route)
                },
                onNavigateToTodo = {
                    navController.navigate(Screen.Todo.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ===== 我的 (bottom nav) =====
        composable(
            route = Screen.Mine.route,
            enterTransition = if (animEnabled) {
                {
                    if (tabDirection(initialState, targetState)) forwardEnter(this)
                    else backEnter(this)
                }
            } else null,
            exitTransition = if (animEnabled) {
                {
                    if (tabDirection(initialState, targetState)) forwardExit(this)
                    else backExit(this)
                }
            } else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            com.sunjk.sunjktool.feature.mine.MineScreen()
        }

        // ===== 编辑日志 (push) =====
        composable(
            route = Screen.LogEdit.route,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("notebookId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "sunjktool://learning_log/edit" }),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: -1L
            val notebookId = backStackEntry.arguments?.getLong("notebookId")?.takeIf { it != -1L }
            LogEditScreen(
                viewModel = viewModel(
                    key = "log_edit_$logId",
                    factory = LogEditVMF(
                        logRepository,
                        reviewHelper,
                        notebookRepository,
                        if (logId == -1L) null else logId,
                        notebookId
                    )
                ),
                onNavigateBack = { navController.popBackStack() },
                animatedVisibilityScope = this,
                sharedTransitionScope = sharedTransitionScope
            )
        }

        // ===== 日志详情 (push) =====
        composable(
            route = Screen.LogDetail.route,
            arguments = listOf(
                navArgument("logId") { type = NavType.LongType },
                navArgument("heading") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: return@composable
            val heading = backStackEntry.arguments?.getString("heading")
            LogDetailScreen(
                viewModel = viewModel(
                    key = "log_detail_$logId",
                    factory = LogDetailVMF(logRepository, reviewHelper, deepSeekApi, flashcardRepository, reviewNoteRepository, apiPreferences, logId, questionBankRepository)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.LogEdit.createRoute(logId))
                },
                onNavigateToFlashcards = {
                    navController.navigate(Screen.Flashcard.createRoute(logId))
                },
                onNavigateToFlashcardHub = {
                    navController.navigate(Screen.FlashcardHub.createRoute(logId))
                },
                onNavigateToReviewNotes = {
                    navController.navigate(Screen.ReviewNoteList.createRoute(logId))
                },
                onNavigateToReviewNoteDetail = { noteId ->
                    navController.navigate(Screen.ReviewNoteDetail.createRoute(logId, noteId))
                },
                onNavigateToQuestionLinks = { qLogId, headingId ->
                    navController.navigate(Screen.QuestionLinkList.createRoute(qLogId, headingId))
                },
                initialHeading = heading,
                animatedVisibilityScope = this,
                sharedTransitionScope = sharedTransitionScope
            )
        }

        // ===== 章节引用题目列表 (push) =====
        composable(
            route = Screen.QuestionLinkList.route,
            arguments = listOf(
                navArgument("logId") { type = NavType.LongType },
                navArgument("headingId") { type = NavType.StringType }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val qLogId = backStackEntry.arguments?.getLong("logId") ?: return@composable
            val headingId = backStackEntry.arguments?.getString("headingId") ?: return@composable
            QuestionLinkListScreen(
                viewModel = viewModel(
                    key = "qlink_${qLogId}_$headingId",
                    factory = QuestionLinkListVMF(questionBankRepository, qLogId, headingId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onQuestionClick = { categoryId, questionId ->
                    navController.navigate(Screen.QuestionBankDetail.createRoute(categoryId, questionId))
                }
            )
        }

        // ===== 倒数日列表 (push) =====
        composable(
            route = Screen.CountdownList.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            CountdownListScreen(
                viewModel = viewModel(factory = CountdownListVMF(countdownRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { countdownId ->
                    navController.navigate(Screen.CountdownEdit.createRoute(countdownId))
                }
            )
        }

        // ===== 倒数日编辑 (push) =====
        composable(
            route = Screen.CountdownEdit.route,
            arguments = listOf(
                navArgument("countdownId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val countdownId = backStackEntry.arguments?.getLong("countdownId") ?: -1L
            CountdownEditScreen(
                viewModel = viewModel(
                    factory = CountdownEditVMF(
                        countdownRepository,
                        if (countdownId == -1L) null else countdownId
                    )
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 编辑首页 (push from home FAB) =====
        composable(
            route = Screen.HomeEdit.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            HomeEditScreen(
                viewModel = viewModel(factory = HomeEditVMF(homeModuleRepository, countdownRepository, habitRepository)),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 学习记录时间轴 (push) =====
        composable(
            route = Screen.LearningRecordList.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            TimelineListScreen(
                viewModel = viewModel(factory = TimelineListVMF(logRepository, pomodoroManager)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { logId ->
                    navController.navigate(Screen.LogDetail.createRoute(logId))
                },
                animatedVisibilityScope = this,
                sharedTransitionScope = sharedTransitionScope
            )
        }

        // ===== 复盘列表 (push) =====
        composable(route = Screen.ReviewList.route, enterTransition = forwardEnter, exitTransition = forwardExit, popEnterTransition = backEnter, popExitTransition = backExit) {
            com.sunjk.sunjktool.feature.review.ReviewListScreen(
                viewModel = viewModel(factory = ReviewListVMF(reviewDao, logDao)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { logId -> navController.navigate(Screen.LogDetail.createRoute(logId)) },
                onNavigateToHistory = { navController.navigate(Screen.ReviewHistory.route) }
            )
        }

        // ===== 历史复盘 (push) =====
        composable(route = Screen.ReviewHistory.route, enterTransition = forwardEnter, exitTransition = forwardExit, popEnterTransition = backEnter, popExitTransition = backExit) {
            com.sunjk.sunjktool.feature.review.ReviewHistoryScreen(
                viewModel = viewModel(factory = ReviewHistoryVMF(reviewDao, logDao)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { logId -> navController.navigate(Screen.LogDetail.createRoute(logId)) }
            )
        }

        // ===== 习惯列表 (push) =====
        composable(route = Screen.HabitList.route, enterTransition = forwardEnter, exitTransition = forwardExit, popEnterTransition = backEnter, popExitTransition = backExit) {
            com.sunjk.sunjktool.feature.habit.list.HabitListScreen(
                viewModel = viewModel(factory = HabitListVMF(habitRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { habitId -> navController.navigate(Screen.HabitEdit.createRoute(habitId)) }
            )
        }

        // ===== 习惯编辑 (push) =====
        composable(
            route = Screen.HabitEdit.route,
            arguments = listOf(navArgument("habitId") { type = NavType.LongType; defaultValue = -1L }),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId")?.takeIf { it != -1L }
            com.sunjk.sunjktool.feature.habit.edit.HabitEditScreen(
                viewModel = viewModel(factory = HabitEditVMF(habitRepository, habitId)),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== DeepSeek 额度 (push) =====
        composable(route = Screen.DeepSeekBalance.route, enterTransition = forwardEnter, exitTransition = forwardExit, popEnterTransition = backEnter, popExitTransition = backExit) {
            DeepSeekScreen(viewModel = viewModel(factory = DeepSeekVMF(deepSeekRepository)), onNavigateBack = { navController.popBackStack() })
        }

        // ===== 番茄钟 (push) =====
        composable(
            route = Screen.Pomodoro.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            PomodoroScreen(
                viewModel = viewModel(factory = PomodoroVMF(pomodoroManager)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.PomodoroHistory.route) },
                animatedVisibilityScope = this,
                sharedTransitionScope = sharedTransitionScope
            )
        }

        // ===== 番茄钟历史 (push) =====
        composable(
            route = Screen.PomodoroHistory.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            PomodoroHistoryScreen(
                viewModel = viewModel(factory = PomodoroVMF(pomodoroManager)),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 天气详情 (push) =====
        composable(
            route = Screen.WeatherDetail.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            WeatherDetailScreen(
                viewModel = viewModel(factory = WeatherDetailVMF(weatherRepository)),
                onNavigateBack = { navController.popBackStack() },
                animatedVisibilityScope = this,
                sharedTransitionScope = sharedTransitionScope
            )
        }

        // ===== 设置 (push) =====
        composable(
            route = Screen.Settings.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            SettingsScreen(
                viewModel = viewModel(factory = SettingsVMF(syncEngine, apiPreferences, tickTickRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateTo = { section ->
                    val route = when (section) {
                        SettingsSection.API_KEYS -> Screen.SettingsApiKeys.route
                        SettingsSection.AI -> Screen.SettingsAi.route
                        SettingsSection.WEBDAV -> Screen.SettingsWebDav.route
                        SettingsSection.TICKTICK -> Screen.SettingsTickTick.route
                        SettingsSection.DISPLAY -> Screen.SettingsDisplay.route
                        SettingsSection.ABOUT -> Screen.SettingsAbout.route
                    }
                    navController.navigate(route)
                }
            )
        }

        // ---- 设置二级页面: Screen.SettingsApiKeys ----
        composable(
            route = Screen.SettingsApiKeys.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { entry ->
            val settingsEntry = remember(entry) { navController.getBackStackEntry(Screen.Settings.route) }
            val vm: SettingsViewModel = viewModel(viewModelStoreOwner = settingsEntry, factory = SettingsVMF(syncEngine, apiPreferences, tickTickRepository))
            SettingsApiKeysScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        // ---- 设置二级页面: Screen.SettingsAi ----
        composable(
            route = Screen.SettingsAi.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { entry ->
            val settingsEntry = remember(entry) { navController.getBackStackEntry(Screen.Settings.route) }
            val vm: SettingsViewModel = viewModel(viewModelStoreOwner = settingsEntry, factory = SettingsVMF(syncEngine, apiPreferences, tickTickRepository))
            SettingsAiScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        // ---- 设置二级页面: Screen.SettingsWebDav ----
        composable(
            route = Screen.SettingsWebDav.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { entry ->
            val settingsEntry = remember(entry) { navController.getBackStackEntry(Screen.Settings.route) }
            val vm: SettingsViewModel = viewModel(viewModelStoreOwner = settingsEntry, factory = SettingsVMF(syncEngine, apiPreferences, tickTickRepository))
            SettingsWebDavScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        // ---- 设置二级页面: Screen.SettingsTickTick ----
        composable(
            route = Screen.SettingsTickTick.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { entry ->
            val settingsEntry = remember(entry) { navController.getBackStackEntry(Screen.Settings.route) }
            val vm: SettingsViewModel = viewModel(viewModelStoreOwner = settingsEntry, factory = SettingsVMF(syncEngine, apiPreferences, tickTickRepository))
            SettingsTickTickScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        // ---- 设置二级页面: Screen.SettingsDisplay ----
        composable(
            route = Screen.SettingsDisplay.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { entry ->
            val settingsEntry = remember(entry) { navController.getBackStackEntry(Screen.Settings.route) }
            val vm: SettingsViewModel = viewModel(viewModelStoreOwner = settingsEntry, factory = SettingsVMF(syncEngine, apiPreferences, tickTickRepository))
            SettingsDisplayScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        // ---- 设置二级页面: Screen.SettingsAbout ----
        composable(
            route = Screen.SettingsAbout.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { entry ->
            val settingsEntry = remember(entry) { navController.getBackStackEntry(Screen.Settings.route) }
            val vm: SettingsViewModel = viewModel(viewModelStoreOwner = settingsEntry, factory = SettingsVMF(syncEngine, apiPreferences, tickTickRepository))
            SettingsAboutScreen(viewModel = vm, onBack = { navController.popBackStack() }, onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) })
        }

        composable(
            route = Screen.Onboarding.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            OnboardingScreen(
                apiPreferences = apiPreferences,
                syncPreferencesManager = syncPreferencesManager,
                onFinished = { navController.popBackStack() }
            )
        }

        // ===== WebDAV 同步设置 (push, deprecated — kept for backward compat) =====
        composable(
            route = Screen.SyncSettings.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            SyncSettingsScreen(
                viewModel = viewModel(factory = SyncSettingsVMF(syncEngine)),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== AI 闪卡 (push) =====
        composable(
            route = Screen.Flashcard.route,
            arguments = listOf(
                navArgument("logId") { type = NavType.LongType },
                navArgument("sessionId") { type = NavType.LongType; defaultValue = -1L }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: return@composable
            val sessionId = backStackEntry.arguments?.getLong("sessionId")?.takeIf { it != -1L }
            val autoAdvance = androidx.compose.ui.platform.LocalContext.current
                .getSharedPreferences("flashcard_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("auto_advance", false)
            FlashcardScreen(
                viewModel = viewModel(
                    key = "flashcard_${sessionId ?: logId}",
                    factory = FlashcardVMF(flashcardRepository, reviewNoteRepository, knowledgePointStatsRepository, logId, sessionId, autoAdvance)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== AI 闪卡中枢 (push) =====
        composable(
            route = Screen.FlashcardHub.route,
            arguments = listOf(navArgument("logId") { type = NavType.LongType }),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: return@composable
            FlashcardHubScreen(
                viewModel = viewModel(
                    key = "flashcard_hub_$logId",
                    factory = FlashcardHubVMF(flashcardRepository, deepSeekApi, logRepository, apiPreferences, logId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSession = { sessionId ->
                    navController.navigate(Screen.Flashcard.createRoute(logId, sessionId))
                },
                onGenerateFlashcards = {}
            )
        }

        // ===== 复盘心得列表 (push) =====
        composable(
            route = Screen.ReviewNoteList.route,
            arguments = listOf(navArgument("logEntryId") { type = NavType.LongType }),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val logEntryId = backStackEntry.arguments?.getLong("logEntryId") ?: return@composable
            ReviewNoteListScreen(
                viewModel = viewModel(
                    key = "review_note_list_$logEntryId",
                    factory = ReviewNoteListVMF(reviewNoteRepository, logEntryId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdd = { navController.navigate(Screen.ReviewNoteEdit.createRoute(logEntryId, null)) },
                onNavigateToDetail = { noteId ->
                    navController.navigate(Screen.ReviewNoteDetail.createRoute(logEntryId, noteId))
                }
            )
        }

        // ===== 复盘心得编辑 (push) =====
        composable(
            route = Screen.ReviewNoteEdit.route,
            arguments = listOf(
                navArgument("logEntryId") { type = NavType.LongType },
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val logEntryId = backStackEntry.arguments?.getLong("logEntryId") ?: return@composable
            val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it != -1L }
            ReviewNoteEditScreen(
                viewModel = viewModel(
                    key = "review_note_edit_${noteId ?: logEntryId}",
                    factory = ReviewNoteEditVMF(reviewNoteRepository, logEntryId, noteId)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 复盘心得详情 (push) =====
        composable(
            route = Screen.ReviewNoteDetail.route,
            arguments = listOf(
                navArgument("logEntryId") { type = NavType.LongType },
                navArgument("noteId") { type = NavType.LongType }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val logEntryId = backStackEntry.arguments?.getLong("logEntryId") ?: return@composable
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            ReviewNoteDetailScreen(
                viewModel = viewModel(
                    key = "review_note_detail_$noteId",
                    factory = ReviewNoteDetailVMF(reviewNoteRepository, noteId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { targetNoteId ->
                    navController.navigate(Screen.ReviewNoteEdit.createRoute(logEntryId, targetNoteId))
                }
            )
        }

        // ===== 笔记本列表 (bottom nav) =====
        composable(
            route = Screen.NotebookList.route,
            enterTransition = if (animEnabled) {
                { if (tabDirection(initialState, targetState)) forwardEnter(this) else backEnter(this) }
            } else null,
            exitTransition = if (animEnabled) {
                { if (tabDirection(initialState, targetState)) forwardExit(this) else backExit(this) }
            } else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            NotebookListScreen(
                viewModel = viewModel(factory = NotebookListVMF(notebookRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { notebookId ->
                    navController.navigate(Screen.NotebookDetail.createRoute(notebookId))
                },
                onNavigateToCreate = { parentId ->
                    navController.navigate(Screen.NotebookEdit.createRoute(parentId = parentId))
                }
            )
        }

        // ===== 笔记本详情 (push) =====
        composable(
            route = Screen.NotebookDetail.route,
            arguments = listOf(navArgument("notebookId") { type = NavType.LongType }),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getLong("notebookId") ?: return@composable
            NotebookDetailScreen(
                viewModel = viewModel(
                    key = "notebook_detail_$notebookId",
                    factory = NotebookDetailVMF(notebookRepository, logRepository, notebookId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubNotebook = { subId ->
                    navController.navigate(Screen.NotebookDetail.createRoute(subId))
                },
                onNavigateToLogDetail = { logId ->
                    navController.navigate(Screen.LogDetail.createRoute(logId))
                },
                onNavigateToEdit = {
                    navController.navigate(Screen.NotebookEdit.createRoute(notebookId = notebookId))
                },
                onNavigateToAddLog = {
                    navController.navigate(Screen.LogEdit.createRoute(notebookId = notebookId))
                },
                onNavigateToAddSubNotebook = {
                    navController.navigate(Screen.NotebookEdit.createRoute(parentId = notebookId))
                },
                onNavigateToBreadcrumb = { breadcrumbId ->
                    navController.navigate(Screen.NotebookDetail.createRoute(breadcrumbId)) {
                        // Pop any existing notebook detail screens to avoid deep stack
                        popUpTo(Screen.NotebookList.route)
                    }
                }
            )
        }

        // ===== 笔记本编辑 (push) =====
        composable(
            route = Screen.NotebookEdit.route,
            arguments = listOf(
                navArgument("notebookId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("parentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getLong("notebookId")?.takeIf { it != -1L }
            val parentId = backStackEntry.arguments?.getLong("parentId")?.takeIf { it != -1L }
            NotebookEditScreen(
                viewModel = viewModel(
                    key = "notebook_edit_${notebookId ?: "new"}",
                    factory = NotebookEditVMF(notebookRepository, notebookId, parentId)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 题集列表 (bottom nav) =====
        composable(
            route = Screen.QuestionBankList.route,
            enterTransition = if (animEnabled) {
                { if (tabDirection(initialState, targetState)) forwardEnter(this) else backEnter(this) }
            } else null,
            exitTransition = if (animEnabled) {
                { if (tabDirection(initialState, targetState)) forwardExit(this) else backExit(this) }
            } else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            QuestionBankListScreen(
                viewModel = viewModel(factory = QuestionBankListVMF(questionBankRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { categoryId ->
                    navController.navigate(Screen.QuestionBankDetail.createRoute(categoryId))
                },
                onNavigateToCreate = { parentId ->
                    navController.navigate(Screen.QuestionBankEdit.createRoute(parentId = parentId))
                }
            )
        }

        // ===== 题集详情 (push) =====
        composable(
            route = Screen.QuestionBankDetail.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("questionId") { type = NavType.LongType; defaultValue = -1L }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
            val questionId = backStackEntry.arguments?.getLong("questionId")?.takeIf { it > 0 }
            QuestionBankDetailScreen(
                viewModel = viewModel(
                    key = "qb_detail_$categoryId",
                    factory = QuestionBankDetailVMF(questionBankRepository, deepSeekApi, logRepository, notebookRepository, apiPreferences, categoryId, initialQuestionId = questionId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubCategory = { subId ->
                    navController.navigate(Screen.QuestionBankDetail.createRoute(subId))
                },
                onNavigateToEdit = {
                    navController.navigate(Screen.QuestionBankEdit.createRoute(categoryId = categoryId))
                },
                onNavigateToAddSubCategory = {
                    navController.navigate(Screen.QuestionBankEdit.createRoute(parentId = categoryId))
                },
                onNavigateToBreadcrumb = { breadcrumbId ->
                    navController.navigate(Screen.QuestionBankDetail.createRoute(breadcrumbId)) {
                        popUpTo(Screen.QuestionBankList.route)
                    }
                },
                initialQuestionId = questionId,
                onNavigateToLogDetail = { logId, heading ->
                    navController.navigate(Screen.LogDetail.createRoute(logId, heading))
                }
            )
        }

        // ===== 题集编辑 (push) =====
        composable(
            route = Screen.QuestionBankEdit.route,
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("parentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            val parentId = backStackEntry.arguments?.getLong("parentId")?.takeIf { it != -1L }
            QuestionBankEditScreen(
                viewModel = viewModel(
                    key = "qb_edit_${categoryId ?: "new"}",
                    factory = QuestionBankEditVMF(questionBankRepository, categoryId, parentId)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 概览 (bottom nav) =====
        composable(
            route = Screen.Overview.route,
            enterTransition = if (animEnabled) {
                { if (tabDirection(initialState, targetState)) forwardEnter(this) else backEnter(this) }
            } else null,
            exitTransition = if (animEnabled) {
                { if (tabDirection(initialState, targetState)) forwardExit(this) else backExit(this) }
            } else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            com.sunjk.sunjktool.feature.overview.OverviewScreen(
                viewModel = viewModel(factory = com.sunjk.sunjktool.di.OverviewVMF(logRepository, pomodoroRecordDao, habitRepository, reviewDao, lifeLogRepository, tickTickRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogDetail = { id -> navController.navigate(Screen.LogDetail.createRoute(id)) },
                onNavigateToReviewList = { navController.navigate(Screen.ReviewList.route) },
                onNavigateToLifeLogDetail = { id -> navController.navigate(Screen.LifeLogDetail.createRoute(id)) }
            )
        }

        // ===== 待办 (push) =====
        composable(
            route = Screen.Todo.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            com.sunjk.sunjktool.feature.todo.TodoScreen(
                viewModel = viewModel(factory = com.sunjk.sunjktool.di.TodoVMF(tickTickRepository)),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 生活记录列表 (push) =====
        composable(
            route = Screen.LifeLogList.route,
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) {
            com.sunjk.sunjktool.feature.lifelog.list.LifeLogListScreen(
                viewModel = viewModel(factory = com.sunjk.sunjktool.di.LifeLogListVMF(lifeLogRepository)),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.LifeLogDetail.createRoute(id)) },
                onNavigateToCreate = { navController.navigate(Screen.LifeLogEdit.createRoute()) }
            )
        }

        // ===== 生活记录详情 (push) =====
        composable(
            route = Screen.LifeLogDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: return@composable
            com.sunjk.sunjktool.feature.lifelog.detail.LifeLogDetailScreen(
                viewModel = viewModel(
                    key = "life_log_detail_$entryId",
                    factory = com.sunjk.sunjktool.di.LifeLogDetailVMF(lifeLogRepository, entryId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.LifeLogEdit.createRoute(entryId)) }
            )
        }

        // ===== 生活记录编辑 (push) =====
        composable(
            route = Screen.LifeLogEdit.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType; defaultValue = -1L }),
            enterTransition = if (animEnabled) forwardEnter else null,
            exitTransition = if (animEnabled) forwardExit else null,
            popEnterTransition = if (animEnabled) backEnter else null,
            popExitTransition = if (animEnabled) backExit else null
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId")?.takeIf { it != -1L }
            com.sunjk.sunjktool.feature.lifelog.edit.LifeLogEditScreen(
                viewModel = viewModel(
                    key = "life_log_edit_${entryId ?: "new"}",
                    factory = com.sunjk.sunjktool.di.LifeLogEditVMF(lifeLogRepository, entryId)
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
