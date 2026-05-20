package com.example.clima_v100.data.repository

import com.example.clima_v100.data.local.dto.WeatherRecordDto
import com.example.clima_v100.data.local.entity.DiaryRecord

/**
 * Repository interface for DiaryRecord operations.
 * Defines contract for weather record management, including database operations and API interactions.
 */
interface IRepositoryDiaryRecord {

    /**
     * Retrieves historical weather data from the local database.
     * This is a pure database query operation with NO API fallback.
     *
     * @param date Date in YYYY-MM-DD format
     * @param city City name
     * @param region State/Province
     * @param country Country name
     * @return WeatherRecordDto if found in database, null otherwise
     * @throws Exception if database operation fails
     */
    suspend fun obtainHistoricalWeatherFromDatabase(
        date: String,
        city: String,
        region: String,
        country: String
    ): WeatherRecordDto?

    /**
     * Fetches historical weather data from WeatherAPI for a specific date and city.
     * This is a pure API operation that automatically saves the result to the database.
     * Este caso de uso NO SE PUEDE USAR PARA FECHAS FUTURAS.
     *
     * @param date Date in YYYY-MM-DD format
     * @param city City name
     * @param region State/Province
     * @param country Country name
     * @return WeatherRecordDto containing the fetched weather data
     * @throws IllegalArgumentException if date is a future date
     * @throws Exception if API request or database save operation fails
     */
    suspend fun fetchHistoricalWeatherFromApi(
        date: String,
        city: String,
        region: String,
        country: String
    ): WeatherRecordDto?

    /**
     * Searches for historical weather data, combining database and API operations.
     * This is the ORCHESTRATOR method that:
     * 1. First attempts to retrieve from the database
     * 2. If not found, fetches from the WeatherAPI and saves it
     * Este caso de uso NO SE PUEDE USAR PARA FECHAS FUTURAS.
     *
     * @param date Date in YYYY-MM-DD format
     * @param city City name
     * @param region State/Province
     * @param country Country name
     * @return WeatherRecordDto containing weather data, or null if neither DB nor API have data
     * @throws IllegalArgumentException if date is a future date
     * @throws Exception if database or API operation fails
     */
    suspend fun obtainOrFetchHistoricalWeather(
        date: String,
        city: String,
        region: String,
        country: String
    ): WeatherRecordDto?

    /**
     * Inserts a new weather record into the database.
     *
     * @param diaryRecord DiaryRecord object to insert
     * @return ID of the inserted record
     * @throws Exception if database operation fails
     */
    suspend fun insert(diaryRecord: DiaryRecord): Long

    /**
     * Retrieves all weather records from the database.
     *
     * @return List of all DiaryRecord records
     * @throws Exception if database operation fails
     */
    suspend fun obtainAll(): List<DiaryRecord>

    /**
     * Updates an existing weather record in the database.
     *
     * @param diaryRecord DiaryRecord object with updated data
     * @return Number of records updated
     * @throws Exception if database operation fails
     */
    suspend fun update(diaryRecord: DiaryRecord): Int

    /**
     * Deletes a weather record from the database by ID.
     *
     * @param id Primary key of the record to delete
     * @return Number of records deleted
     * @throws Exception if database operation fails
     */
    suspend fun delete(id: Int): Int

    /**
     * Retrieves a weather record by date, city, region, and country.
     *
     * @param date Date in YYYY-MM-DD format
     * @param city City name
     * @param region State/Province
     * @param country Country name
     * @return DiaryRecord object if found, null otherwise
     * @throws Exception if database operation fails
     */
    suspend fun obtainByDateAndLocation(
        date: String,
        city: String,
        region: String,
        country: String
    ): DiaryRecord?
}