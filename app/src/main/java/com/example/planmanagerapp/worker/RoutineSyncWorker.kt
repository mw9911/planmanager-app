package com.example.planmanagerapp.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.planmanagerapp.data.repository.RoutineRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RoutineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // 💡 핵심: WorkManager에서 Hilt 의존성을 끌어다 쓰기 위한 진입점(EntryPoint) 정의
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun routineRepository(): RoutineRepository
    }

    override suspend fun doWork(): Result {
        return try {
            // EntryPoint를 통해 Hilt 컨테이너에서 Repository 객체 추출
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            )
            val repository = entryPoint.routineRepository()

            // 💡 수정됨: 구버전 syncRoutines 대신, 오늘 날짜 기준 최신 레코드를 DB에 동기화
            val today = LocalDate.now().toString()
            repository.fetchAndCacheRoutines(today)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// 매일 자정에 백그라운드 동기화를 예약하는 함수 (MainActivity에서 호출됨)
fun scheduleMidnightSync(context: Context) {
    val currentDate = Calendar.getInstance()
    val dueDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(currentDate)) {
            add(Calendar.HOUR_OF_DAY, 24)
        }
    }
    val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED) // 인터넷 연결 시에만 작동
        .build()

    val syncWorkRequest = PeriodicWorkRequestBuilder<RoutineSyncWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "MidnightRoutineSync",
        ExistingPeriodicWorkPolicy.KEEP,
        syncWorkRequest
    )
}
fun scheduleDailyReminder(context: Context) {
    // 💡 주기적 작업 요청 (예: 12시간마다 미완료 항목 체크)
    val reminderWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
        .build()

    // 동일한 작업이 중복 등록되지 않도록 KEEP 정책 사용
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "DailyPlanReminder",
        ExistingPeriodicWorkPolicy.KEEP,
        reminderWorkRequest
    )
}