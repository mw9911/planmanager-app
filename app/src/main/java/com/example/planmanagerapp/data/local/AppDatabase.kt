package com.example.planmanagerapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.dao.RoutineDao
import com.example.planmanagerapp.data.local.entity.PlanEntity
import com.example.planmanagerapp.data.local.entity.RoutineRecordEntity

@Database(
    entities = [PlanEntity::class, RoutineRecordEntity::class], // 💡 반드시 PlanEntity여야 함
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun routineDao(): RoutineDao
}
