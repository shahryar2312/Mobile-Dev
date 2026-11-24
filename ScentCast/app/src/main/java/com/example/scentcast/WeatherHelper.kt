package com.example.scentcast

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Weather at the phone's position, from Open-Meteo. */
data class WeatherReading(
    val tempC: Double,
    val humidity: Int,
    val latitude: Double,
    val longitude: Double
) {
    /** Cold or damp-and-cool gives COLD, warm gives HOT, anything else is MILD. */
    fun toWeather(): Weather = when {
        tempC < 12.0 || (humidity >= 85 && tempC < 16.0) -> Weather.COLD
        tempC >= 20.0 -> Weather.HOT
        else -> Weather.MILD
    }
}

/**
 * Sensor 1, the GPS. Finds the phone's position, then asks Open-Meteo for the temperature
 * and humidity there. The network call runs on a background thread, and the result is
 * handed back on the main thread through the callbacks.
 */
class WeatherHelper(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun hasPermission(): Boolean =
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** onError gets a string resource id to show to the user. */
    fun load(onSuccess: (WeatherReading) -> Unit, onError: (Int) -> Unit) {
        if (!hasPermission()) {
            onError(R.string.err_no_permission)
            return
        }
        findLocation { location ->
            if (location == null) {
                onError(R.string.err_no_location)
            } else {
                fetchWeather(location, onSuccess, onError)
            }
        }
    }

    @SuppressLint("MissingPermission") // hasPermission() is checked in load()
    @Suppress("DEPRECATION")
    private fun findLocation(onFound: (Location?) -> Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Network location works with approximate permission, GPS needs precise permission.
        val providers = mutableListOf(LocationManager.NETWORK_PROVIDER)
        if (isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            providers.add(LocationManager.GPS_PROVIDER)
        }
        val enabled = providers.filter { manager.isProviderEnabled(it) }
        if (enabled.isEmpty()) {
            onFound(null)
            return
        }

        // A remembered position is good enough for weather. Take the newest one, since
        // each provider keeps its own and one can be hours old.
        val remembered = enabled.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (remembered != null) {
            onFound(remembered)
            return
        }

        // Otherwise wait for one fresh position, but give up after 10 seconds.
        var finished = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!finished) {
                    finished = true
                    onFound(location)
                }
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        manager.requestSingleUpdate(enabled.first(), listener, Looper.getMainLooper())
        mainHandler.postDelayed({
            if (!finished) {
                finished = true
                manager.removeUpdates(listener)
                onFound(null)
            }
        }, 10_000)
    }

    private fun fetchWeather(
        location: Location,
        onSuccess: (WeatherReading) -> Unit,
        onError: (Int) -> Unit
    ) {
        Thread {
            try {
                // Rounded to 2 decimals (about 1 km): enough for weather, and shares less.
                val lat = String.format(Locale.US, "%.2f", location.latitude)
                val lon = String.format(Locale.US, "%.2f", location.longitude)
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,relative_humidity_2m"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val current = JSONObject(body).getJSONObject("current")
                val reading = WeatherReading(
                    tempC = current.getDouble("temperature_2m"),
                    humidity = current.getInt("relative_humidity_2m"),
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                mainHandler.post { onSuccess(reading) }
            } catch (e: Exception) { // no internet, HTTP error or unexpected JSON
                mainHandler.post { onError(R.string.err_network) }
            }
        }.start()
    }
}
