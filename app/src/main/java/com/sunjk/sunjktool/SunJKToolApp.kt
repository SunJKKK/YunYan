package com.sunjk.sunjktool

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.Decoder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.sunjk.sunjktool.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SunJKToolApp : Application(), ImageLoaderFactory {

    var container: AppContainer? = null
        private set

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .allowHardware(false) // Software bitmaps avoid GPU buffer limits for large images
            .build()

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val c = AppContainer(this@SunJKToolApp)
            container = c
            c.homeModuleRepository.initializeIfNeeded()
        }
    }
}
