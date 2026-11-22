package com.example.planmanagerapp.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.entity.PendingSyncActionEntity
import com.example.planmanagerapp.data.local.entity.PlanEntity
import com.example.planmanagerapp.data.local.entity.toDto
import com.example.planmanagerapp.data.remote.api.PlanApiService
import com.example.planmanagerapp.data.remote.dto.PlanCreateRequest
import com.example.planmanagerapp.data.remote.dto.PlanResponse
import com.example.planmanagerapp.ui.widget.PlanWidget
import com.example.planmanagerapp.worker.NetworkSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class PlanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val planDao: PlanDao,
    private val apiService: PlanApiService
) {
    fun getAllPlansStream(): Flow<List<PlanResponse>> {
        return planDao.getAllPlansStream().map { entities ->
            entities.map { it.toDto() }
        }
    }

    suspend fun refreshAllPlans() {
        val remotePlans = apiService.getAllPlans()
        val entities = remotePlans.map {
            PlanEntity(id = it.id, title = it.title, planDate = it.planDate, isCompleted = it.isCompleted, isSynced = true)
        }
        planDao.insertPlans(entities)
        PlanWidget.updateWidget(context)
    }

    // 💡 재사용을 위한 헬퍼 함수: 인터넷 연결 시 워커 예약
    private fun scheduleNetworkSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<NetworkSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "OfflineSync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun createPlan(title: String, date: String) {
        val request = PlanCreateRequest(title, date)
        try {
            val response = apiService.createPlan(request)
            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                val entity = PlanEntity(
                    id = responseBody.id,
                    title = responseBody.title,
                    planDate = responseBody.planDate,
                    isCompleted = responseBody.isCompleted,
                    isSynced = true
                )
                planDao.insertPlan(entity)
                PlanWidget.updateWidget(context)
            } else {
                throw RuntimeException("계획 생성 실패")
            }
        } catch (e: IOException) {
            val tempEntity = PlanEntity(title = title, planDate = date, isCompleted = false, isSynced = false)
            planDao.insertPlan(tempEntity)
            PlanWidget.updateWidget(context)
            // 💡 오프라인 큐 생성 후 워커 예약
            scheduleNetworkSyncWorker()
        }
    }

    suspend fun togglePlan(id: Long, currentStatus: Boolean) {
        planDao.updateCompletion(id, !currentStatus)
        PlanWidget.updateWidget(context)

        try {
            apiService.togglePlan(id)
        } catch (e: IOException) {
            planDao.insertPlanSyncAction(
                PendingSyncActionEntity(actionType = "TOGGLE_PLAN", targetId = id)
            )
            // 💡 큐 적재 후 워커 예약
            scheduleNetworkSyncWorker()
        } catch (e: HttpException) {
            if (e.code() == 409) {
                planDao.updateCompletion(id, currentStatus)
                PlanWidget.updateWidget(context)
                throw IllegalStateException("데이터가 다른 기기에서 변경되었습니다. 최신화가 필요합니다.", e)
            } else {
                planDao.updateCompletion(id, currentStatus)
                PlanWidget.updateWidget(context)
                throw e
            }
        } catch (e: Exception) {
            planDao.updateCompletion(id, currentStatus)
            PlanWidget.updateWidget(context)
            throw e
        }
    }

    suspend fun deletePlan(id: Long) {
        planDao.deletePlan(id)
        PlanWidget.updateWidget(context)

        try {
            apiService.deletePlan(id)
        } catch (e: IOException) {
            planDao.insertPlanSyncAction(
                PendingSyncActionEntity(actionType = "DELETE_PLAN", targetId = id)
            )
            // 💡 큐 적재 후 워커 예약
            scheduleNetworkSyncWorker()
        }
    }
}