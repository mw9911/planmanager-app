package com.example.planmanagerapp.data.local.entity

import androidx.room.*

@Entity(tableName = "routine_groups")
data class RoutineGroupLocalEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val intervalDays: Int,
    val date: String // 날짜별 조회를 위한 기준값
)

@Entity(tableName = "routine_items")
data class RoutineItemLocalEntity(
    @PrimaryKey val id: Long,
    val groupId: Long,
    val title: String,
    val isCompleted: Boolean,
    @ColumnInfo(defaultValue = "0") val version: Long = 0
)

// 1:N 관계를 매핑하는 조인 클래스
data class RoutineGroupWithItems(
    @Embedded val group: RoutineGroupLocalEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val items: List<RoutineItemLocalEntity>
)