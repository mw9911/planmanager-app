package com.example.planmanagerapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.planmanagerapp.data.remote.dto.PlanResponse

@Entity(tableName = "plans")
data class PlanEntity(
    // 오프라인에서 만든 계획은 로컬에서 id를 발급한다.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val planDate: String,
    val isCompleted: Boolean = false,

    // 서버 반영 여부 (오프라인에서 만들면 false)
    @ColumnInfo(name = "is_synced")
    var isSynced: Boolean = true,

    // 서버에서 마지막으로 받은 version. 토글 요청에 담아 보낸다.
    @ColumnInfo(defaultValue = "0")
    val version: Long = 0
)

// Mapping Extensions
fun PlanEntity.toDto() = PlanResponse(id, title, planDate, isCompleted, version)
fun PlanResponse.toEntity() = PlanEntity(
    id = id,
    title = title,
    planDate = planDate,
    isCompleted = isCompleted,
    version = version
)