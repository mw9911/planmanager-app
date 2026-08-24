package com.example.planmanagerapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.dao.RoutineDao
import com.example.planmanagerapp.data.local.entity.PlanEntity
import com.example.planmanagerapp.data.local.entity.RoutineRecordEntity

@Database(
    entities = [PlanEntity::class, RoutineRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun routineDao(): RoutineDao
}
// 💡 문제의 원인이었던 하단의 @Provides 함수를 완전히 삭제했습니다.