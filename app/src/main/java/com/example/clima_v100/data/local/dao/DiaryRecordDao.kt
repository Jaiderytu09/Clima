package com.example.clima_v100.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.clima_v100.data.local.entity.DiaryRecord

@Dao
interface DiaryRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DiaryRecord): Long

    @Query("SELECT * FROM diary_record")
    suspend fun getAll(): List<DiaryRecord>

    @Update
    suspend fun update(record: DiaryRecord): Int

    @Query("DELETE FROM diary_record WHERE id = :id")
    suspend fun delete(id: Int): Int

    /**
     * Retrieves a weather record by date, city, region, and country.
     * @param date Date in YYYY-MM-DD format
     * @param city City name
     * @param region State/Province
     * @param country Country name
     * @return DiaryRecord if found, null otherwise
     */
    @Query("SELECT * FROM diary_record WHERE date = :date AND city = :city AND region = :region AND country = :country")
    suspend fun getByDateCityRegionCountry(
        date: String,
        city: String,
        region: String,
        country: String
    ): DiaryRecord?
}