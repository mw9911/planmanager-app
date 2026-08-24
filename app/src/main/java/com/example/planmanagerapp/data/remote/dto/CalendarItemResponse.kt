package com.example.planmanagerapp.data.remote.dto

data class CalendarItemResponse(
    val id: Long,
    val type: String, // "PLAN" 또는 "ROUTINE"
    val title: String,
    val targetDate: String, // "YYYY-MM-DD" 형태
    val isCompleted: Boolean
)