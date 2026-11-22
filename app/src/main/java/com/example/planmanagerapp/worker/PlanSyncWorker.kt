// app/src/.../worker/PlanSyncWorker.kt (새로 만들기)
package com.example.planmanagerapp.worker

import android.content.Context
import androidx.work.*
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.remote.api.PlanApiService
import com.example.planmanagerapp.data.remote.dto.PlanSyncRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class PlanSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun planDao(): PlanDao
        fun planApiService(): PlanApiService
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(applicationContext, SyncWorkerEntryPoint::class.java)
            val dao = entryPoint.planDao()
            val api = entryPoint.planApiService()

            // 1. 아직 서버로 안 넘어간 데이터 조회
            val unsyncedPlans = dao.getUnsyncedPlans()
            if (unsyncedPlans.isEmpty()) return Result.success()

            // 2. 서버 통신 규격(DTO)에 맞춰 변환 후 전송 (API 인터페이스에 syncPlansSync(List) 추가 필요)
            val syncRequests = unsyncedPlans.map { entity ->
                PlanSyncRequest(
                    title = entity.title,
                    planDate = entity.planDate,
                    isCompleted = entity.isCompleted
                )
            }
            // 3. 동기식 호출(execute)로 서버에 밀어넣기
            val response = api.syncPlansSync(syncRequests).execute()

            if (response.isSuccessful) {
                // 4. 성공 시 로컬 DB 플래그 업데이트
                dao.markAsSynced(unsyncedPlans.map { it.id })
                Result.success()
            } else {
                Result.retry() // 서버 에러 시 다음 네트워크 복구 때 재시도
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}