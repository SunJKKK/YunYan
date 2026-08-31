package com.sunjk.sunjktool.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationUnavailableException(message: String) : Exception(message)

class LocationHelper(private val context: Context) {

    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    return@withContext Pair(lat, lon)
                }
            } catch (_: Exception) {
                // try next provider
            }
        }
        throw LocationUnavailableException("无法获取当前位置，请确保已开启定位服务")
    }
}
