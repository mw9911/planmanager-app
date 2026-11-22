package com.example.planmanagerapp.data.remote.dto

data class ToggleRequest(
    val version: Long?
)

data class ToggleResponse(
    val id: Long,
    val isCompleted: Boolean,
    val version: Long
)