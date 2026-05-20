package com.example.clima_v100.data.repository.impl

import android.util.Log
import com.example.clima_v100.BuildConfig
import com.example.clima_v100.data.local.dao.HourlyRecordDao
import com.example.clima_v100.data.local.dto.LocationInfo
import com.example.clima_v100.data.local.entity.HourlyRecord
import com.example.clima_v100.data.remote.WeatherApiService
import com.example.clima_v100.data.repository.IRepositoryDiaryRecord
import com.example.clima_v100.data.repository.IRepositoryHourlyRecord
import com.example.clima_v100.data.repository.utils.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of IRepositoryHourlyRecord.
 * Handles hourly weather record management with database and API operations.
 * Provides current weather fetching and hourly record persistence.
 *
 * Architecture:
 * - fetchAndSaveCurrentWeather(): Main entry point for current weather (orchestrator method)
 * - Standard CRUD operations for hourly records
 */
class RepositoryHourlyRecord(
    private val hourlyRecordDao: HourlyRecordDao,
    private val weatherApiService: WeatherApiService,
    private val repositoryDiaryRecord: IRepositoryDiaryRecord
) : IRepositoryHourlyRecord {

    companion object {
        private const val TAG = "RepositoryHourlyRecord"
    }

    /**
     * Validates that the given hour is in valid range (0-23).
     * @param hour Hour to validate
     * @throws IllegalArgumentException if hour is outside valid range
     */
    private fun validateHour(hour: Int) {
        if (hour < 0 || hour > 23) {
            val errorMessage = "Hour must be 0-23, provided: $hour"
            Log.e(TAG, errorMessage)
            throw IllegalArgumentException(errorMessage)
        }
    }

    /**
     * Extracts hour (0-23) from WeatherAPI localtime format.
     * Format expected: "YYYY-MM-DD HH:mm" or "YYYY-MM-DD HH:mm:ss"
     * @param localtime Local time string from API
     * @return Hour in 24-hour format (0-23)
     */
    private fun extractHourFromLocaltime(localtime: String): Int {
        return try {
            val parts = localtime.split(" ")
            if (parts.size >= 2) {
                val timeParts = parts[1].split(":")
                timeParts[0].toInt()
            } else {
                Log.w(TAG, "Unexpected localtime format: $localtime")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting hour from localtime: $localtime", e)
            0
        }
    }

    /**
     * Fetches current weather data and stores/updates hourly record for today.
     * Smart upsert logic: only updates if temperatures differ.
     *
     * Process:
     * 1. Fetches today's DiaryRecord (creates if needed)
     * 2. Queries current weather API with lat/lon
     * 3. Extracts hour from API's localtime
     * 4. Checks if record exists for this hour
     *    - If exists & temps identical: no action
     *    - If exists & temps changed: UPDATE
     *    - If not exists: INSERT
     * 5. Returns the stored HourlyRecord
     */
    override suspend fun fetchAndSaveCurrentWeather(locationInfo: LocationInfo): HourlyRecord? {
        return withContext(Dispatchers.IO) {
            try {
                val today = TimeUtil.getCurrentUtcDate()
                Log.d(
                    TAG,
                    "fetchAndSaveCurrentWeather: city=${locationInfo.city}, region=${locationInfo.region}, country=${locationInfo.country}, date=$today"
                )

                // Step 1: Ensure today's DiaryRecord exists (create if needed)
                Log.d(TAG, "Fetching or creating today's DiaryRecord record")
                val weatherRecord = repositoryDiaryRecord.obtainOrFetchHistoricalWeather(
                    date = today,
                    city = locationInfo.city,
                    region = locationInfo.region,
                    country = locationInfo.country
                )
                    ?: run {
                        Log.e(TAG, "Failed to obtain or fetch DiaryRecord for today")
                        return@withContext null
                    }

                // Step 2: Get the DiaryRecord ID from database
                val diaryRecord = repositoryDiaryRecord.obtainByDateAndLocation(
                    date = today,
                    city = locationInfo.city,
                    region = locationInfo.region,
                    country = locationInfo.country
                )
                val diaryRecordId = diaryRecord?.id
                    ?: run {
                        Log.e(TAG, "Could not resolve DiaryRecord ID for today")
                        return@withContext null
                    }

                Log.d(TAG, "DiaryRecord ID resolved: $diaryRecordId")

                // Step 3: Fetch current weather from API using lat/lon coordinates
                val query = "${locationInfo.latitude},${locationInfo.longitude}"
                Log.d(TAG, "Fetching current weather from API for coordinates: $query")
                val currentWeatherResponse = weatherApiService.getCurrentWeather(
                    apiKey = BuildConfig.API_KEY,
                    query = query,
                    aqi = "no"
                )

                // Step 4: Extract hour from API response's localtime
                val currentHour =
                    extractHourFromLocaltime(currentWeatherResponse.location.localtime)
                validateHour(currentHour)
                Log.d(TAG, "Extracted current hour from API: $currentHour")

                // Step 5: Check if hourly record exists for this hour
                val existingRecord = hourlyRecordDao.getByDiaryRegisterIdAndHour(
                    diaryRecordId,
                    currentHour
                )

                val apiTempFahrenheit = currentWeatherResponse.current.temp_f
                val apiTempCelsius = currentWeatherResponse.current.temp_c

                // Step 6: Upsert based on existence and temperature changes
                return@withContext if (existingRecord != null) {
                    // Record exists: check if temps changed
                    val tempsChanged =
                        (existingRecord.tempFahrenheit != apiTempFahrenheit) ||
                                (existingRecord.tempCelsius != apiTempCelsius)

                    if (tempsChanged) {
                        Log.d(
                            TAG,
                            "Temperatures changed for hour $currentHour. Updating record (old: ${existingRecord.tempFahrenheit}°F → new: $apiTempFahrenheit°F)"
                        )
                        val updated = existingRecord.copy(
                            tempFahrenheit = apiTempFahrenheit,
                            tempCelsius = apiTempCelsius
                        )
                        hourlyRecordDao.update(updated)
                        Log.d(TAG, "Hourly record updated for hour $currentHour")
                        updated
                    } else {
                        Log.d(TAG, "Temperatures unchanged for hour $currentHour. No update needed")
                        existingRecord
                    }
                } else {
                    // Record doesn't exist: insert new
                    Log.d(
                        TAG,
                        "No existing record for hour $currentHour. Inserting new hourly record"
                    )
                    val newRecord = HourlyRecord(
                        diaryRecordId = diaryRecordId,
                        hour = currentHour,
                        tempFahrenheit = apiTempFahrenheit,
                        tempCelsius = apiTempCelsius
                    )
                    val insertedId = hourlyRecordDao.insert(newRecord)
                    Log.d(TAG, "Hourly record inserted with ID: $insertedId for hour $currentHour")
                    newRecord.copy(id = insertedId.toInt())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Error in fetchAndSaveCurrentWeather: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Inserts a new hourly weather record into the database.
     */
    override suspend fun insert(hourlyRecord: HourlyRecord): Long {
        return withContext(Dispatchers.IO) {
            try {
                validateHour(hourlyRecord.hour)
                Log.d(
                    TAG,
                    "Inserting hourly record for hour ${hourlyRecord.hour} in DiaryRecord ${hourlyRecord.diaryRecordId}"
                )
                val insertedId = hourlyRecordDao.insert(hourlyRecord)
                Log.d(TAG, "Hourly record inserted successfully with ID: $insertedId")
                return@withContext insertedId
            } catch (exception: Exception) {
                Log.e(TAG, "Error inserting hourly record: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Retrieves all hourly records for a specific day.
     */
    override suspend fun obtainDailyHourlyRecords(diaryRecordId: Int): List<HourlyRecord> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching all hourly records for DiaryRecord: $diaryRecordId")
                val records = hourlyRecordDao.getByDiaryRegisterId(diaryRecordId)
                Log.d(
                    TAG,
                    "Retrieved ${records.size} hourly record(s) for DiaryRecord: $diaryRecordId"
                )
                return@withContext records
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Error fetching hourly records for DiaryRecord: ${exception.message}",
                    exception
                )
                throw exception
            }
        }
    }

    /**
     * Retrieves a specific hourly record by day and hour.
     */
    override suspend fun obtainHourlyRecord(diaryRecordId: Int, hour: Int): HourlyRecord? {
        return withContext(Dispatchers.IO) {
            try {
                validateHour(hour)
                Log.d(
                    TAG,
                    "Fetching hourly record for DiaryRecord: $diaryRecordId, hour: $hour"
                )
                val record =
                    hourlyRecordDao.getByDiaryRegisterIdAndHour(diaryRecordId, hour)
                if (record != null) {
                    Log.d(
                        TAG,
                        "Hourly record found for hour $hour with temperature ${record.tempCelsius}°C"
                    )
                } else {
                    Log.d(TAG, "No hourly record found for hour $hour")
                }
                return@withContext record
            } catch (exception: Exception) {
                Log.e(TAG, "Error fetching hourly record: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Updates an existing hourly record.
     */
    override suspend fun update(hourlyRecord: HourlyRecord): Int {
        return withContext(Dispatchers.IO) {
            try {
                validateHour(hourlyRecord.hour)
                Log.d(TAG, "Updating hourly record ID: ${hourlyRecord.id}")
                val updatedCount = hourlyRecordDao.update(hourlyRecord)
                Log.d(TAG, "Updated $updatedCount hourly record(s)")
                return@withContext updatedCount
            } catch (exception: Exception) {
                Log.e(TAG, "Error updating hourly record: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Deletes an hourly record by ID.
     */
    override suspend fun delete(id: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting hourly record with ID: $id")
                val deletedCount = hourlyRecordDao.delete(id)
                Log.d(TAG, "Deleted $deletedCount hourly record(s)")
                return@withContext deletedCount
            } catch (exception: Exception) {
                Log.e(TAG, "Error deleting hourly record: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Retrieves all hourly records from entire database.
     */
    override suspend fun obtainAll(): List<HourlyRecord> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching all hourly records from database")
                val records = hourlyRecordDao.getAll()
                Log.d(TAG, "Retrieved ${records.size} total hourly record(s)")
                return@withContext records
            } catch (exception: Exception) {
                Log.e(TAG, "Error fetching all hourly records: ${exception.message}", exception)
                throw exception
            }
        }
    }
}

