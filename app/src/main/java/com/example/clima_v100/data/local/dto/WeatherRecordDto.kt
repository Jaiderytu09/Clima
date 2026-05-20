package com.example.clima_v100.data.local.dto

/**
 * Data Transfer Object for location information.
 * Contains city, region, country and coordinates.
 * Used for reverse geocoding results and location context.
 */
data class LocationInfo(
    val city: String,
    val region: String,         // State/Province
    val country: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Data Transfer Object for weather records.
 * Used for API responses and business logic operations.
 * Does not include auto-generated ID to maintain separation between
 * data layer and business logic layer.
 */
data class WeatherRecordDto(
    val date: String,           // YYYY-MM-DD format (UTC)
    val city: String,
    val region: String,         // State/Province
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val maxTempFahrenheit: Float,
    val minTempFahrenheit: Float,
    val maxTempCelsius: Float,
    val minTempCelsius: Float
)

/**
 * Extended DTO for weather records with database ID.
 * Used when returning data from the repository after it has been persisted.
 */
data class WeatherRecordWithIdDto(
    val id: Int,
    val date: String,
    val city: String,
    val region: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val maxTempFahrenheit: Float,
    val minTempFahrenheit: Float,
    val maxTempCelsius: Float,
    val minTempCelsius: Float
)

