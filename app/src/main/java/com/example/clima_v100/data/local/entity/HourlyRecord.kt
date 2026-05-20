package com.example.clima_v100.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "hourly_record",
    foreignKeys = [
        ForeignKey(
            entity = DiaryRecord::class,
            parentColumns = ["id"],
            childColumns = ["diary_record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HourlyRecord(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "diary_record_id")
    var diaryRecordId: Int,
    var hour: Int,             // 0-23 (hour of day)
    @ColumnInfo(name = "temp_fahrenheit")
    var tempFahrenheit: Float,
    @ColumnInfo(name = "temp_celsius")
    var tempCelsius: Float
)