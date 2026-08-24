package com.example.planmanagerapp.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val repository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState(selectedDate = LocalDate.now().toString()))
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    init {
        fetchRoutines()
    }

    fun handleIntent(intent: RoutineIntent) {
        when (intent) {
            is RoutineIntent.FetchRoutines -> fetchRoutines()
            is RoutineIntent.CreateGroup -> createGroup(intent.title, intent.intervalDays)
            is RoutineIntent.AddItem -> addItem(intent.groupId, intent.title)
            is RoutineIntent.ToggleItem -> toggleItem(intent.itemId)
            is RoutineIntent.ChangeDate -> {
                _uiState.update { it.copy(selectedDate = intent.date) }
                fetchRoutines()
            }
        }
    }

    private fun fetchRoutines() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val groups = repository.getRoutineGroups(_uiState.value.selectedDate)
                _uiState.update { it.copy(groups = groups, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "조회 실패: ${e.message}") }
            }
        }
    }

    private fun createGroup(title: String, intervalDays: Int) {
        viewModelScope.launch {
            try {
                repository.createGroup(title, intervalDays)
                fetchRoutines() // 성공 시 화면 즉시 갱신
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun addItem(groupId: Long, title: String) {
        viewModelScope.launch {
            try {
                repository.addItem(groupId, title)
                fetchRoutines()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun toggleItem(itemId: Long) {
        viewModelScope.launch {
            try {
                repository.toggleItem(itemId)
                fetchRoutines()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}