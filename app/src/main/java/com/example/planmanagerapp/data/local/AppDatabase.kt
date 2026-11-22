package com.example.planmanagerapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.dao.RoutineDao
import com.example.planmanagerapp.data.local.entity.PendingSyncActionEntity
import com.example.planmanagerapp.data.local.entity.PlanEntity
import com.example.planmanagerapp.data.local.entity.RoutineGroupLocalEntity
import com.example.planmanagerapp.data.local.entity.RoutineItemLocalEntity
import com.example.planmanagerapp.data.local.entity.RoutineRecordEntity

@Database(
    entities = [
        PlanEntity::class, // 기존 계획 엔티티
        RoutineGroupLocalEntity::class, // 💡 새로 추가된 루틴 그룹 엔티티
        RoutineItemLocalEntity::class,   // 💡 새로 추가된 루틴 세부 항목 엔티티
        PendingSyncActionEntity::class
    ],
    version = 4, // 💡 스키마가 변경되었으므로 기존 1에서 2로 올려야 합니다.
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun routineDao(): RoutineDao
}