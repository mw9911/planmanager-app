package com.example.planmanagerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.planmanagerapp.data.local.entity.PendingSyncActionEntity
import com.example.planmanagerapp.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

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

    // ReminderWorker에서 사용
    @Query("SELECT * FROM plans WHERE isCompleted = 0 AND planDate = :date")
    suspend fun getUncompletedPlansByDate(date: String): List<PlanEntity>

    @Query("SELECT version FROM plans WHERE id = :id")
    suspend fun getVersion(id: Long): Long?

    @Query("UPDATE plans SET isCompleted = :isCompleted, version = :version WHERE id = :id")
    suspend fun updateCompletionAndVersion(id: Long, isCompleted: Boolean, version: Long)

    @Query("UPDATE plans SET version = :version WHERE id = :id")
    suspend fun updateVersion(id: Long, version: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanSyncAction(action: PendingSyncActionEntity)

    @Delete
    suspend fun deletePlanSyncAction(action: PendingSyncActionEntity)
}