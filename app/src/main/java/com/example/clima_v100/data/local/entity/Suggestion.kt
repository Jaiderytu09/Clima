package com.example.clima_v100.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suggestion")
data class Suggestion(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "full_name")
    var fullName: String,
    var message: String,
    @ColumnInfo(name = "created_at_utc")
    var createdAtUtc: String
)