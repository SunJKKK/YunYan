package com.sunjk.sunjktool.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.HomeModuleRepository
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.WeatherRepository
import com.sunjk.sunjktool.feature.countdown.edit.CountdownEditViewModel
import com.sunjk.sunjktool.feature.countdown.list.CountdownListViewModel
import com.sunjk.sunjktool.feature.home.HomeViewModel
import com.sunjk.sunjktool.feature.home.edit.HomeEditViewModel
import com.sunjk.sunjktool.feature.learninglog.detail.LogDetailViewModel
import com.sunjk.sunjktool.feature.learninglog.edit.LogEditViewModel
import com.sunjk.sunjktool.domain.repository.HabitRepository
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.domain.repository.LifeLogRepository
import com.sunjk.sunjktool.feature.habit.edit.HabitEditViewModel
import com.sunjk.sunjktool.feature.habit.list.HabitListViewModel
import com.sunjk.sunjktool.feature.review.ReviewHistoryViewModel
import com.sunjk.sunjktool.feature.review.ReviewListViewModel
import com.sunjk.sunjktool.feature.deepseek.DeepSeekViewModel
import com.sunjk.sunjktool.feature.pomodoro.PomodoroViewModel
import com.sunjk.sunjktool.domain.repository.DeepSeekRepository
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.util.PomodoroManager
import com.sunjk.sunjktool.util.ReviewHelper
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.sync.SyncEngine
import com.sunjk.sunjktool.domain.repository.FlashcardRepository
import com.sunjk.sunjktool.feature.learninglog.flashcard.FlashcardHubViewModel
import com.sunjk.sunjktool.feature.learninglog.flashcard.FlashcardViewModel
import com.sunjk.sunjktool.feature.reviewnote.list.ReviewNoteListViewModel
import com.sunjk.sunjktool.feature.reviewnote.edit.ReviewNoteEditViewModel
import com.sunjk.sunjktool.feature.reviewnote.detail.ReviewNoteDetailViewModel
import com.sunjk.sunjktool.feature.settings.SettingsViewModel
import com.sunjk.sunjktool.feature.sync.SyncSettingsViewModel
import com.sunjk.sunjktool.feature.weather.detail.WeatherDetailViewModel

class SettingsVMF(
    private val syncEngine: SyncEngine,
    private val apiPreferences: ApiPreferences,
    private val tickTickRepository: com.sunjk.sunjktool.domain.repository.TickTickRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(syncEngine, apiPreferences, tickTickRepository) as T
}

class SyncSettingsVMF(
    private val syncEngine: SyncEngine
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SyncSettingsViewModel(syncEngine) as T
}

class ReviewListVMF(
    private val reviewDao: ReviewStatusDao,
    private val logDao: LogEntryDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReviewListViewModel(reviewDao, logDao) as T
}

class ReviewHistoryVMF(
    private val reviewDao: ReviewStatusDao,
    private val logDao: LogEntryDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReviewHistoryViewModel(reviewDao, logDao) as T
}

class HabitListVMF(private val repo: HabitRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HabitListViewModel(repo) as T
}

class HabitEditVMF(
    private val repo: HabitRepository,
    private val habitId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HabitEditViewModel(repo, habitId) as T
}

class HomeVMF(
    private val logRepo: LogRepository,
    private val homeModuleRepo: HomeModuleRepository,
    private val countdownRepo: CountdownRepository,
    private val weatherRepo: WeatherRepository,
    private val pomodoroManager: PomodoroManager,
    private val deepSeekRepo: DeepSeekRepository,
    private val reviewHelper: ReviewHelper,
    private val reviewDao: ReviewStatusDao,
    private val habitRepo: HabitRepository,
    private val notebookRepo: NotebookRepository,
    private val tickTickRepository: com.sunjk.sunjktool.domain.repository.TickTickRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(logRepo, homeModuleRepo, countdownRepo, weatherRepo, pomodoroManager, deepSeekRepo, reviewHelper, reviewDao, habitRepo, notebookRepo, tickTickRepository) as T
}

class LogEditVMF(
    private val repo: LogRepository,
    private val reviewHelper: ReviewHelper,
    private val notebookRepo: NotebookRepository,
    private val logId: Long?,
    private val preSelectedNotebookId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LogEditViewModel(repo, reviewHelper, notebookRepo, logId, preSelectedNotebookId) as T
}

class LogDetailVMF(
    private val repo: LogRepository,
    private val reviewHelper: ReviewHelper,
    private val deepSeekApi: DeepSeekApi,
    private val flashcardRepo: FlashcardRepository,
    private val reviewNoteRepo: ReviewNoteRepository,
    private val apiPreferences: ApiPreferences,
    private val logId: Long,
    private val questionBankRepo: QuestionBankRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LogDetailViewModel(repo, reviewHelper, deepSeekApi, flashcardRepo, reviewNoteRepo, apiPreferences, logId, questionBankRepo) as T
}

class CountdownListVMF(private val repo: CountdownRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CountdownListViewModel(repo) as T
}

class CountdownEditVMF(
    private val repo: CountdownRepository,
    private val countdownId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CountdownEditViewModel(repo, countdownId) as T
}

class HomeEditVMF(
    private val homeModuleRepo: HomeModuleRepository,
    private val countdownRepo: CountdownRepository,
    private val habitRepo: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeEditViewModel(homeModuleRepo, countdownRepo, habitRepo) as T
}

class TimelineListVMF(
    private val logRepo: LogRepository,
    private val pomodoroManager: PomodoroManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.learninglog.list.TimelineListViewModel(logRepo, pomodoroManager) as T
}

class DeepSeekVMF(private val repo: DeepSeekRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DeepSeekViewModel(repo) as T
}

class PomodoroVMF(private val pomodoroManager: PomodoroManager) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PomodoroViewModel(pomodoroManager) as T
}

class WeatherDetailVMF(
    private val weatherRepo: WeatherRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        WeatherDetailViewModel(weatherRepo) as T
}

class FlashcardVMF(
    private val flashcardRepo: FlashcardRepository,
    private val reviewNoteRepo: ReviewNoteRepository,
    private val kpStatsRepo: com.sunjk.sunjktool.domain.repository.KnowledgePointStatsRepository,
    private val logEntryId: Long,
    private val sessionId: Long?,
    private val autoAdvance: Boolean = false
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FlashcardViewModel(flashcardRepo, reviewNoteRepo, kpStatsRepo, logEntryId, sessionId, autoAdvance) as T
}

class FlashcardHubVMF(
    private val flashcardRepo: FlashcardRepository,
    private val deepSeekApi: DeepSeekApi,
    private val logRepo: com.sunjk.sunjktool.domain.repository.LogRepository,
    private val apiPreferences: ApiPreferences,
    private val logEntryId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FlashcardHubViewModel(flashcardRepo, deepSeekApi, logRepo, apiPreferences, logEntryId) as T
}

class ReviewNoteListVMF(
    private val repo: ReviewNoteRepository,
    private val logEntryId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReviewNoteListViewModel(repo, logEntryId) as T
}

class ReviewNoteEditVMF(
    private val repo: ReviewNoteRepository,
    private val logEntryId: Long,
    private val noteId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReviewNoteEditViewModel(repo, logEntryId, noteId) as T
}

class ReviewNoteDetailVMF(
    private val repo: ReviewNoteRepository,
    private val noteId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReviewNoteDetailViewModel(repo, noteId) as T
}

class NotebookListVMF(
    private val notebookRepo: NotebookRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.notebook.list.NotebookListViewModel(notebookRepo) as T
}

class TodoVMF(
    private val tickTickRepository: com.sunjk.sunjktool.domain.repository.TickTickRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.todo.TodoViewModel(tickTickRepository) as T
}

class NotebookDetailVMF(
    private val notebookRepo: NotebookRepository,
    private val logRepo: LogRepository,
    private val notebookId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.notebook.detail.NotebookDetailViewModel(notebookRepo, logRepo, notebookId) as T
}

class NotebookEditVMF(
    private val notebookRepo: NotebookRepository,
    private val notebookId: Long?,
    private val parentId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.notebook.edit.NotebookEditViewModel(notebookRepo, notebookId, parentId) as T
}

class LifeLogListVMF(
    private val repo: LifeLogRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.lifelog.list.LifeLogListViewModel(repo) as T
}

class LifeLogEditVMF(
    private val repo: LifeLogRepository,
    private val entryId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.lifelog.edit.LifeLogEditViewModel(repo, entryId) as T
}

class OverviewVMF(
    private val logRepo: LogRepository,
    private val pomodoroRecordDao: com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao,
    private val habitRepo: HabitRepository,
    private val reviewDao: ReviewStatusDao,
    private val lifeLogRepo: com.sunjk.sunjktool.domain.repository.LifeLogRepository,
    private val questionBankRepo: com.sunjk.sunjktool.domain.repository.QuestionBankRepository,
    private val tickTickRepository: com.sunjk.sunjktool.domain.repository.TickTickRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.overview.OverviewViewModel(logRepo, pomodoroRecordDao, habitRepo, reviewDao, lifeLogRepo, questionBankRepo, tickTickRepository) as T
}

class LearningStatsVMF(
    private val logRepo: LogRepository,
    private val pomodoroRecordDao: com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao,
    private val notebookRepo: NotebookRepository,
    private val questionBankRepo: com.sunjk.sunjktool.domain.repository.QuestionBankRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.stats.LearningStatsViewModel(logRepo, pomodoroRecordDao, notebookRepo, questionBankRepo) as T
}

class LifeLogDetailVMF(
    private val repo: LifeLogRepository,
    private val entryId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.lifelog.detail.LifeLogDetailViewModel(repo, entryId) as T
}

class QuestionBankListVMF(
    private val repo: QuestionBankRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.questionbank.list.QuestionBankListViewModel(repo) as T
}

class QuestionBankDetailVMF(
    private val repo: QuestionBankRepository,
    private val deepSeekApi: DeepSeekApi,
    private val logRepo: LogRepository,
    private val notebookRepo: com.sunjk.sunjktool.domain.repository.NotebookRepository,
    private val apiPreferences: ApiPreferences,
    private val categoryId: Long,
    private val initialQuestionId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.questionbank.detail.QuestionBankDetailViewModel(repo, deepSeekApi, logRepo, notebookRepo, apiPreferences, categoryId, initialQuestionId) as T
}

class QuestionLinkListVMF(
    private val repo: QuestionBankRepository,
    private val logId: Long,
    private val headingId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.questionbank.link.QuestionLinkListViewModel(repo, logId, headingId) as T
}

class QuestionBankEditVMF(
    private val repo: QuestionBankRepository,
    private val categoryId: Long?,
    private val parentId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.sunjk.sunjktool.feature.questionbank.edit.QuestionBankEditViewModel(repo, categoryId, parentId) as T
}
