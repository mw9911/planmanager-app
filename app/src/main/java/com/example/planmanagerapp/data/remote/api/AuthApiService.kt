// [수정된 AuthApiService.kt]
package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.remote.dto.AuthResponse
import com.example.planmanagerapp.data.remote.dto.LoginRequest
import com.example.planmanagerapp.data.remote.dto.SignUpRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // 💡 백그라운드 갱신을 위해 동기식(Call) 호출 방식 추가
    @POST("api/v1/auth/reissue")
    fun reissueTokenSync(@Header("Refresh-Token") refreshToken: String): Call<AuthResponse>
}