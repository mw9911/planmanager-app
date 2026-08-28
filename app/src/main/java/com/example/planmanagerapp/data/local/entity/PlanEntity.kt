package com.example.planmanagerapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.planmanagerapp.data.remote.dto.PlanResponse

@Entity(tableName = "plans")
data class PlanEntity(
    // 💡 오프라인 로컬 생성을 지원하기 위해 autoGenerate = true 적용
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val planDate: String,
    val isCompleted: Boolean = false,

    // 💡 Room 컴파일러가 찾지 못했던 오프라인 식별 플래그를 정식 통합
    @ColumnInfo(name = "is_synced")
    var isSynced: Boolean = true
)

// Mapping Extensions
fun PlanEntity.toDto() = PlanResponse(id, title, planDate, isCompleted)
fun PlanResponse.toEntity() = PlanEntity(id = id, title = title, planDate = planDate, isCompleted = isCompleted)