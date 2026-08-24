package com.example.planmanagerapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.planmanagerapp.data.remote.dto.RoutineRecordResponse

@Entity(tableName = "routine_records")
data class RoutineRecordEntity(
    @PrimaryKey val id: Long,
    val routineId: Long,
    val title: String,
    val targetDate: String,
    val isCompleted: Boolean
)

fun RoutineRecordEntity.toDto() = RoutineRecordResponse(id, routineId, title, targetDate, isCompleted)