package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp Interceptor는 워커 스레드에서 실행되므로 runBlocking 사용이 안전함
        val token = runBlocking { tokenManager.getAccessToken() }

        val requestBuilder = chain.request().newBuilder()

        // 토큰이 존재할 경우에만 Authorization 헤더 추가
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}