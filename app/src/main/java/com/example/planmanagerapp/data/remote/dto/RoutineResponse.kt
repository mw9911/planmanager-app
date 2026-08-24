package com.example.planmanagerapp.data.remote.dto

data class RoutineGroupResponse(
    val id: Long,
    val title: String,
    val intervalDays: Int,
    val items: List<RoutineItemResponse>
)

data class RoutineItemResponse(
    val id: Long,
    val title: String,
    val isCompleted: Boolean
)