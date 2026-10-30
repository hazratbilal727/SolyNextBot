package com.example.data.tools

import com.example.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WeatherTool(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun getWeather(locationQuery: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        val city = locationQuery.trim().ifEmpty { "London" }
        try {
            // 1. Geocode city name to lat/lon
            val encodedCity = URLEncoder.encode(city, "UTF-8")
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json"
            val geoRequest = Request.Builder().url(geoUrl).build()

            var lat = 51.5072
            var lon = -0.1276
            var resolvedName = city
            var country = ""

            client.newCall(geoRequest).execute().use { geoResponse ->
                if (geoResponse.isSuccessful) {
                    val geoBody = geoResponse.body?.string().orEmpty()
                    val geoJson = JSONObject(geoBody)
                    val results = geoJson.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val first = results.getJSONObject(0)
                        lat = first.optDouble("latitude", lat)
                        lon = first.optDouble("longitude", lon)
                        resolvedName = first.optString("name", city)
                        country = first.optString("country", "")
                    }
                }
            }

            // 2. Fetch current weather from Open-Meteo
            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
            val weatherRequest = Request.Builder().url(weatherUrl).build()

            client.newCall(weatherRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ToolExecutionResult(
                        toolName = "Weather",
                        summary = "Unable to retrieve meteorological telemetry for $city.",
                        isSuccess = false
                    )
                }

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val humidity = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m")
                val weatherCode = current.getInt("weather_code")

                val condition = mapWeatherCode(weatherCode)
                val locationDisplay = if (country.isNotBlank()) "$resolvedName, $country" else resolvedName
                val summary = "Current weather in $locationDisplay: $condition, ${temp}°C (${(temp * 9 / 5 + 32).toInt()}°F), Humidity $humidity%, Wind $wind km/h."

                ToolExecutionResult(
                    toolName = "Weather",
                    summary = summary,
                    detailedData = "Location: $locationDisplay\nCoordinates: $lat, $lon\nCondition: $condition\nTemperature: ${temp}°C\nRelative Humidity: $humidity%\nWind Speed: $wind km/h",
                    sourceUrl = "https://open-meteo.com",
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "Weather",
                summary = "Weather telemetry failed for '$city': ${e.localizedMessage ?: "Network error"}",
                isSuccess = false
            )
        }
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear to partly cloudy"
            45, 48 -> "Fog and depositing rime fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain shower"
            71, 73, 75 -> "Snow fall"
            80, 81, 82 -> "Violent rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Atmospheric variation"
        }
    }
}
