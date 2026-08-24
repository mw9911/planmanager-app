package com.example.planmanagerapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.planmanagerapp.data.local.entity.RoutineRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_records WHERE targetDate = :date")
    fun getRoutineRecordsStream(date: String): Flow<List<RoutineRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<RoutineRecordEntity>)

    @Query("UPDATE routine_records SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletion(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM routine_records WHERE routineId = :routineId")
    suspend fun deleteRecordsByRoutineId(routineId: Long)
}