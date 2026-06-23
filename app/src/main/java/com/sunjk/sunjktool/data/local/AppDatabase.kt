package com.sunjk.sunjktool.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sunjk.sunjktool.data.local.dao.CountdownDao
import com.sunjk.sunjktool.data.local.dao.LogEntryDao
import com.sunjk.sunjktool.data.model.CountdownEntity
import com.sunjk.sunjktool.data.model.LogEntryEntity

@Database(
    entities = [LogEntryEntity::class, CountdownEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logEntryDao(): LogEntryDao
    abstract fun countdownDao(): CountdownDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sunjk_toolbox.db"
                ).fallbackToDestructiveMigration(false).build().also { INSTANCE = it }
            }
    }
}
