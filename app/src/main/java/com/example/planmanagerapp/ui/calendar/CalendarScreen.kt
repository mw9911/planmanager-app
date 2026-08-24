package com.example.planmanagerapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.planmanagerapp.data.remote.dto.CalendarItemResponse
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 1. 달력 헤더 (연월 표시 및 이동)
        CalendarHeader(
            currentMonth = uiState.currentMonth,
            onPreviousMonth = { viewModel.handleIntent(CalendarIntent.ChangeMonth(uiState.currentMonth.minusMonths(1))) },
            onNextMonth = { viewModel.handleIntent(CalendarIntent.ChangeMonth(uiState.currentMonth.plusMonths(1))) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 요일 표시 바
        DaysOfWeekHeader()

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 달력 그리드 (데이터 매핑)
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            CalendarGrid(
                currentMonth = uiState.currentMonth,
                items = uiState.items,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // 4. 선택된 날짜의 상세 리스트
        val selectedItems = uiState.items.filter { it.targetDate == selectedDate.toString() }
        SelectedDateItemList(date = selectedDate, items = selectedItems)
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달")
        }
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달")
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (day == "일") Color.Red else if (day == "토") Color.Blue else Color.DarkGray
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    items: List<CalendarItemResponse>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    // 💡 날짜(String)를 Key로 하는 빠른 검색용 Map 생성 (시간 복잡도 O(1))
    val itemsByDate = items.groupBy { it.targetDate }

    val daysInMonth = currentMonth.lengthOfMonth()
    // java.time.DayOfWeek는 월(1) ~ 일(7)이므로, 일(0) ~ 토(6)으로 변환
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1일 이전의 빈 칸(Empty Slot) 렌더링
        items(firstDayOfWeek) {
            Box(modifier = Modifier.aspectRatio(1f))
        }

        // 실제 날짜 렌더링
        items(daysInMonth) { day ->
            val date = currentMonth.atDay(day + 1)
            val dateItems = itemsByDate[date.toString()] ?: emptyList()

            DayCell(
                date = date,
                isToday = date == LocalDate.now(),
                isSelected = date == selectedDate,
                items = dateItems,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    items: List<CalendarItemResponse>,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent
    val textColor = when {
        isSelected -> Color(0xFF1976D2)
        isToday -> Color(0xFF4CAF50)
        date.dayOfWeek.value == 7 -> Color.Red // 일요일
        date.dayOfWeek.value == 6 -> Color.Blue // 토요일
        else -> Color.Black
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 💡 아이템 상태 표시 인디케이터 (Plan = 파랑, Routine = 초록)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            items.take(3).forEach { item ->
                val dotColor = if (item.type == "PLAN") Color(0xFF2196F3) else Color(0xFF4CAF50)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .padding(horizontal = 1.dp)
                        .clip(CircleShape)
                        .background(if (item.isCompleted) dotColor.copy(alpha = 0.3f) else dotColor)
                )
            }
            if (items.size > 3) {
                Text("+", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.padding(start = 1.dp))
            }
        }
    }
}

@Composable
fun SelectedDateItemList(date: LocalDate, items: List<CalendarItemResponse>) {
    Text(
        text = "${date.format(DateTimeFormatter.ofPattern("M월 d일"))} 일정",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("등록된 일정이 없습니다.", color = Color.Gray)
        }
    } else {
        LazyColumn {
            items(items) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val labelColor = if (item.type == "PLAN") Color(0xFF2196F3) else Color(0xFF4CAF50)
                    Box(
                        modifier = Modifier
                            .background(labelColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (item.type == "PLAN") "계획" else "루틴",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        color = if (item.isCompleted) Color.Gray else Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}