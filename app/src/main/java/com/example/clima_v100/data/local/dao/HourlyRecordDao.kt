package com.example.clima_v100.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.clima_v100.data.local.entity.HourlyRecord

@Dao
interface HourlyRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HourlyRecord): Long

    @Query("SELECT * FROM hourly_record")
    suspend fun getAll(): List<HourlyRecord>

    @Update
    suspend fun update(record: HourlyRecord): Int

    @Query("DELETE FROM hourly_record WHERE id = :id")
    suspend fun delete(id: Int): Int

    @Query("SELECT * FROM hourly_record WHERE diary_record_id = :diaryRecordId")
    suspend fun getByDiaryRegisterId(diaryRecordId: Int): List<HourlyRecord>

    @Query("SELECT * FROM hourly_record WHERE diary_record_id = :diaryRecordId AND hour = :hour")
    suspend fun getByDiaryRegisterIdAndHour(diaryRecordId: Int, hour: Int): HourlyRecord?
}