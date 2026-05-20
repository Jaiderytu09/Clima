package com.example.clima_v100.data.repository

import com.example.clima_v100.data.local.entity.Suggestion

/**
 * Repository interface for Suggestion (feedback/suggestion) operations.
 * Defines contract for user feedback management with basic CRUD operations.
 */
interface IRepositorySuggestion {

    /**
     * Inserts a new suggestion/feedback into the database.
     *
     * @param suggestion Suggestion object to insert
     * @return ID of the inserted record
     * @throws Exception if database operation fails
     */
    suspend fun insert(suggestion: Suggestion): Long

    /**
     * Retrieves all suggestions/feedback from the database.
     *
     * @return List of all Suggestion records
     * @throws Exception if database operation fails
     */
    suspend fun obtainAll(): List<Suggestion>

    /**
     * Updates an existing suggestion/feedback in the database.
     *
     * @param suggestion Suggestion object with updated data
     * @return Number of records updated
     * @throws Exception if database operation fails
     */
    suspend fun update(suggestion: Suggestion): Int

    /**
     * Deletes a suggestion/feedback from the database by ID.
     *
     * @param id Primary key of the record to delete
     * @return Number of records deleted
     * @throws Exception if database operation fails
     */
    suspend fun delete(id: Int): Int
}