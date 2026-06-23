package com.sunjk.sunjktool.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sunjk.sunjktool.di.CountdownEditVMF
import com.sunjk.sunjktool.di.CountdownListVMF
import com.sunjk.sunjktool.di.HomeVMF
import com.sunjk.sunjktool.di.LogDetailVMF
import com.sunjk.sunjktool.di.LogEditVMF
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.feature.countdown.edit.CountdownEditScreen
import com.sunjk.sunjktool.feature.countdown.list.CountdownListScreen
import com.sunjk.sunjktool.feature.home.HomeScreen
import com.sunjk.sunjktool.feature.learninglog.detail.LogDetailScreen
import com.sunjk.sunjktool.feature.learninglog.edit.LogEditScreen

internal const val TRANSITION_DURATION = 300

private fun tabIndex(route: String?): Int =
    TopLevelDestination.entries.indexOfFirst { it.screen.route == route }

// ---- 统一动画：前进 ----
// 新页面从右侧全屏滑入 + 淡入；旧页面向左微移 1/3 屏宽 + 淡出
private val forwardEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing))
}
private val forwardExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing)) { -it / 3 } +
        fadeOut(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing))
}

// ---- 统一动画：返回 ----
// 上级页面从左微移 1/3 屏宽滑入 + 淡入；当前页面向右全屏滑出 + 淡出
private val backEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)) { -it / 3 } +
        fadeIn(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing))
}
private val backExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing)) { it } +
        fadeOut(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing))
}

// Tab 目的地进入方向判断：从子页面返回 → back；否则比较 tab 索引
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(
    from: NavBackStackEntry,
    to: NavBackStackEntry
): Boolean { // true = forward, false = back
    val fromIdx = tabIndex(from.destination.route)
    val toIdx = tabIndex(to.destination.route)
    // 来自子页面 (fromIdx == -1) → 返回上级 → back
    if (fromIdx == -1) return false
    // 去往子页面 (toIdx == -1) → 进入更深 → forward
    if (toIdx == -1) return true
    // Tab 之间：索引增大 = 前进
    return toIdx >= fromIdx
}

@Composable
fun SunJKToolNavHost(
    navController: NavHostController,
    logRepository: LogRepository,
    countdownRepository: CountdownRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // ===== 首页 (bottom nav) =====
        composable(
            route = Screen.Home.route,
            enterTransition = {
                if (tabDirection(initialState, targetState)) forwardEnter(this)
                else backEnter(this)
            },
            exitTransition = {
                if (tabDirection(initialState, targetState)) forwardExit(this)
                else backExit(this)
            },
            popEnterTransition = backEnter,
            popExitTransition = backExit
        ) {
            HomeScreen(
                viewModel = viewModel(factory = HomeVMF(logRepository)),
                onNavigateToEdit = { logId ->
                    navController.navigate(Screen.LogEdit.createRoute(logId))
                },
                onNavigateToDetail = { logId ->
                    navController.navigate(Screen.LogDetail.createRoute(logId))
                }
            )
        }

        // ===== 工具 (bottom nav) =====
        composable(
            route = Screen.Tools.route,
            enterTransition = {
                if (tabDirection(initialState, targetState)) forwardEnter(this)
                else backEnter(this)
            },
            exitTransition = {
                if (tabDirection(initialState, targetState)) forwardExit(this)
                else backExit(this)
            },
            popEnterTransition = backEnter,
            popExitTransition = backExit
        ) {
            com.sunjk.sunjktool.feature.tools.ToolsScreen(
                onNavigateToCountdown = {
                    navController.navigate(Screen.CountdownList.route)
                }
            )
        }

        // ===== 我的 (bottom nav) =====
        composable(
            route = Screen.Mine.route,
            enterTransition = {
                if (tabDirection(initialState, targetState)) forwardEnter(this)
                else backEnter(this)
            },
            exitTransition = {
                if (tabDirection(initialState, targetState)) forwardExit(this)
                else backExit(this)
            },
            popEnterTransition = backEnter,
            popExitTransition = backExit
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
                }
            ),
            enterTransition = forwardEnter,
            exitTransition = forwardExit,
            popEnterTransition = backEnter,
            popExitTransition = backExit
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: -1L
            LogEditScreen(
                viewModel = viewModel(
                    factory = LogEditVMF(
                        logRepository,
                        if (logId == -1L) null else logId
                    )
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== 日志详情 (push) =====
        composable(
            route = Screen.LogDetail.route,
            arguments = listOf(
                navArgument("logId") { type = NavType.LongType }
            ),
            enterTransition = forwardEnter,
            exitTransition = forwardExit,
            popEnterTransition = backEnter,
            popExitTransition = backExit
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: return@composable
            LogDetailScreen(
                viewModel = viewModel(
                    factory = LogDetailVMF(logRepository, logId)
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.LogEdit.createRoute(logId))
                }
            )
        }

        // ===== 倒数日列表 (push) =====
        composable(
            route = Screen.CountdownList.route,
            enterTransition = forwardEnter,
            exitTransition = forwardExit,
            popEnterTransition = backEnter,
            popExitTransition = backExit
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
            enterTransition = forwardEnter,
            exitTransition = forwardExit,
            popEnterTransition = backEnter,
            popExitTransition = backExit
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
    }
}
