package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.domain.model.WeatherResult
import kotlinx.coroutines.flow.StateFlow

interface WeatherRepository {
    val weatherResult: StateFlow<WeatherResult>
    suspend fun refresh()
}
