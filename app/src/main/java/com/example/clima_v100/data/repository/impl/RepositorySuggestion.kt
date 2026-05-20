package com.example.clima_v100.data.repository.impl

import android.util.Log
import com.example.clima_v100.data.local.dao.SuggestionDao
import com.example.clima_v100.data.local.entity.Suggestion
import com.example.clima_v100.data.repository.IRepositorySuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of IRepositorySugerencia.
 * Handles user feedback/suggestions management with database operations.
 *
 * Architecture:
 * - insert(): Create new feedback record
 * - obtainAll(): Retrieve all feedback records
 * - update(): Modify existing feedback
 * - delete(): Remove feedback by ID
 */
class RepositorySuggestion @Inject constructor(
    private val suggestionDao: SuggestionDao
) : IRepositorySuggestion {

    companion object {
        private const val TAG = "RepositorySuggestion"
    }

    /**
     * Inserts a new suggestion/feedback into the database.
     */
    override suspend fun insert(suggestion: Suggestion): Long {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(
                    TAG,
                    "Inserting suggestion from ${suggestion.fullName}: ${
                        suggestion.message.take(
                            50
                        )
                    }..."
                )
                val insertedId = suggestionDao.insert(suggestion)
                Log.d(TAG, "Suggestion inserted successfully with ID: $insertedId")
                return@withContext insertedId
            } catch (exception: Exception) {
                Log.e(TAG, "Error inserting suggestion: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Retrieves all suggestions/feedback from the database.
     */
    override suspend fun obtainAll(): List<Suggestion> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching all suggestions from database")
                val suggestions = suggestionDao.getAll()
                Log.d(TAG, "Retrieved ${suggestions.size} suggestion(s)")
                return@withContext suggestions
            } catch (exception: Exception) {
                Log.e(TAG, "Error retrieving all suggestions: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Updates an existing suggestion/feedback in the database.
     */
    override suspend fun update(suggestion: Suggestion): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating suggestion ID: ${suggestion.id}")
                val updatedCount = suggestionDao.update(suggestion)
                Log.d(TAG, "Updated $updatedCount suggestion(s)")
                return@withContext updatedCount
            } catch (exception: Exception) {
                Log.e(TAG, "Error updating suggestion: ${exception.message}", exception)
                throw exception
            }
        }
    }

    /**
     * Deletes a suggestion/feedback from the database by ID.
     */
    override suspend fun delete(id: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting suggestion with ID: $id")
                val deletedCount = suggestionDao.delete(id)
                Log.d(TAG, "Deleted $deletedCount suggestion(s)")
                return@withContext deletedCount
            } catch (exception: Exception) {
                Log.e(TAG, "Error deleting suggestion: ${exception.message}", exception)
                throw exception
            }
        }
    }
}