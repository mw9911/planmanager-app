package com.example.planmanagerapp.data.repository

import com.example.planmanagerapp.data.remote.api.RoutineApiService
import com.example.planmanagerapp.data.remote.dto.*
import javax.inject.Inject

class RoutineRepository @Inject constructor(
    private val apiService: RoutineApiService
) {
    suspend fun getRoutineGroups(date: String): List<RoutineGroupResponse> {
        return apiService.getRoutineGroups(date)
    }

    suspend fun createGroup(title: String, intervalDays: Int) {
        val response = apiService.createGroup(RoutineGroupCreateRequest(title, intervalDays))
        if (!response.isSuccessful) {
            val error = response.errorBody()?.string() ?: "응답 없음"
            throw RuntimeException("그룹 생성 실패: $error")
        }
    }

    suspend fun addItem(groupId: Long, title: String) {
        val response = apiService.addItem(groupId, RoutineItemCreateRequest(title))
        if (!response.isSuccessful) {
            val error = response.errorBody()?.string() ?: "응답 없음"
            throw RuntimeException("항목 추가 실패: $error")
        }
    }

    suspend fun toggleItem(itemId: Long) {
        val response = apiService.toggleItem(itemId)
        if (!response.isSuccessful) throw RuntimeException("상태 변경 실패")
    }
}