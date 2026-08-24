package com.example.planmanagerapp.data.remote.dto

// (조회 응답용 DTO인 RoutineGroupResponse, RoutineItemResponse는 기존 유지)

// 1. 그룹 생성 전용
data class RoutineGroupCreateRequest(
    val title: String,
    val intervalDays: Int
)

// 2. 단건 아이템 생성 전용
data class RoutineItemCreateRequest(
    val title: String
)