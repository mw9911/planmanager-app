package com.example.planmanagerapp.data.remote.dto
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

// 서버로 보낼 요청 DTO
data class RoutineSyncRequest(
    @SerializedName("targetDate")
    val targetDate: String // LocalDate를 ISO String (예: "2026-08-21")으로 직렬화
)

// 서버로부터 받을 응답 DTO
data class RoutineSyncResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)