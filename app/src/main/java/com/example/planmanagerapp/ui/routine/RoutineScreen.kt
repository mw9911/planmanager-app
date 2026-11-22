package com.example.planmanagerapp.ui.routine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.planmanagerapp.data.remote.dto.RoutineGroupResponse
import com.example.planmanagerapp.data.remote.dto.RoutineItemResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    viewModel: RoutineViewModel = hiltViewModel(),
    onNavigateToCalendar: () -> Unit // 💡 1. 네비게이션 콜백 파라미터 추가
) {
    val uiState by viewModel.uiState.collectAsState()

    var showGroupDialog by remember { mutableStateOf(false) }
    var groupTitleInput by remember { mutableStateOf("") }
    var groupIntervalInput by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("루틴 관리 - ${uiState.selectedDate}") },
                // 💡 2. 상단 앱바 우측에 달력 이동 아이콘 버튼 추가
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.DateRange, contentDescription = "캘린더 보기")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showGroupDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "그룹 추가")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (uiState.errorMessage != null) {
                Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.groups, key = { it.id }) { group ->
                    RoutineGroupCard(group = group, viewModel = viewModel)
                }
            }
        }
    }
    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("새 루틴 그룹 생성") },
            text = {
                Column {
                    OutlinedTextField(value = groupTitleInput, onValueChange = { groupTitleInput = it }, label = { Text("그룹 이름 (예: 아침 운동)") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = groupIntervalInput,
                        onValueChange = { groupIntervalInput = it.filter { char -> char.isDigit() } },
                        label = { Text("반복 주기 (일)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val interval = groupIntervalInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (groupTitleInput.isNotBlank()) {
                        viewModel.handleIntent(RoutineIntent.CreateGroup(groupTitleInput, interval))
                        showGroupDialog = false
                        groupTitleInput = ""
                        groupIntervalInput = "1"
                    }
                }) { Text("생성") }
            },
            dismissButton = { TextButton(onClick = { showGroupDialog = false }) { Text("취소") } }
        )
    }
}
@Composable
fun RoutineGroupCard(group: RoutineGroupResponse, viewModel: RoutineViewModel) {
    // 항목 추가용 상태
    var showItemDialog by remember { mutableStateOf(false) }
    var itemTitleInput by remember { mutableStateOf("") }

    // 삭제 확인 다이얼로그용 상태 변수 추가
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Long?>(null) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // 그룹 타이틀 및 삭제 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${group.title} (${group.intervalDays}일 주기)", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showDeleteGroupDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "그룹 삭제", tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 자식 항목 리스트 및 개별 항목 삭제 버튼 렌더링
            group.items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        // 💡 핵심 수정: ToggleItem에 item.isCompleted 상태 값을 함께 전달
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = {
                                viewModel.handleIntent(RoutineIntent.ToggleItem(item.id, item.isCompleted))
                            }
                        )
                        Text(
                            text = item.title,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    IconButton(onClick = { itemToDelete = item.id }) {
                        Icon(Icons.Default.Delete, contentDescription = "항목 삭제", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            TextButton(onClick = { showItemDialog = true }, modifier = Modifier.align(Alignment.End)) {
                Text("+ 세부 항목 추가")
            }
        }
    }

    // 그룹 삭제 확인 다이얼로그 UI
    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("그룹 삭제") },
            text = { Text("'${group.title}' 그룹을 정말 삭제하시겠습니까?\n내부에 포함된 모든 세부 항목도 함께 삭제되며 복구할 수 없습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleIntent(RoutineIntent.DeleteGroup(group.id))
                        showDeleteGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) { Text("취소") }
            }
        )
    }

    // 세부 항목 삭제 확인 다이얼로그 UI
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("항목 삭제") },
            text = { Text("해당 루틴 항목을 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleIntent(RoutineIntent.DeleteItem(itemToDelete!!))
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("취소") }
            }
        )
    }

    // 세부 항목 추가 다이얼로그
    if (showItemDialog) {
        AlertDialog(
            onDismissRequest = { showItemDialog = false },
            title = { Text("항목 추가") },
            text = {
                OutlinedTextField(value = itemTitleInput, onValueChange = { itemTitleInput = it }, label = { Text("항목 이름") })
            },
            confirmButton = {
                Button(onClick = {
                    if (itemTitleInput.isNotBlank()) {
                        viewModel.handleIntent(RoutineIntent.AddItem(group.id, itemTitleInput))
                        showItemDialog = false
                        itemTitleInput = ""
                    }
                }) { Text("추가") }
            },
            dismissButton = { TextButton(onClick = { showItemDialog = false }) { Text("취소") } }
        )
    }
}