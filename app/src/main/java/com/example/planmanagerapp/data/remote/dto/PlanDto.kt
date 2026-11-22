package com.example.planmanagerapp.data.remote.dto

data class PlanCreateRequest(
    val title: String,
    val planDate: String
)

data class PlanResponse(
    val id: Long,
    val title: String,
    val planDate: String,
    val isCompleted: Boolean,
    val version: Long = 0
)

data class PlanSyncRequest(
    val title: String,
    val planDate: String,
    val isCompleted: Boolean
)