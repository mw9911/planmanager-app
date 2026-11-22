package com.example.planmanagerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.planmanagerapp.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

// 💡 삭제된 Plan 임포트 제거 완료

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans WHERE planDate = :date")
    fun getPlansStream(date: String): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<PlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity)

    @Query("UPDATE plans SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletion(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Query("SELECT * FROM plans ORDER BY planDate ASC")
    fun getAllPlansStream(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE is_synced = 0")
    suspend fun getUnsyncedPlans(): List<PlanEntity>

    @Query("UPDATE plans SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    // 💡 에러 해결: ReminderWorker를 위한 미완료 조회 쿼리 신규 추가
    @Query("SELECT * FROM plans WHERE isCompleted = 0 AND planDate = :date")
    suspend fun getUncompletedPlansByDate(date: String): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanSyncAction(action: com.example.planmanagerapp.data.local.entity.PendingSyncActionEntity)
}