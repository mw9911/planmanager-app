package com.example.planmanagerapp.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.planmanagerapp.data.remote.dto.PlanResponse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    onNavigateToCalendar: () -> Unit // 💡 캘린더 화면 이동 콜백 추가
) {
    val uiState by viewModel.uiState.collectAsState()

    // 계획 추가 다이얼로그 상태 관리
    var showAddDialog by remember { mutableStateOf(false) }
    var newPlanTitle by remember { mutableStateOf("") }

    // 날짜 선택기(DatePicker) 상태 관리
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val selectedDate = datePickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
    } ?: LocalDate.now().toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("전체 계획 관리") },
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.DateRange, contentDescription = "캘린더 보기")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "추가")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            if (uiState.errorMessage != null) {
                Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = uiState.plans, key = { plan -> plan.id }) { plan ->
                    PlanItem(
                        plan = plan,
                        onToggle = { viewModel.handleIntent(PlanIntent.TogglePlan(plan.id, plan.isCompleted)) },
                        onDelete = { viewModel.handleIntent(PlanIntent.DeletePlan(plan.id)) }
                    )
                }
            }
        }
    }

    // 💡 계획 생성 다이얼로그
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("새 계획 추가") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlanTitle,
                        onValueChange = { newPlanTitle = it },
                        label = { Text("계획 이름") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        label = { Text("날짜 선택") },
                        enabled = false, // 직접 입력 방지
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPlanTitle.isNotBlank()) {
                        viewModel.handleIntent(PlanIntent.CreatePlan(newPlanTitle, selectedDate))
                        showAddDialog = false
                        newPlanTitle = ""
                    }
                }) { Text("생성") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("취소") }
            }
        )
    }

    // 💡 실제 달력 팝업
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun PlanItem(
    plan: PlanResponse,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(checked = plan.isCompleted, onCheckedChange = { onToggle() })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = plan.title,
                        textDecoration = if (plan.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    // 💡 전체 목록이므로 날짜를 표시해줍니다.
                    Text(text = plan.planDate, fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}