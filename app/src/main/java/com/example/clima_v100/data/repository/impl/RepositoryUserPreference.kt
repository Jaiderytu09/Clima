package com.example.clima_v100.data.repository.impl

import android.util.Log
import com.example.clima_v100.data.local.dao.UserPreferenceDao
import com.example.clima_v100.data.local.entity.UserPreference
import com.example.clima_v100.data.repository.IRepositoryUserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of IRepositoryUserPreference.
 * Handles user preference management with database operations and validation.
 *
 * Architecture:
 * - insert(): Create new preference record with validation
 * - update(): Modify existing preference with validation
 * - obtainCurrentPreference(): Retrieve the current user preference (singleton pattern)
 *
 * Validation Rules:
 * - Temperature unit (favTempScale) must be "C" or "F"
 * - Format militar (is24HourFormat) must be boolean (true = 24h, false = 12h AM/PM)
 */
class RepositoryUserPreference @Inject constructor(
    private val userPreferenceDao: UserPreferenceDao
) : IRepositoryUserPreference {

    companion object {
        private const val TAG = "RepositoryUserPreference"
        private const val VALID_TEMP_UNIT_C = "C"
        private const val VALID_TEMP_UNIT_F = "F"
    }

    /**
     * Validates that the temperature unit is either "C" or "F".
     * @param favTempScale Temperature unit preference
     * @throws IllegalArgumentException if temperature unit is not valid
     */
    private fun validateTemperatureUnit(favTempScale: String) {
        if (favTempScale != VALID_TEMP_UNIT_C && favTempScale != VALID_TEMP_UNIT_F) {
            val errorMessage = "Invalid temperature unit: $favTempScale. Must be 'C' or 'F'"
            Log.e(TAG, errorMessage)
            throw IllegalArgumentException(errorMessage)
        }
    }

    /**
     * Inserts a new user preference into the database with validation.
     * IMPORTANT: Enforces singleton pattern by setting id=1.
     * If preference already exists, it will be replaced (REPLACE conflict strategy).
     */
    override suspend fun insert(preference: UserPreference): Long {
        return withContext(Dispatchers.IO) {
            try {
                validateTemperatureUnit(preference.favTempScale)
                // ENFORCE SINGLETON: Always use id=1
                val singletonPreference = preference.copy(id = 1)
                Log.d(
                    TAG,
                    "Inserting/Replacing preference (id=1): tempUnit=${singletonPreference.favTempScale}, is 24h format=${singletonPreference.is24HourFormat}"
                )
                val insertedId = userPreferenceDao.insert(singletonPreference)
                Log.d(TAG, "Preference saved with ID: $insertedId (singleton pattern)")
                return@withContext insertedId
            } catch (exception: Exception) {
                Log.e(TAG, "Error inserting preference: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Updates an existing user preference in the database with validation.
     * IMPORTANT: Enforces singleton pattern by setting id=1.
     */
    override suspend fun update(preference: UserPreference): Int {
        return withContext(Dispatchers.IO) {
            try {
                validateTemperatureUnit(preference.favTempScale)
                // ENFORCE SINGLETON: Always use id=1
                val singletonPreference = preference.copy(id = 1)
                Log.d(
                    TAG,
                    "Updating preference (id=1): tempUnit=${singletonPreference.favTempScale}, is 24h format=${singletonPreference.is24HourFormat}"
                )
                val updatedCount = userPreferenceDao.update(singletonPreference)
                Log.d(TAG, "Updated $updatedCount preference record(s) (singleton pattern)")
                return@withContext updatedCount
            } catch (exception: Exception) {
                Log.e(TAG, "Error updating preference: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Retrieves the current user preference (singleton pattern - id=1).
     * This is the ONLY preference that should exist in the database.
     * Returns null only on first app launch before initialization.
     * Uses optimized DAO query that directly fetches id=1.
     *
     * @return UserPreference object if found, null otherwise
     */
    override suspend fun obtainCurrentPreference(): UserPreference? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching current user preference (id=1)")
                val preference = userPreferenceDao.getSingleton()
                if (preference != null) {
                    Log.d(
                        TAG,
                        "Preference found: tempUnit=${preference.favTempScale}, 24h=${preference.is24HourFormat}"
                    )
                } else {
                    Log.d(TAG, "No preference found (first launch), returning null")
                }
                return@withContext preference
            } catch (exception: Exception) {
                Log.e(TAG, "Error retrieving current preference: ${exception.message}", exception)
                throw exception
            }
        }
    }
}