package com.example.planmanagerapp.data.repository

import android.content.Context
import android.util.Log
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
import com.example.planmanagerapp.data.remote.dto.ToggleRequest
import com.example.planmanagerapp.ui.widget.PlanWidget
import com.example.planmanagerapp.worker.NetworkSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
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
            PlanEntity(
                id = it.id,
                title = it.title,
                planDate = it.planDate,
                isCompleted = it.isCompleted,
                isSynced = true,
                version = it.version
            )
        }
        planDao.insertPlans(entities)
        PlanWidget.updateWidget(context)
    }

    // 409·404 뒤에 최신 목록을 받는다. 실패해도 호출한 흐름은 그대로 진행한다.
    suspend fun tryRefreshAllPlans() {
        try {
            refreshAllPlans()
        } catch (e: IOException) {
            Log.w("Sync", "계획 목록 갱신 실패: ${e.message}")
        } catch (e: HttpException) {
            Log.w("Sync", "계획 목록 갱신 실패: HTTP ${e.code()}")
        }
    }

    // 인터넷이 연결되면 NetworkSyncWorker가 큐를 다시 보낸다.
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
                    isSynced = true,
                    version = responseBody.version
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
            scheduleNetworkSyncWorker()
        }
    }

    suspend fun togglePlan(id: Long, currentStatus: Boolean) {
        // 오프라인에서 만들어 아직 서버에 없는 계획은 로컬만 바꾼다. 동기화할 때 isCompleted가 함께 올라간다.
        if (planDao.isSynced(id) == false) {
            planDao.updateCompletion(id, !currentStatus)
            PlanWidget.updateWidget(context)
            return
        }
        // 같은 계획의 요청이 아직 끝나지 않았으면 이번 입력은 무시한다. 같은 version이 두 번 가서 409가 나는 것을 막는다.
        if (!togglingIds.add(id)) return
        try {
            processToggle(id, currentStatus)
        } finally {
            togglingIds.remove(id)
        }
    }

    private suspend fun processToggle(id: Long, currentStatus: Boolean) {
        // 1. 로컬 먼저 반영 (낙관적 업데이트)
        planDao.updateCompletion(id, !currentStatus)
        PlanWidget.updateWidget(context)

        // 아직 보내지 못한 같은 계획의 토글이 있으면 두 번 누른 셈이라, 둘 다 없애고 끝낸다.
        planDao.findPendingToggle(id)?.let {
            planDao.deletePlanSyncAction(it)
            return
        }

        val localVersion = planDao.getVersion(id)

        val response = try {
            apiService.togglePlan(id, ToggleRequest(localVersion))
        } catch (e: IOException) {
            // 2. 연결 실패: 그때의 version과 함께 큐에 넣는다.
            planDao.insertPlanSyncAction(
                PendingSyncActionEntity(actionType = "TOGGLE_PLAN", targetId = id, expectedVersion = localVersion)
            )
            scheduleNetworkSyncWorker()
            return
        } catch (e: Exception) {
            revertPlan(id, currentStatus)
            throw e
        }

        // 3. 응답 코드에 따라 로컬을 맞춘다.
        when {
            response.isSuccessful -> {
                response.body()?.let { planDao.updateCompletionAndVersion(id, it.isCompleted, it.version) }
                PlanWidget.updateWidget(context)
            }
            response.code() == 409 -> {
                revertPlan(id, currentStatus)
                tryRefreshAllPlans()
                throw VersionConflictException()
            }
            response.code() == 404 -> {
                planDao.deletePlan(id)
                PlanWidget.updateWidget(context)
                tryRefreshAllPlans()
            }
            else -> {
                revertPlan(id, currentStatus)
                throw HttpException(response)
            }
        }
    }

    /** 큐에 쌓인 TOGGLE_PLAN을 expectedVersion과 함께 다시 보낸다. IOException은 호출한 쪽으로 던진다. */
    suspend fun resendToggle(action: PendingSyncActionEntity): ResendResult {
        val response = apiService.togglePlan(action.targetId, ToggleRequest(action.expectedVersion))
        return when {
            response.isSuccessful -> {
                // 서버가 돌려준 상태로 맞춘다. 보내는 사이에 화면에서 다시 눌렀어도 서버와 같아진다.
                response.body()?.let { planDao.updateCompletionAndVersion(action.targetId, it.isCompleted, it.version) }
                planDao.deletePlanSyncAction(action)
                ResendResult.DONE
            }
            response.code() == 409 || response.code() == 404 -> {
                planDao.deletePlanSyncAction(action)
                ResendResult.STALE
            }
            else -> ResendResult.RETRY
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
            scheduleNetworkSyncWorker()
        }
    }

    private suspend fun revertPlan(id: Long, currentStatus: Boolean) {
        planDao.updateCompletion(id, currentStatus)
        PlanWidget.updateWidget(context)
    }

    companion object {
        // 저장소가 주입될 때마다 새로 만들어지므로, 진행 중인 토글은 프로세스 전체에서 공유한다.
        private val togglingIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    }
}