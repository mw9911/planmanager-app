package com.example.planmanagerapp.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        // 뷰모델 초기화 시 현재 월의 데이터 자동 조회
        fetchCalendarData(_uiState.value.currentMonth)
    }

    fun handleIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.ChangeMonth -> {
                _uiState.update { it.copy(currentMonth = intent.yearMonth) }
                fetchCalendarData(intent.yearMonth)
            }
            is CalendarIntent.Refresh -> {
                fetchCalendarData(_uiState.value.currentMonth)
            }
        }
    }

    private fun fetchCalendarData(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 💡 YearMonth를 기반으로 해당 월의 1일과 말일을 YYYY-MM-DD 문자열로 변환
            val startDate = yearMonth.atDay(1).toString()
            val endDate = yearMonth.atEndOfMonth().toString()

            repository.getCalendarItems(startDate, endDate).fold(
                onSuccess = { items ->
                    _uiState.update { state ->
                        state.copy(
                            items = items,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "달력 데이터를 불러오지 못했습니다."
                        )
                    }
                }
            )
        }
    }
}