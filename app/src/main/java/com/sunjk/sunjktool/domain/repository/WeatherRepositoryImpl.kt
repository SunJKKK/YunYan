package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.remote.QWeatherApi
import com.sunjk.sunjktool.domain.model.DayForecast
import com.sunjk.sunjktool.domain.model.WeatherBundle
import com.sunjk.sunjktool.domain.model.WeatherResult
import com.sunjk.sunjktool.domain.model.WeatherWarning
import com.sunjk.sunjktool.util.LocationHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherRepositoryImpl(
    private val api: QWeatherApi,
    private val locationHelper: LocationHelper
) : WeatherRepository {

    private val _weatherResult = MutableStateFlow<WeatherResult>(WeatherResult.Idle)
    override val weatherResult: StateFlow<WeatherResult> = _weatherResult.asStateFlow()

    override suspend fun refresh() {
        _weatherResult.value = WeatherResult.Loading
        try {
            val (lat, lon) = locationHelper.getCurrentLocation()
            val bundle = fetchWeather(lon, lat)
            _weatherResult.value = WeatherResult.Success(bundle)
        } catch (e: Exception) {
            _weatherResult.value = WeatherResult.Error(
                e.message ?: "获取天气失败"
            )
        }
    }

    private suspend fun fetchWeather(lon: Double, lat: Double): WeatherBundle = coroutineScope {
        val nowDeferred = async { api.getNowWeather(lon, lat) }
        val forecastDeferred = async { api.get7dForecast(lon, lat) }
        val warningDeferred = async { api.getWarnings(lon, lat) }
        val indexDeferred = async { api.getLifeIndices(lon, lat, type = 1) }
        val cityDeferred = async { api.lookupCity(lon, lat) }

        val nowResponse = nowDeferred.await()
        val forecastResponse = forecastDeferred.await()
        val warningResponse = warningDeferred.await()
        val indexResponse = indexDeferred.await()
        val cityResponse = cityDeferred.await()

        val now = nowResponse.now
        val daily = forecastResponse.daily.orEmpty()
        val tomorrow = daily.getOrNull(1)
        val todayIndex = indexResponse.daily?.firstOrNull()
        val cityName = cityResponse.location?.firstOrNull()?.name
            ?: cityResponse.location?.firstOrNull()?.adm2
            ?: "未知城市"

        val dailyForecast = daily.map { dto ->
            DayForecast(
                date = dto.fxDate.orEmpty(),
                tempMax = dto.tempMax.orEmpty(),
                tempMin = dto.tempMin.orEmpty(),
                iconDay = dto.iconDay.orEmpty(),
                textDay = dto.textDay.orEmpty(),
                iconNight = dto.iconNight.orEmpty(),
                textNight = dto.textNight.orEmpty(),
                windDirDay = dto.windDirDay.orEmpty(),
                windScaleDay = dto.windScaleDay.orEmpty()
            )
        }

        val warnings = warningResponse.warning.orEmpty().map { w ->
            WeatherWarning(
                title = w.title.orEmpty(),
                level = w.level.orEmpty(),
                text = w.text.orEmpty(),
                pubTime = w.pubTime.orEmpty()
            )
        }

        WeatherBundle(
            cityName = cityName,
            currentTemp = now?.temp.orEmpty(),
            currentText = now?.text.orEmpty(),
            currentIcon = now?.icon.orEmpty(),
            feelsLike = now?.feelsLike.orEmpty(),
            windDir = now?.windDir.orEmpty(),
            windScale = now?.windScale.orEmpty(),
            humidity = now?.humidity.orEmpty(),
            pressure = now?.pressure.orEmpty(),
            vis = now?.vis.orEmpty(),
            obsTime = now?.obsTime.orEmpty(),
            tomorrowDate = tomorrow?.fxDate.orEmpty(),
            tomorrowTempMax = tomorrow?.tempMax.orEmpty(),
            tomorrowTempMin = tomorrow?.tempMin.orEmpty(),
            tomorrowIconDay = tomorrow?.iconDay.orEmpty(),
            tomorrowTextDay = tomorrow?.textDay.orEmpty(),
            dailyForecast = dailyForecast,
            clothingName = todayIndex?.name.orEmpty(),
            clothingText = todayIndex?.text.orEmpty(),
            clothingCategory = todayIndex?.category.orEmpty(),
            warnings = warnings
        )
    }
}
