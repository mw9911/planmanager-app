package com.example.planmanagerapp.data.repository

import android.content.Context
import android.util.Log
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
                        RoutineItemResponse(item.id, item.title, item.isCompleted, item.version)
                    }
                )
            }
        }
    }

    suspend fun fetchAndCacheRoutines(date: String) {
        val response = apiService.getRoutineGroups(date)
        if (response.isSuccessful) {
            val remoteGroups = response.body() ?: emptyList()

            val groupEntities = remoteGroups.map { RoutineGroupLocalEntity(it.id, it.title, it.intervalDays, date) }
            val itemEntities = remoteGroups.flatMap { group ->
                group.items.map { RoutineItemLocalEntity(it.id, group.id, it.title, it.isCompleted, it.version) }
            }
            routineDao.refreshRoutinesForDate(date, groupEntities, itemEntities)
            PlanWidget.updateWidget(context)
        }
    }

    suspend fun toggleItem(itemId: Long, currentStatus: Boolean) {
        // 1. 로컬 즉시 반영 (낙관적 업데이트)
        routineDao.updateItemCompletion(itemId, !currentStatus)
        PlanWidget.updateWidget(context)
        val localVersion = routineDao.getItemVersion(itemId)

        val response = try {
            apiService.toggleItem(itemId, ToggleRequest(localVersion))
        } catch (e: IOException) {
            // 2. 연결 실패: 그때의 version과 함께 큐에 넣는다.
            routineDao.insertSyncAction(
                PendingSyncActionEntity(actionType = "TOGGLE_ITEM", targetId = itemId, expectedVersion = localVersion)
            )
            scheduleNetworkSyncWorker()
            return
        } catch (e: Exception) {
            revertItem(itemId, currentStatus)
            throw e
        }

        // 3. 응답 코드에 따라 로컬을 맞춘다.
        when {
            response.isSuccessful -> {
                response.body()?.let { routineDao.updateItemCompletionAndVersion(itemId, it.isCompleted, it.version) }
                PlanWidget.updateWidget(context)
            }
            response.code() == 409 -> {
                revertItem(itemId, currentStatus)
                tryRefreshRoutinesOf(listOf(itemId))
                throw VersionConflictException()
            }
            response.code() == 404 -> {
                val date = routineDao.getItemDate(itemId) // 지우기 전에 다시 받을 날짜를 알아 둔다
                routineDao.deleteItem(itemId)
                PlanWidget.updateWidget(context)
                if (date != null) tryRefreshRoutines(date)
            }
            else -> {
                revertItem(itemId, currentStatus)
                throw HttpException(response)
            }
        }
    }

    /** 큐에 쌓인 TOGGLE_ITEM을 expectedVersion과 함께 다시 보낸다. IOException은 호출한 쪽으로 던진다. */
    suspend fun resendToggle(action: PendingSyncActionEntity): ResendResult {
        val response = apiService.toggleItem(action.targetId, ToggleRequest(action.expectedVersion))
        return when {
            response.isSuccessful -> {
                response.body()?.let { routineDao.updateItemVersion(action.targetId, it.version) }
                routineDao.deleteSyncAction(action)
                ResendResult.DONE
            }
            response.code() == 409 || response.code() == 404 -> {
                routineDao.deleteSyncAction(action)
                ResendResult.STALE
            }
            else -> ResendResult.RETRY
        }
    }

    suspend fun syncPendingActions() {
        val pendingActions = routineDao.getAllPendingActions()
        if (pendingActions.isEmpty()) return

        val staleItemIds = mutableSetOf<Long>()
        for (action in pendingActions) {
            if (action.actionType != "TOGGLE_ITEM") continue
            try {
                if (resendToggle(action) == ResendResult.STALE) staleItemIds += action.targetId
            } catch (e: Exception) {
                // IOException 등: 큐에 남겨 두고 다음 항목으로 넘어간다.
                Log.e("Sync", "동기화 실패: ${e.message}")
            }
        }
        tryRefreshRoutinesOf(staleItemIds)
    }

    // 항목들이 속한 날짜의 루틴을 다시 받는다. 실패해도 호출한 흐름은 그대로 진행한다.
    suspend fun tryRefreshRoutinesOf(itemIds: Collection<Long>) {
        itemIds.mapNotNull { routineDao.getItemDate(it) }
            .distinct()
            .forEach { tryRefreshRoutines(it) }
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

    private suspend fun tryRefreshRoutines(date: String) {
        try {
            fetchAndCacheRoutines(date)
        } catch (e: IOException) {
            Log.w("Sync", "루틴 목록 갱신 실패: ${e.message}")
        }
    }

    private suspend fun revertItem(itemId: Long, currentStatus: Boolean) {
        routineDao.updateItemCompletion(itemId, currentStatus)
        PlanWidget.updateWidget(context)
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
}