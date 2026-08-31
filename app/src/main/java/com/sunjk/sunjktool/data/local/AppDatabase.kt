package com.sunjk.sunjktool.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunjk.sunjktool.data.local.dao.BalanceRecordDao
import com.sunjk.sunjktool.data.local.dao.CountdownDao
import com.sunjk.sunjktool.data.local.dao.GreetingQuoteDao
import com.sunjk.sunjktool.data.local.dao.HomeModuleDao
import com.sunjk.sunjktool.data.local.dao.FlashcardSessionDao
import com.sunjk.sunjktool.data.local.dao.PomodoroRecordDao
import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.local.dao.HabitDao
import com.sunjk.sunjktool.data.local.dao.HabitRecordDao
import com.sunjk.sunjktool.data.local.dao.KnowledgePointStatsDao
import com.sunjk.sunjktool.data.local.dao.LifeLogEntryDao
import com.sunjk.sunjktool.data.local.dao.NotebookDao
import com.sunjk.sunjktool.data.local.dao.QuestionBankCategoryDao
import com.sunjk.sunjktool.data.local.dao.QuestionDao
import com.sunjk.sunjktool.data.local.dao.ReviewNoteDao
import com.sunjk.sunjktool.data.local.dao.TickTickProjectDao
import com.sunjk.sunjktool.data.local.dao.TickTickTaskDao
import com.sunjk.sunjktool.data.model.BalanceRecordEntity
import com.sunjk.sunjktool.data.model.PomodoroRecordEntity
import com.sunjk.sunjktool.data.model.CountdownEntity
import com.sunjk.sunjktool.data.model.FlashcardSessionEntity
import com.sunjk.sunjktool.data.model.HomeModuleEntity
import com.sunjk.sunjktool.data.model.GreetingQuoteEntity
import com.sunjk.sunjktool.data.model.ReviewStatusEntity
import com.sunjk.sunjktool.data.model.LogEntryEntity
import com.sunjk.sunjktool.data.model.HabitEntity
import com.sunjk.sunjktool.data.model.HabitRecordEntity
import com.sunjk.sunjktool.data.model.KnowledgePointStatsEntity
import com.sunjk.sunjktool.data.model.LifeLogEntryEntity
import com.sunjk.sunjktool.data.model.NotebookEntity
import com.sunjk.sunjktool.data.model.QuestionBankCategoryEntity
import com.sunjk.sunjktool.data.model.QuestionEntity
import com.sunjk.sunjktool.data.model.ReviewNoteEntity
import com.sunjk.sunjktool.data.model.TickTickProjectEntity
import com.sunjk.sunjktool.data.model.TickTickTaskEntity

@Database(
    entities = [LogEntryEntity::class, CountdownEntity::class, HomeModuleEntity::class, BalanceRecordEntity::class, GreetingQuoteEntity::class, ReviewStatusEntity::class, FlashcardSessionEntity::class, PomodoroRecordEntity::class, HabitEntity::class, HabitRecordEntity::class, ReviewNoteEntity::class, NotebookEntity::class, LifeLogEntryEntity::class, KnowledgePointStatsEntity::class, QuestionBankCategoryEntity::class, QuestionEntity::class, TickTickProjectEntity::class, TickTickTaskEntity::class],
    version = 32,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logEntryDao(): LogEntryDao
    abstract fun countdownDao(): CountdownDao
    abstract fun homeModuleDao(): HomeModuleDao
    abstract fun balanceRecordDao(): BalanceRecordDao
    abstract fun greetingQuoteDao(): GreetingQuoteDao
    abstract fun reviewStatusDao(): ReviewStatusDao
    abstract fun flashcardSessionDao(): FlashcardSessionDao
    abstract fun pomodoroRecordDao(): PomodoroRecordDao
    abstract fun habitDao(): HabitDao
    abstract fun habitRecordDao(): HabitRecordDao
    abstract fun reviewNoteDao(): ReviewNoteDao
    abstract fun notebookDao(): NotebookDao
    abstract fun lifeLogEntryDao(): LifeLogEntryDao
    abstract fun knowledgePointStatsDao(): KnowledgePointStatsDao
    abstract fun questionBankCategoryDao(): QuestionBankCategoryDao
    abstract fun questionDao(): QuestionDao
    abstract fun tickTickProjectDao(): TickTickProjectDao
    abstract fun tickTickTaskDao(): TickTickTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO home_modules (moduleKey, enabled, sortOrder) VALUES ('weather', 0, 3)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO home_modules (moduleKey, enabled, sortOrder) VALUES ('pomodoro', 0, 4)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS balance_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, totalBalance REAL NOT NULL, grantedBalance REAL NOT NULL, toppedUpBalance REAL NOT NULL, timestamp INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO home_modules (moduleKey, enabled, sortOrder) VALUES ('deepseek', 0, 5)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS greeting_quotes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO home_modules (moduleKey, enabled, sortOrder) SELECT 'countdown_' || selectedCountdownId, enabled, sortOrder FROM home_modules WHERE moduleKey = 'countdown' AND selectedCountdownId IS NOT NULL")
                db.execSQL("DELETE FROM home_modules WHERE moduleKey = 'countdown'")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS review_status (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, logEntryId INTEGER NOT NULL, reviewDate INTEGER NOT NULL, reviewType TEXT NOT NULL, isCompleted INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_status_logEntryId ON review_status (logEntryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_status_reviewDate ON review_status (reviewDate)")
                db.execSQL("INSERT OR IGNORE INTO home_modules (moduleKey, enabled, sortOrder) VALUES ('review', 1, 1)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE log_entries ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE log_entries ADD COLUMN aiSummary TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcard_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, logEntryId INTEGER NOT NULL, cardsJson TEXT NOT NULL, createdDate INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcard_sessions ADD COLUMN answersJson TEXT NOT NULL DEFAULT '{}'")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcard_sessions ADD COLUMN style TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS pomodoro_records (date TEXT PRIMARY KEY NOT NULL, focusSecs INTEGER NOT NULL, completedCount INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_16_18 = object : Migration(16, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', colorArgb INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_records (date TEXT PRIMARY KEY NOT NULL, habitId INTEGER NOT NULL, isCompleted INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop and recreate habit tables if they exist with mismatched schema
                db.execSQL("DROP TABLE IF EXISTS habit_records")
                db.execSQL("DROP TABLE IF EXISTS habits")
                db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', colorArgb INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS habit_records (date TEXT PRIMARY KEY NOT NULL, habitId INTEGER NOT NULL, isCompleted INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS review_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, logEntryId INTEGER NOT NULL, content TEXT NOT NULL, imagePaths TEXT, sourceType TEXT NOT NULL DEFAULT 'manual', flashcardSessionId INTEGER, createdDate INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS notebooks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, parentId INTEGER, sortOrder INTEGER NOT NULL DEFAULT 0, createdDate INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE log_entries ADD COLUMN notebookId INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS life_log_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, content TEXT NOT NULL DEFAULT '', mood TEXT NOT NULL DEFAULT '', imagePath TEXT, createdDate INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO home_modules (moduleKey, enabled, sortOrder) VALUES ('overview', 0, 6)")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS knowledge_point_stats (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, logEntryId INTEGER NOT NULL, knowledgePoint TEXT NOT NULL, totalQuestions INTEGER NOT NULL DEFAULT 0, correctAnswers INTEGER NOT NULL DEFAULT 0, updatedDate INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE knowledge_point_stats ADD COLUMN weaknessSummary TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE log_entries ADD COLUMN selfCheckContent TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE log_entries ADD COLUMN mindMapJson TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS question_bank_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, parentId INTEGER, sortOrder INTEGER NOT NULL DEFAULT 0, createdDate INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS questions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryId INTEGER NOT NULL, content TEXT NOT NULL, imagePaths TEXT NOT NULL DEFAULT '', aiAnalysis TEXT NOT NULL DEFAULT '', sortOrder INTEGER NOT NULL DEFAULT 0, createdDate INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE log_entries ADD COLUMN attachmentPaths TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE log_entries ADD COLUMN attachmentText TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE home_modules ADD COLUMN size TEXT NOT NULL DEFAULT 'small'")
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notebooks ADD COLUMN icon TEXT NOT NULL DEFAULT 'folder'")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("INSERT OR IGNORE INTO home_modules (moduleKey, enabled, sortOrder) VALUES ('notebook_shortcuts', 0, 7)")
            }
        }

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS ticktick_projects (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, sortOrder INTEGER NOT NULL DEFAULT 0, color TEXT NOT NULL DEFAULT '')")
                db.execSQL("CREATE TABLE IF NOT EXISTS ticktick_tasks (id TEXT NOT NULL PRIMARY KEY, projectId TEXT NOT NULL DEFAULT '', title TEXT NOT NULL DEFAULT '', isCompleted INTEGER NOT NULL DEFAULT 0, dueDate TEXT, priority INTEGER NOT NULL DEFAULT 0, sortOrder INTEGER NOT NULL DEFAULT 0, content TEXT NOT NULL DEFAULT '')")
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop and recreate to fix schema hash mismatch from 26→27
                db.execSQL("DROP TABLE IF EXISTS questions")
                db.execSQL("DROP TABLE IF EXISTS question_bank_categories")
                db.execSQL("CREATE TABLE IF NOT EXISTS question_bank_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, parentId INTEGER, sortOrder INTEGER NOT NULL DEFAULT 0, createdDate INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS questions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, categoryId INTEGER NOT NULL, content TEXT NOT NULL, imagePaths TEXT NOT NULL DEFAULT '', aiAnalysis TEXT NOT NULL DEFAULT '', sortOrder INTEGER NOT NULL DEFAULT 0, createdDate INTEGER NOT NULL, updatedDate INTEGER NOT NULL)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "sunjk_toolbox.db")
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_18, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32)
                    .fallbackToDestructiveMigration(false)
                    .build().also { INSTANCE = it }
            }
    }
}
