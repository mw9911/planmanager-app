package com.example.planmanagerapp.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.planmanagerapp.data.local.dao.RoutineDao
import com.example.planmanagerapp.data.local.entity.PendingSyncActionEntity
import com.example.planmanagerapp.data.local.entity.RoutineGroupLocalEntity
import com.example.planmanagerapp.data.local.entity.RoutineItemLocalEntity
import com.example.planmanagerapp.data.remote.api.RoutineApiService
import com.example.planmanagerapp.data.remote.dto.*
import com.example.planmanagerapp.ui.widget.PlanWidget
import com.example.planmanagerapp.worker.NetworkSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class RoutineRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: RoutineApiService,
    private val routineDao: RoutineDao
) {
    fun getRoutinesStream(date: String): Flow<List<RoutineGroupResponse>> {
        return routineDao.getRoutinesStream(date).map { entities ->
            entities.map { entity ->
                RoutineGroupResponse(
                    id = entity.group.id,
                    title = entity.group.title,
                    intervalDays = entity.group.intervalDays,
                    items = entity.items.map { item ->
                        RoutineItemResponse(item.id, item.title, item.isCompleted)
                    }
                )
            }
        }
    }

    suspend fun fetchAndCacheRoutines(date: String) {
        // 💡 1. Response 객체로 받아 성공 여부 확인 후 body() 추출
        val response = apiService.getRoutineGroups(date)
        if (response.isSuccessful) {
            val remoteGroups = response.body() ?: emptyList()

            val groupEntities = remoteGroups.map { RoutineGroupLocalEntity(it.id, it.title, it.intervalDays, date) }
            val itemEntities = remoteGroups.flatMap { group ->
                group.items.map { RoutineItemLocalEntity(it.id, group.id, it.title, it.isCompleted) }
            }
            routineDao.refreshRoutinesForDate(date, groupEntities, itemEntities)
            PlanWidget.updateWidget(context)
        }
    }

    suspend fun toggleItem(itemId: Long, currentStatus: Boolean) {
        // 1. 로컬 즉시 반영 (낙관적 업데이트)
        routineDao.updateItemCompletion(itemId, !currentStatus)
        PlanWidget.updateWidget(context)

        try {
            apiService.toggleItem(itemId)
        } catch (e: IOException) {
            // 2. 물리적 네트워크 단절 시: 오프라인 큐에 저장
            routineDao.insertSyncAction(
                PendingSyncActionEntity(actionType = "TOGGLE_ITEM", targetId = itemId)
            )

            // 💡 추가됨: 인터넷 연결 시 즉각 작동하도록 WorkManager 예약
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
        } catch (e: HttpException) {
            // 3. 서버에서 409 Conflict 발생 시
            if (e.code() == 409) {
                routineDao.updateItemCompletion(itemId, currentStatus)
                PlanWidget.updateWidget(context)
                throw IllegalStateException("데이터가 다른 기기에서 변경되었습니다. 최신화가 필요합니다.", e)
            } else {
                routineDao.updateItemCompletion(itemId, currentStatus)
                PlanWidget.updateWidget(context)
                throw e
            }
        } catch (e: Exception) {
            // 4. 기타 알 수 없는 에러
            routineDao.updateItemCompletion(itemId, currentStatus)
            PlanWidget.updateWidget(context)
            throw e
        }
    }

    // 💡 추가됨: 워커가 백그라운드에서 호출할 실제 동기화 로직
    suspend fun syncPendingActions() {
        val pendingActions = routineDao.getAllPendingActions()
        if (pendingActions.isEmpty()) return

        for (action in pendingActions) {
            try {
                if (action.actionType == "TOGGLE_ITEM") {
                    apiService.toggleItem(action.targetId)
                    routineDao.deleteSyncAction(action) // 성공 시 큐에서 제거
                }
            } catch (e: Exception) {
                android.util.Log.e("Sync", "동기화 실패: ${e.message}")
            }
        }
    }

    suspend fun createGroup(title: String, intervalDays: Int, date: String) {
        apiService.createGroup(RoutineGroupCreateRequest(title, intervalDays))
        fetchAndCacheRoutines(date)
    }

    suspend fun addItem(groupId: Long, title: String, date: String) {
        apiService.addItem(groupId, RoutineItemCreateRequest(title))
        fetchAndCacheRoutines(date)
    }

    suspend fun deleteGroup(groupId: Long, date: String) {
        apiService.deleteGroup(groupId)
        fetchAndCacheRoutines(date)
    }

    suspend fun deleteItem(itemId: Long, date: String) {
        apiService.deleteItem(itemId)
        fetchAndCacheRoutines(date)
    }
}