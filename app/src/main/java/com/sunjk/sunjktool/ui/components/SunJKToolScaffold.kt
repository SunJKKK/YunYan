package com.sunjk.sunjktool.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.navigation.Screen
import com.sunjk.sunjktool.navigation.SunJKToolNavHost
import com.sunjk.sunjktool.navigation.TopLevelDestination
import com.sunjk.sunjktool.navigation.TRANSITION_DURATION

@Composable
fun SunJKToolScaffold(
    logRepository: LogRepository,
    countdownRepository: CountdownRepository,
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
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    animationSpec = tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)
                ) { it } + fadeIn(tween(TRANSITION_DURATION, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(
                    animationSpec = tween(TRANSITION_DURATION, easing = FastOutLinearInEasing)
                ) { it } + fadeOut(tween(TRANSITION_DURATION, easing = FastOutLinearInEasing))
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
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
        SunJKToolNavHost(
            navController = navController,
            logRepository = logRepository,
            countdownRepository = countdownRepository,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
