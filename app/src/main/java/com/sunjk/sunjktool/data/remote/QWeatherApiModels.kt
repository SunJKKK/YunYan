package com.sunjk.sunjktool.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://devapi.qweather.com/v7/weather/now
@Serializable
data class QWeatherNowResponse(
    val code: String,
    val updateTime: String? = null,
    val now: NowDto? = null
)

@Serializable
data class NowDto(
    val temp: String? = null,
    val feelsLike: String? = null,
    val icon: String? = null,
    val text: String? = null,
    val windDir: String? = null,
    val windScale: String? = null,
    val windSpeed: String? = null,
    val humidity: String? = null,
    val precip: String? = null,
    val pressure: String? = null,
    val vis: String? = null,
    val obsTime: String? = null
)

// https://devapi.qweather.com/v7/weather/7d
@Serializable
data class QWeather7dResponse(
    val code: String,
    val daily: List<DailyForecastDto>? = null
)

@Serializable
data class DailyForecastDto(
    val fxDate: String? = null,
    val tempMax: String? = null,
    val tempMin: String? = null,
    val iconDay: String? = null,
    val textDay: String? = null,
    val iconNight: String? = null,
    val textNight: String? = null,
    val windDirDay: String? = null,
    val windScaleDay: String? = null,
    val humidity: String? = null,
    val precip: String? = null,
    val uvIndex: String? = null
)

// https://devapi.qweather.com/v7/warning/now
@Serializable
data class QWeatherWarningResponse(
    val code: String,
    val warning: List<WarningDto>? = null
)

@Serializable
data class WarningDto(
    val id: String? = null,
    val sender: String? = null,
    val pubTime: String? = null,
    val title: String? = null,
    val status: String? = null,
    val severity: String? = null,
    val severityColor: String? = null,
    val type: String? = null,
    val typeName: String? = null,
    val level: String? = null,
    val text: String? = null
)

// https://devapi.qweather.com/v7/indices/1d
@Serializable
data class QWeatherIndicesResponse(
    val code: String,
    val daily: List<LifeIndexDto>? = null
)

@Serializable
data class LifeIndexDto(
    val date: String? = null,
    val type: String? = null,
    val name: String? = null,
    val level: String? = null,
    val category: String? = null,
    val text: String? = null
)

// https://geoapi.qweather.com/v2/city/lookup
@Serializable
data class QWeatherCityResponse(
    val code: String,
    val location: List<CityDto>? = null
)

@Serializable
data class CityDto(
    val id: String? = null,
    val name: String? = null,
    val adm1: String? = null,
    val adm2: String? = null,
    val country: String? = null
)
