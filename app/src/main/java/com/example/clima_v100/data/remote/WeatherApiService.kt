package com.example.clima_v100.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for WeatherAPI.
 * Handles all HTTP requests to the WeatherAPI endpoint.
 */
interface WeatherApiService {

    /**
     * Fetches historical weather data for a specific date and location.
     * @param apiKey API key for WeatherAPI authentication
     * @param query City name or coordinates for the location
     * @param date Date in YYYY-MM-DD format
     * @return WeatherApiResponse containing weather data
     */
    @GET("v1/history.json")
    suspend fun getHistoricalWeather(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("dt") date: String
    ): WeatherApiResponse

    /**
     * Fetches current weather data for a specific location.
     * @param apiKey API key for WeatherAPI authentication
     * @param query Latitude and Longitude (e.g., "40.7128,-74.0060") or city name
     * @param aqi Air quality data flag (no = exclude AQI data)
     * @return CurrentWeatherResponse containing current weather data
     */
    @GET("v1/current.json")
    suspend fun getCurrentWeather(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("aqi") aqi: String = "no"
    ): CurrentWeatherResponse
}

/**
 * Data class representing the response from WeatherAPI.
 * This is a simplified structure; extend as needed based on the actual API response.
 */
data class WeatherApiResponse(
    val location: LocationData,
    val forecast: ForecastData
)

data class LocationData(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double
)

data class ForecastData(
    val forecastday: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: DayData
)

data class DayData(
    val maxtemp_f: Float,
    val mintemp_f: Float,
    val maxtemp_c: Float,
    val mintemp_c: Float
)

/**
 * Data class representing the response from WeatherAPI current endpoint.
 * Contains current weather conditions and location data.
 */
data class CurrentWeatherResponse(
    val location: CurrentLocationData,
    val current: CurrentWeatherData
)

data class CurrentLocationData(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val localtime: String  // Format: "2026-04-27 14:30" or "YYYY-MM-DD HH:mm"
)

data class CurrentWeatherData(
    val temp_f: Float,      // Temperature in Fahrenheit
    val temp_c: Float,      // Temperature in Celsius
    val condition: CurrentCondition
)

data class CurrentCondition(
    val text: String,       // e.g., "Partly Cloudy"
    val icon: String        // Icon URL
)
