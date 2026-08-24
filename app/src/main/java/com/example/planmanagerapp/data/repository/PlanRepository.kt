package com.example.planmanagerapp.data.repository

import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.entity.PlanEntity
import com.example.planmanagerapp.data.local.entity.toDto
import com.example.planmanagerapp.data.remote.api.PlanApiService
import com.example.planmanagerapp.data.remote.dto.PlanCreateRequest
import com.example.planmanagerapp.data.remote.dto.PlanResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlanRepository @Inject constructor(
    private val planDao: PlanDao,
    private val apiService: PlanApiService
) {
    // 💡 전체 목록 스트림 반환
    fun getAllPlansStream(): Flow<List<PlanResponse>> {
        return planDao.getAllPlansStream().map { entities ->
            entities.map { it.toDto() }
        }
    }

    suspend fun deletePlan(id: Long) {
        planDao.deletePlan(id)
        apiService.deletePlan(id)
    }

    suspend fun refreshAllPlans() {
        val remotePlans = apiService.getAllPlans()
        val entities = remotePlans.map {
            PlanEntity(id = it.id, title = it.title, planDate = it.planDate, isCompleted = it.isCompleted)
        }
        planDao.insertPlans(entities)
    }

    suspend fun createPlan(title: String, date: String) {
        val request = PlanCreateRequest(title, date)
        val response = apiService.createPlan(request)

        val responseBody = response.body()
        if (response.isSuccessful && responseBody != null) {
            val entity = PlanEntity(
                id = responseBody.id,
                title = responseBody.title,
                planDate = responseBody.planDate,
                isCompleted = responseBody.isCompleted
            )
            planDao.insertPlan(entity)
        } else {
            throw RuntimeException("계획 생성 실패: API 응답 오류 (${response.code()})")
        }
    }

    suspend fun togglePlan(id: Long, currentStatus: Boolean) {
        planDao.updateCompletion(id, !currentStatus)
        try {
            apiService.togglePlan(id)
        } catch (e: Exception) {
            planDao.updateCompletion(id, currentStatus)
            throw e
        }
    }
}