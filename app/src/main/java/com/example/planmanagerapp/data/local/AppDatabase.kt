package com.example.planmanagerapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.dao.RoutineDao
import com.example.planmanagerapp.data.local.entity.PendingSyncActionEntity
import com.example.planmanagerapp.data.local.entity.PlanEntity
import com.example.planmanagerapp.data.local.entity.RoutineGroupLocalEntity
import com.example.planmanagerapp.data.local.entity.RoutineItemLocalEntity
import com.example.planmanagerapp.data.local.entity.RoutineRecordEntity

@Database(
    entities = [
        PlanEntity::class,
        RoutineGroupLocalEntity::class,
        RoutineItemLocalEntity::class,
        PendingSyncActionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun routineDao(): RoutineDao

    companion object {
        // 기존 행은 그대로 두고 컬럼만 추가한다.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plans ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routine_items ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_sync_actions ADD COLUMN expectedVersion INTEGER")
            }
        }
    }
}