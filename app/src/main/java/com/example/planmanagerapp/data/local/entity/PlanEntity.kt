package com.example.planmanagerapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.planmanagerapp.data.remote.dto.PlanResponse

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: Long, // 서버의 ID를 PK로 사용 (동기화 기준)
    val title: String,
    val planDate: String,
    val isCompleted: Boolean
)

// Mapping Extensions
fun PlanEntity.toDto() = PlanResponse(id, title, planDate, isCompleted)
fun PlanResponse.toEntity() = PlanEntity(id, title, planDate, isCompleted)