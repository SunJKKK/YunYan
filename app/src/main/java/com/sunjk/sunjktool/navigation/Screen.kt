package com.sunjk.sunjktool.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Tools : Screen("tools")
    data object Mine : Screen("mine")
    data object LogEdit : Screen("learning_log/edit?logId={logId}") {
        fun createRoute(logId: Long? = null): String =
            if (logId != null) "learning_log/edit?logId=$logId"
            else "learning_log/edit?logId=-1"
    }
    data object LogDetail : Screen("learning_log/{logId}") {
        fun createRoute(logId: Long) = "learning_log/$logId"
    }
    data object CountdownList : Screen("countdown/list")
    data object CountdownEdit : Screen("countdown/edit?countdownId={countdownId}") {
        fun createRoute(countdownId: Long? = null): String =
            if (countdownId != null) "countdown/edit?countdownId=$countdownId"
            else "countdown/edit?countdownId=-1"
    }
}
