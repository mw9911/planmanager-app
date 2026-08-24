package com.example.planmanagerapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp // ⬅️ Hilt가 이 앱에서 DI 코드를 생성하도록 지시합니다.
class PlanManagerApplication : Application()