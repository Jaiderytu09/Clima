package com.example.clima_v100.data.repository.impl

import android.util.Log
import com.example.clima_v100.BuildConfig
import com.example.clima_v100.data.local.dao.DiaryRecordDao
import com.example.clima_v100.data.local.dto.WeatherRecordDto
import com.example.clima_v100.data.local.entity.DiaryRecord
import com.example.clima_v100.data.remote.WeatherApiService
import com.example.clima_v100.data.repository.IRepositoryDiaryRecord
import com.example.clima_v100.data.repository.utils.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of IRepositoryDiaryRecord.
 * Handles weather record management with database and API operations.
 * Implements atomic transactions for data consistency.
 *
 * Architecture:
 * - obtainHistoricalWeatherFromDatabase(): Pure database retrieval
 * - fetchHistoricalWeatherFromApi(): Pure API fetch with database persistence
 * - obtainOrFetchHistoricalWeather(): Orchestrator that combines both
 */
class RepositoryDiaryRecord(
    private val diaryRecordDao: DiaryRecordDao,
    private val weatherApiService: WeatherApiService
) : IRepositoryDiaryRecord {

    companion object {
        private const val TAG = "RepositoryDiaryRecord"
    }

    /**
     * Validates that the given date is not in the future.
     * @param date Date in YYYY-MM-DD format
     * @throws IllegalArgumentException if date is a future date
     */
    private fun validateNotFutureDate(date: String) {
        if (!TimeUtil.isValidHistoricalDate(date)) {
            val errorMessage = "Cannot fetch weather for future dates. Provided date: $date"
            Log.e(TAG, errorMessage)
            throw IllegalArgumentException(errorMessage)
        }
    }

    /**
     * Converts a DiaryRecord entity to a WeatherRecordDto (without ID).
     * Useful for returning clean data from use cases.
     */
    private fun toWeatherRecordDto(diaryRecord: DiaryRecord): WeatherRecordDto {
        return WeatherRecordDto(
            date = diaryRecord.date,
            city = diaryRecord.city,
            region = diaryRecord.region,
            country = diaryRecord.country,
            latitude = diaryRecord.latitude,
            longitude = diaryRecord.longitude,
            maxTempFahrenheit = diaryRecord.maxTempFahrenheit,
            minTempFahrenheit = diaryRecord.minTempFahrenheit,
            maxTempCelsius = diaryRecord.maxTempCelsius,
            minTempCelsius = diaryRecord.minTempCelsius
        )
    }

    /**
     * Retrieves historical weather data from the local database.
     * This is a pure database query operation with NO API fallback.
     * Responsibility: Read from database only.
     */
    override suspend fun obtainHistoricalWeatherFromDatabase(
        date: String,
        city: String,
        region: String,
        country: String
    ): WeatherRecordDto? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(
                    TAG,
                    "Querying database for weather record: date=$date, city=$city, region=$region, country=$country"
                )
                val record =
                    diaryRecordDao.getByDateCityRegionCountry(date, city, region, country)

                if (record != null) {
                    Log.d(
                        TAG,
                        "Weather record found in database for $date in $city, $region, $country"
                    )
                    return@withContext toWeatherRecordDto(record)
                } else {
                    Log.d(
                        TAG,
                        "No weather record found in database for $date in $city, $region, $country"
                    )
                    return@withContext null
                }
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Error querying database for $date in $city: ${exception.message}",
                    exception
                )
                throw exception
            }
        }
    }

    /**
     * Fetches historical weather data from WeatherAPI for a specific date and city.
     * This is a pure API operation that automatically saves the result to the database.
     * Responsibility: Fetch from API and persist to database.
     */
    override suspend fun fetchHistoricalWeatherFromApi(
        date: String,
        city: String,
        region: String,
        country: String
    ): WeatherRecordDto? {
        return withContext(Dispatchers.IO) {
            try {
                validateNotFutureDate(date)

                Log.d(TAG, "Fetching weather data from API for date=$date, city=$city")
                val apiResponse = weatherApiService.getHistoricalWeather(
                    apiKey = BuildConfig.API_KEY,
                    query = city,
                    date = date
                )

                // Extract weather data from API response
                val forecastDay = apiResponse.forecast.forecastday.firstOrNull()
                    ?: throw Exception("No forecast data received from API for $date")

                val dayData = forecastDay.day
                val diaryRecord = DiaryRecord(
                    date = TimeUtil.extractDateFromUtcDateTime(forecastDay.date),
                    city = city,
                    region = region,
                    country = country,
                    latitude = null,  // Will be set by ViewModel if available
                    longitude = null, // Will be set by ViewModel if available
                    maxTempFahrenheit = dayData.maxtemp_f,
                    minTempFahrenheit = dayData.mintemp_f,
                    maxTempCelsius = dayData.maxtemp_c,
                    minTempCelsius = dayData.mintemp_c
                )

                // Save to database (atomic operation)
                val insertedId = diaryRecordDao.insert(diaryRecord)
                Log.d(
                    TAG,
                    "Weather data successfully saved to database with ID: $insertedId for $date in $city, $country"
                )

                // Return as DTO (without ID, following separation of concerns)
                return@withContext toWeatherRecordDto(diaryRecord)
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Error fetching weather from API for $date in $city: ${exception.message}",
                    exception
                )
                throw exception
            }
        }
    }

    /**
     * Searches for historical weather data, combining database and API operations.
     * This is the ORCHESTRATOR method that:
     * 1. First attempts to retrieve from the database
     * 2. If not found, fetches from the WeatherAPI and saves it
     * Responsibility: Coordinate database and API operations.
     */
    override suspend fun obtainOrFetchHistoricalWeather(
        date: String,
        city: String,
        region: String,
        country: String
    ): WeatherRecordDto? {
        return withContext(Dispatchers.IO) {
            try {
                validateNotFutureDate(date)

                Log.d(
                    TAG,
                    "obtainOrFetchHistoricalWeather: Attempting to retrieve weather for date=$date, city=$city, country=$country"
                )

                // Step 1: Try to get from database
                val databaseRecord =
                    obtainHistoricalWeatherFromDatabase(date, city, region, country)
                if (databaseRecord != null) {
                    Log.d(TAG, "Weather retrieved from database for $date in $city, $country")
                    return@withContext databaseRecord
                }

                // Step 2: If not found in database, fetch from API
                Log.d(TAG, "Weather not found in database, fetching from API for $date in $city")
                return@withContext fetchHistoricalWeatherFromApi(date, city, region, country)
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Error in obtainOrFetchHistoricalWeather for $date in $city: ${exception.message}",
                    exception
                )
                throw exception
            }
        }
    }

    /**
     * Inserts a new weather record into the database.
     */
    override suspend fun insert(diaryRecord: DiaryRecord): Long {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(
                    TAG,
                    "Inserting weather record for ${diaryRecord.date} in ${diaryRecord.city}, ${diaryRecord.country}"
                )
                val insertedId = diaryRecordDao.insert(diaryRecord)
                Log.d(TAG, "Record inserted successfully with ID: $insertedId")
                return@withContext insertedId
            } catch (exception: Exception) {
                Log.e(TAG, "Error inserting record: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Retrieves all weather records from the database.
     */
    override suspend fun obtainAll(): List<DiaryRecord> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching all weather records from database")
                val records = diaryRecordDao.getAll()
                Log.d(TAG, "Retrieved ${records.size} weather records")
                return@withContext records
            } catch (exception: Exception) {
                Log.e(TAG, "Error retrieving all records: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Updates an existing weather record in the database.
     */
    override suspend fun update(diaryRecord: DiaryRecord): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating weather record ID: ${diaryRecord.id}")
                val updatedCount = diaryRecordDao.update(diaryRecord)
                Log.d(TAG, "Updated $updatedCount record(s)")
                return@withContext updatedCount
            } catch (exception: Exception) {
                Log.e(TAG, "Error updating record: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Deletes a weather record from the database by ID.
     */
    override suspend fun delete(id: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting weather record with ID: $id")
                val deletedCount = diaryRecordDao.delete(id)
                Log.d(TAG, "Deleted $deletedCount record(s)")
                return@withContext deletedCount
            } catch (exception: Exception) {
                Log.e(TAG, "Error deleting record: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Retrieves a weather record by date, city, region, and country.
     */
    override suspend fun obtainByDateAndLocation(
        date: String,
        city: String,
        region: String,
        country: String
    ): DiaryRecord? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching weather record for $date in $city, $region, $country")
                val record =
                    diaryRecordDao.getByDateCityRegionCountry(date, city, region, country)
                if (record != null) {
                    Log.d(TAG, "Record found with ID: ${record.id}")
                } else {
                    Log.d(TAG, "No record found for $date in $city, $region, $country")
                }
                return@withContext record
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Error fetching record by date and location: ${exception.message}",
                    exception
                )
                throw exception
            }
        }
    }
}

