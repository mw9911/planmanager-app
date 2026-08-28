package com.example.planmanagerapp.data.remote.api

import android.util.Log
import com.example.planmanagerapp.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 💡 runBlocking을 제거하고 SharedPreferences에서 즉각적으로 토큰을 꺼내옴
        val token = tokenManager.getAccessToken()

        Log.d("AuthInterceptor", "Loaded Token: $token")

        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.e("AuthInterceptor", "Token is NULL or BLANK! Request sent without Auth header.")
        }

        return chain.proceed(requestBuilder.build())
    }
}