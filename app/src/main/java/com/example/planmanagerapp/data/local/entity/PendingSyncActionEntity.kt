package com.example.planmanagerapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_actions")
data class PendingSyncActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // 예: "TOGGLE_ITEM"
    val targetId: Long,     // 조작한 아이템의 ID
    val expectedVersion: Long? = null // 큐에 넣을 때의 로컬 version (DB 버전 5 이전에 쌓인 항목은 null)
)