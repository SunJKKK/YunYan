package com.sunjk.sunjktool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sunjk.sunjktool.ui.components.SunJKToolScaffold
import com.sunjk.sunjktool.ui.theme.SunJKToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SunJKToolApp
        setContent {
            SunJKToolTheme {
                SunJKToolScaffold(
                    logRepository = app.container.logRepository,
                    countdownRepository = app.container.countdownRepository
                )
            }
        }
    }
}
