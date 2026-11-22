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
    // 💡 아래 3개 메서드를 RoutineDao 내부에 추가하십시오.
    @Insert
    suspend fun insertSyncAction(action: PendingSyncActionEntity)

    @Query("SELECT * FROM pending_sync_actions")
    suspend fun getAllPendingActions(): List<PendingSyncActionEntity>

    @Delete
    suspend fun deleteSyncAction(action: PendingSyncActionEntity)
}