package com.sunjk.sunjktool.data.remote

import com.sunjk.sunjktool.data.local.ApiPreferences
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class QWeatherApi(
    private val client: HttpClient,
    private val apiPreferences: ApiPreferences
) {

    companion object {
        private const val BASE_URL = "https://devapi.qweather.com/v7/"
    }

    private val apiKey: String get() = apiPreferences.getQWeatherKey()

    suspend fun getNowWeather(lon: Double, lat: Double): QWeatherNowResponse =
        client.get("${BASE_URL}weather/now") {
            parameter("location", "$lon,$lat")
            parameter("key", apiKey)
        }.body()

    suspend fun get7dForecast(lon: Double, lat: Double): QWeather7dResponse =
        client.get("${BASE_URL}weather/7d") {
            parameter("location", "$lon,$lat")
            parameter("key", apiKey)
        }.body()

    suspend fun getWarnings(lon: Double, lat: Double): QWeatherWarningResponse =
        client.get("${BASE_URL}warning/now") {
            parameter("location", "$lon,$lat")
            parameter("key", apiKey)
        }.body()

    suspend fun getLifeIndices(lon: Double, lat: Double, type: Int): QWeatherIndicesResponse =
        client.get("${BASE_URL}indices/1d") {
            parameter("location", "$lon,$lat")
            parameter("key", apiKey)
            parameter("type", type)
        }.body()

    suspend fun lookupCity(lon: Double, lat: Double): QWeatherCityResponse =
        client.get("https://geoapi.qweather.com/v2/city/lookup") {
            parameter("location", "$lon,$lat")
            parameter("key", apiKey)
        }.body()
}
