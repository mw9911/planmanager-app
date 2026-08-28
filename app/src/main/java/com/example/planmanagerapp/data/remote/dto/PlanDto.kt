package com.example.planmanagerapp.data.remote.dto

// @SerializedName 어노테이션을 모두 제거합니다.
data class PlanCreateRequest(
    val title: String,
    val planDate: String // ⬅️ 서버의 planDate 필드와 이름이 정확히 일치합니다.
)

data class PlanResponse(
    val id: Long,
    val title: String,
    val planDate: String, // ⬅️ 서버의 planDate 필드와 이름이 정확히 일치합니다.
    val isCompleted: Boolean
)

data class PlanSyncRequest(
    val title: String,
    val planDate: String,
    val isCompleted: Boolean
)