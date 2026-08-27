package com.example.planmanagerapp.data.remote.dto

data class SignUpRequest(
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    // 기존 val token: String? 을 아래 2개로 분리
    val accessToken: String?,
    val refreshToken: String?,
    val message: String
)