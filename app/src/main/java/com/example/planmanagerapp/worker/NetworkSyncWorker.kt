package com.example.planmanagerapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.planmanagerapp.data.local.dao.RoutineDao
import com.example.planmanagerapp.data.remote.api.PlanApiService
import com.example.planmanagerapp.data.remote.api.RoutineApiService
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
        fun routineApiService(): RoutineApiService
        fun routineDao(): RoutineDao
        fun planApiService(): PlanApiService
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java)
        val apiService = entryPoint.routineApiService()
        val routineDao = entryPoint.routineDao()
        val planApiService = entryPoint.planApiService()
        return try {
            val pendingActions = routineDao.getAllPendingActions()

            for (action in pendingActions) {
                when (action.actionType) {
                    "TOGGLE_ITEM" -> { // 루틴 처리
                        if (apiService.toggleItem(action.targetId).isSuccessful) {
                            routineDao.deleteSyncAction(action)
                        }
                    }
                    // 💡 Plan 토글/삭제 추가 (이 코드를 넣기 위해 EntryPoint에 PlanApiService도 주입해야 합니다)
                    "TOGGLE_PLAN" -> {
                        planApiService.togglePlan(action.targetId) // 예외 처리는 생략(실패 시 큐에 남음)
                        routineDao.deleteSyncAction(action)
                    }
                    "DELETE_PLAN" -> {
                        planApiService.deletePlan(action.targetId)
                        routineDao.deleteSyncAction(action)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry() // 다시 오프라인이 되면 나중에 재시도
        }
    }
}