package com.example.clima_v100.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.clima_v100.data.local.dao.DiaryRecordDao
import com.example.clima_v100.data.local.dao.HourlyRecordDao
import com.example.clima_v100.data.local.dao.SuggestionDao
import com.example.clima_v100.data.local.dao.UserPreferenceDao
import com.example.clima_v100.data.local.entity.DiaryRecord
import com.example.clima_v100.data.local.entity.HourlyRecord
import com.example.clima_v100.data.local.entity.Suggestion
import com.example.clima_v100.data.local.entity.UserPreference
import com.example.clima_v100.data.local.entity.User
import com.example.clima_v100.data.local.dao.UserDao

@Database(
    entities = [
        UserPreference::class,
        DiaryRecord::class,
        HourlyRecord::class,
        Suggestion::class,
        User::class          // 👈 agrega esta línea

    ],
    version = 3,
    exportSchema = false
)


abstract class AppDatabase : RoomDatabase() {

    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun diaryRegisterDao(): DiaryRecordDao
    abstract fun hourlyRegisterDao(): HourlyRecordDao
    abstract fun suggestionDao(): SuggestionDao

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clima_app.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

