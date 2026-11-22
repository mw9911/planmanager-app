package com.example.planmanagerapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PlanManagerApplication : Application(), Configuration.Provider {

    // 💡 1. Hilt가 자동으로 만들어주는 WorkerFactory를 주입받습니다.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 💡 2. WorkManager가 초기화될 때 기본 팩토리 대신 Hilt 팩토리를 사용하도록 설정합니다.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}