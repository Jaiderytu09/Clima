package com.example.clima_v100.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.clima_v100.data.local.entity.Suggestion

@Dao
interface SuggestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suggestion: Suggestion): Long

    @Query("SELECT * FROM suggestion")
    suspend fun getAll(): List<Suggestion>

    @Update
    suspend fun update(suggestion: Suggestion): Int

    @Query("DELETE FROM suggestion WHERE id = :id")
    suspend fun delete(id: Int): Int
}