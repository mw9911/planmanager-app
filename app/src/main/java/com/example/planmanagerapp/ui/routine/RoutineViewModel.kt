package com.example.planmanagerapp.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.repository.RoutineRepository
import com.example.planmanagerapp.data.repository.VersionConflictException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val repository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState(selectedDate = LocalDate.now().toString()))
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    init {
        // 날짜가 바뀔 때마다 해당 날짜의 DB 스트림을 자동으로 구독
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .collect { date ->
                    repository.getRoutinesStream(date).collect { groups ->
                        _uiState.update { it.copy(groups = groups) }
                    }
                }
        }
        fetchRoutines() // 초기 데이터 서버 동기화
    }

    fun handleIntent(intent: RoutineIntent) {
        val currentDate = _uiState.value.selectedDate
        when (intent) {
            is RoutineIntent.FetchRoutines -> fetchRoutines()
            is RoutineIntent.ChangeDate -> {
                _uiState.update { it.copy(selectedDate = intent.date) }
                fetchRoutines()
            }
            is RoutineIntent.CreateGroup -> createGroup(intent.title, intent.intervalDays, currentDate)
            is RoutineIntent.AddItem -> addItem(intent.groupId, intent.title, currentDate)
            is RoutineIntent.ToggleItem -> toggleItem(intent.itemId, intent.currentStatus)
            is RoutineIntent.DeleteGroup -> deleteGroup(intent.groupId, currentDate)
            is RoutineIntent.DeleteItem -> deleteItem(intent.itemId, currentDate)
        }
    }

    private fun fetchRoutines() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.fetchAndCacheRoutines(_uiState.value.selectedDate)
            } catch (e: IOException) {
                // 네트워크 오류: 로컬 캐시를 그대로 보여 준다.
                _uiState.update { it.copy(errorMessage = "오프라인 모드입니다. 로컬 캐시 데이터를 표시합니다.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "조회 에러: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun createGroup(title: String, intervalDays: Int, date: String) {
        viewModelScope.launch {
            try { repository.createGroup(title, intervalDays, date) }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    private fun addItem(groupId: Long, title: String, date: String) {
        viewModelScope.launch {
            try { repository.addItem(groupId, title, date) }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    private fun toggleItem(itemId: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleItem(itemId, currentStatus)
            } catch (e: VersionConflictException) {
                _uiState.update { it.copy(errorMessage = "다른 기기에서 먼저 바뀌어 최신 상태로 맞췄어요") }
            } catch (e: IOException) {
                _uiState.update { it.copy(errorMessage = "오프라인 상태에서는 변경사항을 저장할 수 없습니다.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "상태 변경 실패") }
            }
        }
    }

    private fun deleteGroup(groupId: Long, date: String) {
        viewModelScope.launch {
            try { repository.deleteGroup(groupId, date) }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = "그룹 삭제 실패: ${e.message}") } }
        }
    }

    private fun deleteItem(itemId: Long, date: String) {
        viewModelScope.launch {
            try { repository.deleteItem(itemId, date) }
            catch (e: Exception) { _uiState.update { it.copy(errorMessage = "항목 삭제 실패: ${e.message}") } }
        }
    }
}