package com.example.clima_v100.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User preference singleton entity.
 * Only ONE record should exist in the database (enforced at ID = 1).
 *
 * Singleton Pattern:
 * - Always uses id = 1 to ensure only one preference record exists
 * - When updating, the database replaces the existing record
 * - When inserting with REPLACE strategy, it overwrites the record at id=1
 */
@Entity(tableName = "user_preference")
data class UserPreference(
    @PrimaryKey
    var id: Int = 1,  // FIXED to 1 - only one record per app
    @ColumnInfo(name = "fav_temp_scale")
    var favTempScale: String = "C",
    @ColumnInfo(name = "is_24_hour_format")
    var is24HourFormat: Boolean = false
)