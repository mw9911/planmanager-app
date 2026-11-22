package com.example.planmanagerapp.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent // 💡 알림 터치 시 앱 이동을 위한 임포트
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.dao.RoutineDao // 💡 루틴 DAO 임포트
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first // 💡 Flow 단발성 조회를 위한 임포트
import kotlinx.coroutines.withContext
import java.time.LocalDate

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val planDao: PlanDao, // RoomDB 의존성 주입[cite: 38]
    private val routineDao: RoutineDao // 💡 1. 루틴 데이터 조회를 위한 DAO 추가 주입
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().toString()

            // 1. RoomDB에서 오늘 날짜의 미완료 계획 목록 조회[cite: 38]
            val uncompletedPlans = planDao.getUncompletedPlansByDate(today)
            val planCount = uncompletedPlans.size

            // 💡 2. RoomDB에서 오늘 날짜의 미완료 루틴 항목 개수 집계
            // worker는 단발성 실행이므로 Flow를 계속 구독하지 않고 first()로 첫 스냅샷만 가져옵니다.
            val routineGroups = routineDao.getRoutinesStream(today).first()
            val routineCount = routineGroups.sumOf { group ->
                group.items.count { !it.isCompleted }
            }

            // 3. 미완료 항목이 하나라도 존재할 경우에만 OS 알림 발생[cite: 38]
            if (planCount > 0 || routineCount > 0) {
                showNotification(planCount, routineCount)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry() // 일시적 에러 발생 시 재시도 스케줄링[cite: 38]
        }
    }

    private fun showNotification(planCount: Int, routineCount: Int) {
        val channelId = "plan_reminder_channel"
        val notificationId = 1001
        val notificationManager = NotificationManagerCompat.from(appContext)

        // Android 13 (API 33) 이상 알림 권한 팩트 체크[cite: 38]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return // 권한이 없으면 알림 로직 강제 중단[cite: 38]
        }

        // Android 8.0 (API 26) 이상 채널 생성 필수[cite: 38]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "마감 알림", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // 💡 4. 데이터 기반 맞춤형 동적 메시지 생성
        val message = buildString {
            append("오늘 달성해야 할 ")
            if (planCount > 0) append("계획 ${planCount}개")
            if (planCount > 0 && routineCount > 0) append("와 ")
            if (routineCount > 0) append("루틴 ${routineCount}개")
            append("가 남아있습니다.")
        }

        // 💡 5. 사용자가 알림을 터치했을 때 앱(MainActivity)을 열도록 PendingIntent 연결
        val intent = Intent(appContext, com.example.planmanagerapp.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PlanManager 마감 알림")
            .setContentText(message) // 💡 생성된 동적 메시지 맵핑
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // 💡 클릭 이벤트 부여
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}