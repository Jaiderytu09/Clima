package com.example.clima_v100.di

import android.content.Context
import com.example.clima_v100.data.local.dao.DiaryRecordDao
import com.example.clima_v100.data.local.dao.HourlyRecordDao
import com.example.clima_v100.data.local.dao.SuggestionDao
import com.example.clima_v100.data.local.dao.UserDao
import com.example.clima_v100.data.local.dao.UserPreferenceDao
import com.example.clima_v100.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideDiaryRecordDao(database: AppDatabase): DiaryRecordDao {
        return database.diaryRegisterDao()
    }

    @Provides
    @Singleton
    fun provideHourlyRecordDao(database: AppDatabase): HourlyRecordDao {
        return database.hourlyRegisterDao()
    }

    @Provides
    @Singleton
    fun provideSuggestionDao(database: AppDatabase): SuggestionDao {
        return database.suggestionDao()
    }

    @Provides
    @Singleton
    fun provideUserPreferenceDao(database: AppDatabase): UserPreferenceDao {
        return database.userPreferenceDao()
    }
}