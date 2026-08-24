package com.example.planmanagerapp.data.remote.dto

// 💡 수정: cycleType(String) -> intervalDays(Int)로 롤백
data class RoutineCreateRequest(
    val title: String,
    val intervalDays: Int
)

data class RoutineResponse(
    val id: Long,
    val title: String,
    val intervalDays: Int,
    val status: String
)

data class RoutineRecordResponse(
    val id: Long,
    val routineId: Long,
    val title: String,
    val targetDate: String,
    val isCompleted: Boolean
)