package com.example.planmanagerapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.planmanagerapp.data.local.dao.RoutineDao
import com.example.planmanagerapp.data.remote.api.PlanApiService
import com.example.planmanagerapp.data.repository.PlanRepository
import com.example.planmanagerapp.data.repository.ResendResult
import com.example.planmanagerapp.data.repository.RoutineRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class NetworkSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncEntryPoint {
        fun routineDao(): RoutineDao
        fun planApiService(): PlanApiService
        fun planRepository(): PlanRepository
        fun routineRepository(): RoutineRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java)
        val routineDao = entryPoint.routineDao()
        val planApiService = entryPoint.planApiService()
        val planRepository = entryPoint.planRepository()
        val routineRepository = entryPoint.routineRepository()

        return try {
            var needsRetry = false
            var refreshPlans = false
            val staleItemIds = mutableSetOf<Long>()

            for (action in routineDao.getAllPendingActions()) {
                when (action.actionType) {
                    "TOGGLE_ITEM" -> when (routineRepository.resendToggle(action)) {
                        ResendResult.DONE -> Unit
                        ResendResult.STALE -> staleItemIds += action.targetId
                        ResendResult.RETRY -> needsRetry = true
                    }
                    "TOGGLE_PLAN" -> when (planRepository.resendToggle(action)) {
                        ResendResult.DONE -> Unit
                        ResendResult.STALE -> refreshPlans = true
                        ResendResult.RETRY -> needsRetry = true
                    }
                    "DELETE_PLAN" -> {
                        planApiService.deletePlan(action.targetId)
                        routineDao.deleteSyncAction(action)
                    }
                }
            }

            // 409·404가 난 목록은 큐를 끝까지 보낸 뒤 한 번만 다시 받는다.
            if (refreshPlans) planRepository.tryRefreshAllPlans()
            if (staleItemIds.isNotEmpty()) routineRepository.tryRefreshRoutinesOf(staleItemIds)

            if (needsRetry) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry() // IOException 등: 남은 큐는 다음 실행에서 다시 보낸다
        }
    }
}