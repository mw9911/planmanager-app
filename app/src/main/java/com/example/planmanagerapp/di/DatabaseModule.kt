package com.example.planmanagerapp.di

import android.content.Context
import androidx.room.Room
import com.example.planmanagerapp.data.local.AppDatabase
import com.example.planmanagerapp.data.local.dao.PlanDao // 💡 임포트 추가
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "plan_manager_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // 💡 PlanRepository를 위해 PlanDao 주입 복구
    @Provides
    @Singleton
    fun providePlanDao(database: AppDatabase): PlanDao {
        return database.planDao()
    }
}