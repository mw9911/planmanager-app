package com.example.planmanagerapp.ui.calendar

import com.example.planmanagerapp.data.remote.dto.CalendarItemResponse
import java.time.YearMonth

// 💡 화면의 유일한 진실 공급원(SSOT)
data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val items: List<CalendarItemResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// 💡 사용자의 액션 정의
sealed class CalendarIntent {
    data class ChangeMonth(val yearMonth: YearMonth) : CalendarIntent()
    object Refresh : CalendarIntent()
}