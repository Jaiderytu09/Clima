package com.example.clima_v100.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_record",
    indices = [Index(value = ["date", "city", "region", "country"], unique = true)]
)
data class DiaryRecord(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var date: String,           // YYYY-MM-DD format (UTC)
    var city: String,
    var region: String,         // State/Province
    var country: String,
    var latitude: Double? = null,
    var longitude: Double? = null,
    @ColumnInfo(name = "temp_max_fahrenheit")
    var maxTempFahrenheit: Float,
    @ColumnInfo(name = "temp_min_fahrenheit")
    var minTempFahrenheit: Float,
    @ColumnInfo(name = "temp_max_celsius")
    var maxTempCelsius: Float,
    @ColumnInfo(name = "temp_min_celsius")
    var minTempCelsius: Float
)