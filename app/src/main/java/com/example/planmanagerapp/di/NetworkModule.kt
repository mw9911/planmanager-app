package com.example.planmanagerapp.di

import com.example.planmanagerapp.data.remote.api.AuthApiService
import com.example.planmanagerapp.data.remote.api.AuthInterceptor
import com.example.planmanagerapp.data.remote.api.CalendarApiService
import com.example.planmanagerapp.data.remote.api.PlanApiService
import com.example.planmanagerapp.data.remote.api.RoutineApiService // 💡 임포트 추가
import com.example.planmanagerapp.data.remote.api.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val BASE_URL = "https://planmanager-api.duckdns.org/"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator // 💡 Authenticator 주입 추가
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator) // 💡 401 에러 발생 시 캐치하는 Authenticator 장착
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePlanApiService(retrofit: Retrofit): PlanApiService {
        return retrofit.create(PlanApiService::class.java)
    }

    // 💡 누락되었던 RoutineApiService 제공 코드 추가
    @Provides
    @Singleton
    fun provideRoutineApiService(retrofit: Retrofit): RoutineApiService {
        return retrofit.create(RoutineApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCalendarApiService(retrofit: Retrofit): CalendarApiService {
        return retrofit.create(CalendarApiService::class.java)
    }
}