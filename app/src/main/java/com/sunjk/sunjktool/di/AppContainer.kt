package com.sunjk.sunjktool.di

import android.content.Context
import com.sunjk.sunjktool.data.local.ApiPreferences
import com.sunjk.sunjktool.data.local.AppDatabase
import com.sunjk.sunjktool.data.remote.DeepSeekApi
import com.sunjk.sunjktool.data.remote.QWeatherApi
import com.sunjk.sunjktool.data.remote.TickTickApi
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.CountdownRepositoryImpl
import com.sunjk.sunjktool.domain.repository.HomeModuleRepository
import com.sunjk.sunjktool.domain.repository.HomeModuleRepositoryImpl
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.LogRepositoryImpl
import com.sunjk.sunjktool.domain.repository.DeepSeekRepository
import com.sunjk.sunjktool.domain.repository.DeepSeekRepositoryImpl
import com.sunjk.sunjktool.domain.repository.FlashcardRepository
import com.sunjk.sunjktool.domain.repository.FlashcardRepositoryImpl
import com.sunjk.sunjktool.domain.repository.HabitRepository
import com.sunjk.sunjktool.domain.repository.HabitRepositoryImpl
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepository
import com.sunjk.sunjktool.domain.repository.ReviewNoteRepositoryImpl
import com.sunjk.sunjktool.domain.repository.NotebookRepository
import com.sunjk.sunjktool.domain.repository.NotebookRepositoryImpl
import com.sunjk.sunjktool.domain.repository.QuestionBankRepository
import com.sunjk.sunjktool.domain.repository.QuestionBankRepositoryImpl
import com.sunjk.sunjktool.domain.repository.LifeLogRepository
import com.sunjk.sunjktool.domain.repository.LifeLogRepositoryImpl
import com.sunjk.sunjktool.domain.repository.KnowledgePointStatsRepository
import com.sunjk.sunjktool.domain.repository.WeatherRepository
import com.sunjk.sunjktool.domain.repository.WeatherRepositoryImpl
import com.sunjk.sunjktool.domain.repository.TickTickRepository
import com.sunjk.sunjktool.domain.repository.TickTickRepositoryImpl
import com.sunjk.sunjktool.data.sync.SyncEngine
import com.sunjk.sunjktool.data.sync.SyncPreferencesManager
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.util.LocationHelper
import com.sunjk.sunjktool.util.NotificationHelper
import com.sunjk.sunjktool.util.PomodoroManager
import com.sunjk.sunjktool.util.ReviewHelper
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            socketTimeout = 300_000
        }
    }

    // OkHttp client for WebDAV (supports non-standard HTTP methods: PROPFIND, MKCOL)
    private val webDavHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiPreferences = ApiPreferences(context)
    private val qWeatherApi = QWeatherApi(httpClient, apiPreferences)
    val deepSeekApi = DeepSeekApi(httpClient, apiPreferences)
    private val tickTickApi = TickTickApi(httpClient, apiPreferences)

    val locationHelper = LocationHelper(context)
    val logRepository: LogRepository = LogRepositoryImpl(database.logEntryDao())
    val countdownRepository: CountdownRepository = CountdownRepositoryImpl(database.countdownDao())
    val homeModuleRepository: HomeModuleRepository = HomeModuleRepositoryImpl(database.homeModuleDao())
    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(qWeatherApi, locationHelper)
    val pomodoroManager = PomodoroManager(context)
    val deepSeekRepository: DeepSeekRepository = DeepSeekRepositoryImpl(deepSeekApi, database.balanceRecordDao())
    val flashcardRepository: FlashcardRepository = FlashcardRepositoryImpl(database.flashcardSessionDao())
    val habitRepository: HabitRepository = HabitRepositoryImpl(database.habitDao(), database.habitRecordDao())
    val reviewNoteRepository: ReviewNoteRepository = ReviewNoteRepositoryImpl(database.reviewNoteDao())
    val notebookRepository: NotebookRepository = NotebookRepositoryImpl(database.notebookDao(), database.logEntryDao())
    val lifeLogRepository: LifeLogRepository = LifeLogRepositoryImpl(database.lifeLogEntryDao())
    val pomodoroRecordDao = database.pomodoroRecordDao()
    val knowledgePointStatsRepository = KnowledgePointStatsRepository(database.knowledgePointStatsDao())
    val questionBankRepository: QuestionBankRepository = QuestionBankRepositoryImpl(database.questionBankCategoryDao(), database.questionDao())
    val tickTickRepository: TickTickRepository = TickTickRepositoryImpl(tickTickApi, database.tickTickProjectDao(), database.tickTickTaskDao(), apiPreferences)
    val reviewHelper = ReviewHelper(database.reviewStatusDao())
    val reviewStatusDao = database.reviewStatusDao()
    val logEntryDao = database.logEntryDao()

    // Sync
    val syncPreferencesManager = SyncPreferencesManager(context)
    val syncEngine = SyncEngine(
        database.logEntryDao(),
        database.countdownDao(),
        database.homeModuleDao(),
        database.reviewStatusDao(),
        database.greetingQuoteDao(),
        database.balanceRecordDao(),
        database.flashcardSessionDao(),
        database.pomodoroRecordDao(),
        database.habitDao(),
        database.habitRecordDao(),
        database.reviewNoteDao(),
        database.notebookDao(),
        database.questionBankCategoryDao(),
        database.questionDao(),
        database.lifeLogEntryDao(),
        database.knowledgePointStatsDao(),
        context,
        syncPreferencesManager,
        apiPreferences,
        webDavHttpClient
    )


    init {
        NotificationHelper.createChannels(context)
        SyncTrigger.init(syncEngine)
        pomodoroManager.onSyncRequested = { SyncTrigger.requestAutoSync() }
        pomodoroManager.recordDao = database.pomodoroRecordDao()
    }
}
