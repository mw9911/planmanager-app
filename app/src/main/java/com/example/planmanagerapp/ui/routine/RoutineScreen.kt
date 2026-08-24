package com.example.planmanagerapp.ui.routine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    viewModel: RoutineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showGroupDialog by remember { mutableStateOf(false) }
    var groupTitleInput by remember { mutableStateOf("") }
    var groupIntervalInput by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("루틴 관리 - ${uiState.selectedDate}") })
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
    var showItemDialog by remember { mutableStateOf(false) }
    var itemTitleInput by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "${group.title} (${group.intervalDays}일 주기)", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 자식 항목 리스트 렌더링
            group.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = item.isCompleted, onCheckedChange = { viewModel.handleIntent(RoutineIntent.ToggleItem(item.id)) })
                    Text(text = item.title, textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                }
            }

            TextButton(onClick = { showItemDialog = true }, modifier = Modifier.align(Alignment.End)) {
                Text("+ 세부 항목 추가")
            }
        }
    }

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