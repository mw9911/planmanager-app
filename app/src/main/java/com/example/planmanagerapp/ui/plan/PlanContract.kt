package com.example.planmanagerapp.ui.plan

import com.example.planmanagerapp.data.remote.dto.PlanResponse

data class PlanUiState(
    val isLoading: Boolean = false,
    val plans: List<PlanResponse> = emptyList(), // 날짜 정렬된 전체 리스트
    val errorMessage: String? = null
)

sealed class PlanIntent {
    object FetchPlans : PlanIntent()
    // 💡 생성 시 제목과 '날짜'를 함께 받음
    data class CreatePlan(val title: String, val date: String) : PlanIntent()
    data class TogglePlan(val id: Long, val currentStatus: Boolean) : PlanIntent()
    data class DeletePlan(val id: Long) : PlanIntent()
}