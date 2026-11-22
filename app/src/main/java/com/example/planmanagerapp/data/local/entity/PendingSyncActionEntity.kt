package com.example.planmanagerapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_actions")
data class PendingSyncActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // 예: "TOGGLE_ITEM"
    val targetId: Long      // 조작한 아이템의 ID
)