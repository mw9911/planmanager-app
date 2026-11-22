package com.example.planmanagerapp.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll // 💡 업데이트 헬퍼 임포트
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.planmanagerapp.data.local.dao.PlanDao
import com.example.planmanagerapp.data.local.dao.RoutineDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun planDao(): PlanDao
    fun routineDao(): RoutineDao
}

class PlanWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val planDao = entryPoint.planDao()
        val routineDao = entryPoint.routineDao()

        val today = LocalDate.now().toString()
        val uncompletedPlans = planDao.getUncompletedPlansByDate(today)

        val routineGroups = routineDao.getRoutinesStream(today).first()
        val uncompletedRoutines = routineGroups.flatMap { it.items }.filter { !it.isCompleted }

        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "오늘의 계획 (${uncompletedPlans.size}개 남음)",
                    style = TextStyle(color = ColorProvider(Color.Black), fontWeight = FontWeight.Bold)
                )
                uncompletedPlans.take(3).forEach { plan ->
                    Text(
                        text = "· ${plan.title}",
                        style = TextStyle(color = ColorProvider(Color.DarkGray))
                    )
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                Text(
                    text = "오늘의 루틴 (${uncompletedRoutines.size}개 남음)",
                    style = TextStyle(color = ColorProvider(Color.Black), fontWeight = FontWeight.Bold)
                )
                uncompletedRoutines.take(3).forEach { item ->
                    Text(
                        text = "· ${item.title}",
                        style = TextStyle(color = ColorProvider(Color.DarkGray))
                    )
                }
            }
        }
    }

    // 💡 Repository에서 쉽게 호출할 수 있도록 companion object 추가
    companion object {
        suspend fun updateWidget(context: Context) {
            PlanWidget().updateAll(context)
        }
    }
}

class PlanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlanWidget()
}