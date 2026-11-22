package com.example.planmanagerapp.ui.routine

import com.example.planmanagerapp.data.remote.dto.RoutineGroupResponse

data class RoutineUiState(
    val isLoading: Boolean = false,
    val selectedDate: String = "",
    val groups: List<RoutineGroupResponse> = emptyList(),
    val errorMessage: String? = null
)

sealed class RoutineIntent {
    object FetchRoutines : RoutineIntent()
    data class CreateGroup(val title: String, val intervalDays: Int) : RoutineIntent()
    data class AddItem(val groupId: Long, val title: String) : RoutineIntent()

    // 💡 수정됨: itemId와 함께 현재 상태(currentStatus)를 전달받도록 변경
    data class ToggleItem(val itemId: Long, val currentStatus: Boolean) : RoutineIntent()

    data class ChangeDate(val date: String) : RoutineIntent()
    data class DeleteGroup(val groupId: Long) : RoutineIntent()
    data class DeleteItem(val itemId: Long) : RoutineIntent()
}