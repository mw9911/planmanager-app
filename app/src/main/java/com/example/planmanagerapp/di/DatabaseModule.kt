package com.example.planmanagerapp.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.planmanagerapp.data.local.AppDatabase
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.dao.RoutineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.SharedPreferences
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
            .fallbackToDestructiveMigration() // 💡 DB 버전 변경 시 기존 데이터를 날리고 새 스키마로 덮어씌움 (개발 단계에서 권장)
            .build()
    }

    @Provides
    fun providePlanDao(database: AppDatabase): PlanDao {
        return database.planDao()
    }

    // 💡 새로 추가: RoutineDao 의존성 제공
    @Provides
    fun provideRoutineDao(database: AppDatabase): RoutineDao {
        return database.routineDao()
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        // 1. 하드웨어 Keystore 기반의 마스터 키 생성 (AES256-GCM)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // 2. 키와 값 모두 암호화하는 SharedPreferences 객체 반환
        return EncryptedSharedPreferences.create(
            context,
            "secret_auth_prefs", // 💡 기존과 다른 새로운 암호화 전용 파일명 사용
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}