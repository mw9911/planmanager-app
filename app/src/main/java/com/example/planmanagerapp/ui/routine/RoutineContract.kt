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
    data class ToggleItem(val itemId: Long) : RoutineIntent()
    data class ChangeDate(val date: String) : RoutineIntent()
}