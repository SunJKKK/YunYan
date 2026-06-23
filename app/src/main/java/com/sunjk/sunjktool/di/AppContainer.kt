package com.sunjk.sunjktool.di

import android.content.Context
import com.sunjk.sunjktool.data.local.AppDatabase
import com.sunjk.sunjktool.domain.repository.CountdownRepository
import com.sunjk.sunjktool.domain.repository.CountdownRepositoryImpl
import com.sunjk.sunjktool.domain.repository.LogRepository
import com.sunjk.sunjktool.domain.repository.LogRepositoryImpl

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val logRepository: LogRepository = LogRepositoryImpl(database.logEntryDao())
    val countdownRepository: CountdownRepository = CountdownRepositoryImpl(database.countdownDao())
}
