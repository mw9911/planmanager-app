package com.example.planmanagerapp.di

import android.content.Context
import androidx.room.Room
import com.example.planmanagerapp.data.local.AppDatabase
import com.example.planmanagerapp.data.local.dao.PlanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule { // ⬅️ 여기서 열린 중괄호가

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "plan_manager_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun providePlanDao(database: AppDatabase): PlanDao {
        return database.planDao()
    }

} // ⬅️ 💡 반드시 파일 맨 마지막(모든 함수의 끝)에서 닫혀야 합니다. 이 바깥에 코드가 있으면 안 됩니다.