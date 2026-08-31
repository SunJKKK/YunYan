package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable

@Stable
data class WeatherBundle(
    val cityName: String,
    // current weather
    val currentTemp: String,
    val currentText: String,
    val currentIcon: String,
    val feelsLike: String,
    val windDir: String,
    val windScale: String,
    val humidity: String,
    val pressure: String,
    val vis: String,
    val obsTime: String,
    // tomorrow
    val tomorrowDate: String,
    val tomorrowTempMax: String,
    val tomorrowTempMin: String,
    val tomorrowIconDay: String,
    val tomorrowTextDay: String,
    // 7-day forecast
    val dailyForecast: List<DayForecast>,
    // clothing index
    val clothingName: String,
    val clothingText: String,
    val clothingCategory: String,
    // warnings
    val warnings: List<WeatherWarning>
)

@Stable
data class WeatherWarning(
    val title: String,
    val level: String,
    val text: String,
    val pubTime: String
)

@Stable
data class DayForecast(
    val date: String,
    val tempMax: String,
    val tempMin: String,
    val iconDay: String,
    val textDay: String,
    val iconNight: String,
    val textNight: String,
    val windDirDay: String,
    val windScaleDay: String
)

/** Sealed class for weather loading state on home page. */
sealed class WeatherResult {
    data object Idle : WeatherResult()
    data object Loading : WeatherResult()
    data class Success(val data: WeatherBundle) : WeatherResult()
    data class Error(val message: String) : WeatherResult()
}
