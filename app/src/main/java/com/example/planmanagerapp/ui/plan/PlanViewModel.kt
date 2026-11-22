package com.example.planmanagerapp.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.repository.PlanRepository
import com.example.planmanagerapp.data.repository.VersionConflictException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val repository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        // 특정 날짜가 아닌 전체 스트림 구독
        viewModelScope.launch {
            repository.getAllPlansStream().collect { plans ->
                _uiState.update { it.copy(plans = plans) }
            }
        }
        refreshData()
    }

    fun handleIntent(intent: PlanIntent) {
        when (intent) {
            is PlanIntent.DeletePlan -> deletePlan(intent.id)
            is PlanIntent.FetchPlans -> refreshData()
            is PlanIntent.CreatePlan -> createPlan(intent.title, intent.date)
            is PlanIntent.TogglePlan -> togglePlan(intent.id, intent.currentStatus)
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.refreshAllPlans()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "동기화 실패: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun createPlan(title: String, date: String) {
        viewModelScope.launch {
            try {
                repository.createPlan(title, date)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "계획 생성 실패: ${e.message}") }
            }
        }
    }

    private fun togglePlan(id: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            try {
                repository.togglePlan(id, currentStatus)
            } catch (e: VersionConflictException) {
                // 다른 기기의 변경뿐 아니라 자정 초기화로 version이 오른 경우도 여기로 온다.
                _uiState.update { it.copy(errorMessage = "이미 바뀐 계획이라 최신 상태로 다시 불러왔어요") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "상태 변경 실패") }
            }
        }
    }

    private fun deletePlan(id: Long) {
        viewModelScope.launch {
            try {
                repository.deletePlan(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "삭제 실패: ${e.message}") }
            }
        }
    }
}