package com.example.planmanagerapp.data.local.dao

import androidx.room.*
import com.example.planmanagerapp.data.local.entity.PendingSyncActionEntity
import com.example.planmanagerapp.data.local.entity.RoutineGroupLocalEntity
import com.example.planmanagerapp.data.local.entity.RoutineItemLocalEntity
import com.example.planmanagerapp.data.local.entity.RoutineGroupWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routine_groups WHERE date = :date")
    fun getRoutinesStream(date: String): Flow<List<RoutineGroupWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<RoutineGroupLocalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<RoutineItemLocalEntity>)

    // 낙관적 업데이트를 위한 로컬 상태 즉시 변경
    @Query("UPDATE routine_items SET isCompleted = :isCompleted WHERE id = :itemId")
    suspend fun updateItemCompletion(itemId: Long, isCompleted: Boolean)

    @Query("UPDATE routine_items SET isCompleted = :isCompleted, version = :version WHERE id = :itemId")
    suspend fun updateItemCompletionAndVersion(itemId: Long, isCompleted: Boolean, version: Long)

    @Query("UPDATE routine_items SET version = :version WHERE id = :itemId")
    suspend fun updateItemVersion(itemId: Long, version: Long)

    @Query("SELECT version FROM routine_items WHERE id = :itemId")
    suspend fun getItemVersion(itemId: Long): Long?

    // 항목이 속한 그룹의 날짜 (충돌 뒤 어느 날짜를 다시 받을지 정할 때 사용)
    @Query("SELECT date FROM routine_groups WHERE id = (SELECT groupId FROM routine_items WHERE id = :itemId)")
    suspend fun getItemDate(itemId: Long): String?

    @Query("DELETE FROM routine_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM routine_groups WHERE date = :date")
    suspend fun clearGroupsByDate(date: String)

    @Query("DELETE FROM routine_items WHERE groupId IN (SELECT id FROM routine_groups WHERE date = :date)")
    suspend fun clearItemsByDate(date: String)

    @Transaction
    suspend fun refreshRoutinesForDate(date: String, groups: List<RoutineGroupLocalEntity>, items: List<RoutineItemLocalEntity>) {
        clearItemsByDate(date)
        clearGroupsByDate(date)
        insertGroups(groups)
        insertItems(items)
    }

    // 오프라인 동기화 큐
    @Insert
    suspend fun insertSyncAction(action: PendingSyncActionEntity)

    @Query("SELECT * FROM pending_sync_actions")
    suspend fun getAllPendingActions(): List<PendingSyncActionEntity>

    @Delete
    suspend fun deleteSyncAction(action: PendingSyncActionEntity)
}