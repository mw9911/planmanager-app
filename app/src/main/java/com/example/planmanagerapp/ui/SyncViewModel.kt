package com.example.planmanagerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.repository.RoutineRepository
import com.example.planmanagerapp.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val routineRepository: RoutineRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (isConnected) {
                    // 💡 통신이 복구된 순간 밀린 데이터 즉시 동기화
                    routineRepository.syncPendingActions()
                }
            }
        }
    }
}