package com.example.planmanagerapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SignUpRequest(
    val username: String, // 💡 백엔드 스펙 변경에 따른 신규 필드 추가
    val email: String,
    val password: String
)

data class LoginRequest(
    val loginId: String, // 💡 필드명을 서버 DTO와 정확히 일치시킵니다.
    val password: String
)
data class AuthResponse(
    // 💡 로그에 찍힌 키값 "accessToken"과 정확히 매핑
    @SerializedName("accessToken")
    val accessToken: String?,

    // 💡 로그에 찍힌 키값 "refreshToken"과 정확히 매핑
    @SerializedName("refreshToken")
    val refreshToken: String?,

    val message: String
)