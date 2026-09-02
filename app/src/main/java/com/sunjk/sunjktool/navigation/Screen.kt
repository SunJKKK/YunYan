package com.sunjk.sunjktool.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Tools : Screen("tools")
    data object Mine : Screen("mine")
    data object LogEdit : Screen("learning_log/edit?logId={logId}&notebookId={notebookId}") {
        fun createRoute(logId: Long? = null, notebookId: Long? = null): String {
            val lid = logId ?: -1L
            val nid = notebookId ?: -1L
            return "learning_log/edit?logId=$lid&notebookId=$nid"
        }
    }
    data object LogDetail : Screen("learning_log/{logId}?heading={heading}") {
        fun createRoute(logId: Long, heading: String? = null): String =
            if (heading != null && heading.isNotBlank()) "learning_log/$logId?heading=$heading"
            else "learning_log/$logId"
    }
    data object CountdownList : Screen("countdown/list")
    data object CountdownEdit : Screen("countdown/edit?countdownId={countdownId}") {
        fun createRoute(countdownId: Long? = null): String =
            if (countdownId != null) "countdown/edit?countdownId=$countdownId"
            else "countdown/edit?countdownId=-1"
    }
    data object HomeEdit : Screen("home/edit")
    data object WeatherDetail : Screen("weather/detail")
    data object LearningRecordList : Screen("learning_record/list")
    data object Pomodoro : Screen("pomodoro")
    data object PomodoroHistory : Screen("pomodoro/history")
    data object DeepSeekBalance : Screen("deepseek/balance")
    data object ReviewList : Screen("review/list")
    data object ReviewHistory : Screen("review/history")
    data object HabitList : Screen("habit/list")
    data object HabitEdit : Screen("habit/edit?habitId={habitId}") {
        fun createRoute(habitId: Long? = null): String =
            if (habitId != null) "habit/edit?habitId=$habitId"
            else "habit/edit?habitId=-1"
    }
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
    data object SettingsApiKeys : Screen("settings/api_keys")
    data object SettingsAi : Screen("settings/ai")
    data object SettingsWebDav : Screen("settings/webdav")
    data object SettingsTickTick : Screen("settings/ticktick")
    data object SettingsDisplay : Screen("settings/display")
    data object SettingsAbout : Screen("settings/about")
    data object SyncSettings : Screen("sync/settings")
    data object Flashcard : Screen("learning_log/{logId}/flashcard?sessionId={sessionId}") {
        fun createRoute(logId: Long, sessionId: Long? = null): String =
            if (sessionId != null) "learning_log/$logId/flashcard?sessionId=$sessionId"
            else "learning_log/$logId/flashcard?sessionId=-1"
    }
    data object FlashcardHub : Screen("learning_log/{logId}/flashcard/hub") {
        fun createRoute(logId: Long) = "learning_log/$logId/flashcard/hub"
    }
    data object ReviewNoteList : Screen("review_note/list/{logEntryId}") {
        fun createRoute(logEntryId: Long) = "review_note/list/$logEntryId"
    }
    data object ReviewNoteEdit : Screen("review_note/edit/{logEntryId}?noteId={noteId}") {
        fun createRoute(logEntryId: Long, noteId: Long? = null): String =
            if (noteId != null) "review_note/edit/$logEntryId?noteId=$noteId"
            else "review_note/edit/$logEntryId?noteId=-1"
    }
    data object ReviewNoteDetail : Screen("review_note/detail/{logEntryId}/{noteId}") {
        fun createRoute(logEntryId: Long, noteId: Long) = "review_note/detail/$logEntryId/$noteId"
    }

    data object NotebookList : Screen("notebook/list")
    data object NotebookDetail : Screen("notebook/detail/{notebookId}") {
        fun createRoute(notebookId: Long) = "notebook/detail/$notebookId"
    }
    data object NotebookEdit : Screen("notebook/edit?notebookId={notebookId}&parentId={parentId}") {
        fun createRoute(notebookId: Long? = null, parentId: Long? = null): String {
            val id = notebookId ?: -1L
            val pid = parentId ?: -1L
            return "notebook/edit?notebookId=$id&parentId=$pid"
        }
    }

    data object LifeLogList : Screen("life_log/list")
    data object LifeLogDetail : Screen("life_log/detail/{entryId}") {
        fun createRoute(entryId: Long) = "life_log/detail/$entryId"
    }
    data object LifeLogEdit : Screen("life_log/edit?entryId={entryId}") {
        fun createRoute(entryId: Long? = null): String {
            val id = entryId ?: -1L
            return "life_log/edit?entryId=$id"
        }
    }

    data object Overview : Screen("overview")

    data object LearningStats : Screen("learning_stats")

    data object Todo : Screen("todo")

    data object QuestionBankList : Screen("question_bank/list")
    data object QuestionBankDetail : Screen("question_bank/detail/{categoryId}?questionId={questionId}") {
        fun createRoute(categoryId: Long, questionId: Long? = null): String =
            if (questionId != null) "question_bank/detail/$categoryId?questionId=$questionId"
            else "question_bank/detail/$categoryId?questionId=-1"
    }
    data object QuestionLinkList : Screen("question_links/{logId}/{headingId}") {
        fun createRoute(logId: Long, headingId: String) = "question_links/$logId/$headingId"
    }
    data object QuestionBankEdit : Screen("question_bank/edit?categoryId={categoryId}&parentId={parentId}") {
        fun createRoute(categoryId: Long? = null, parentId: Long? = null): String {
            val cid = categoryId ?: -1L
            val pid = parentId ?: -1L
            return "question_bank/edit?categoryId=$cid&parentId=$pid"
        }
    }
}
