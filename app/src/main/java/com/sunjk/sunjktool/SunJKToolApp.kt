package com.sunjk.sunjktool

import android.app.Application
import com.sunjk.sunjktool.di.AppContainer

class SunJKToolApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
