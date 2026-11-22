package com.example.planmanagerapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.planmanagerapp.ui.auth.AuthScreen
import com.example.planmanagerapp.ui.auth.AuthViewModel
import com.example.planmanagerapp.ui.plan.PlanScreen
import com.example.planmanagerapp.ui.plan.PlanViewModel
import com.example.planmanagerapp.ui.routine.RoutineScreen
import com.example.planmanagerapp.ui.calendar.CalendarScreen
import com.example.planmanagerapp.worker.scheduleMidnightSync
import com.example.planmanagerapp.worker.scheduleDailyReminder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        askNotificationPermission()
        scheduleMidnightSync(this)
        scheduleDailyReminder(this)

        // 💡 기존의 scheduleOfflineSyncWorker(this) 호출부 및 함수 블록 삭제 완료

        setContent {
            MaterialTheme {
                PlanManagerApp()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SYNC_CHANNEL_ID",
                "루틴 동기화 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "자정 루틴 초기화 결과를 알려줍니다." }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun PlanManagerApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    if (authState.isCheckingAuth) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in listOf("routine", "plan")

    val startDest = if (authState.isAuthenticated) "routine" else "auth"

    androidx.compose.runtime.LaunchedEffect(Unit) {
        authViewModel.sessionExpiredEvent.collect {
            authViewModel.resetState()
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentDestination?.route != "auth") {
                Button(onClick = {
                    authViewModel.logout(onLogoutComplete = {
                        navController.navigate("auth") {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    })
                }) {
                    Text("로그아웃")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "루틴") },
                        label = { Text("루틴") },
                        selected = currentDestination?.hierarchy?.any { it.route == "routine" } == true,
                        onClick = {
                            navController.navigate("routine") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "계획") },
                        label = { Text("계획") },
                        selected = currentDestination?.hierarchy?.any { it.route == "plan" } == true,
                        onClick = {
                            navController.navigate("plan") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("auth") {
                AuthScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate("routine") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            composable("routine") {
                RoutineScreen(
                    onNavigateToCalendar = {
                        navController.navigate("calendar") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("plan") {
                val planViewModel: PlanViewModel = hiltViewModel()
                PlanScreen(
                    viewModel = planViewModel,
                    onNavigateToCalendar = {
                        navController.navigate("calendar")
                    }
                )
            }
            composable("calendar") {
                CalendarScreen()
            }
        }
    }
}