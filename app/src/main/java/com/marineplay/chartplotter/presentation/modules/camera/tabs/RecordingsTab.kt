package com.marineplay.chartplotter.presentation.modules.camera.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.modules.camera.CameraTheme
import com.marineplay.chartplotter.presentation.modules.camera.components.RecordingCard
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraPosition
import com.marineplay.chartplotter.presentation.modules.camera.models.EventType
import com.marineplay.chartplotter.presentation.modules.camera.models.Recording
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingsTab() {
    var filterOpen by remember { mutableStateOf(false) }
    var selectedEventType by remember { mutableStateOf<EventType?>(null) }
    var selectedCamera by remember { mutableStateOf<CameraPosition?>(null) }

    // 더미 녹화 기록
    val allRecordings = remember {
        listOf(
            Recording("1", "2026-01-12", "14:23:15", CameraPosition.BOW, "전면", EventType.IMPACT, 120, true),
            Recording("2", "2026-01-12", "12:45:33", CameraPosition.STERN, "후면", EventType.MANUAL, 180, false),
            Recording("3", "2026-01-12", "09:12:08", CameraPosition.PORT, "좌현", EventType.AUTO, 300, false),
            Recording("4", "2026-01-11", "18:34:22", CameraPosition.STARBOARD, "우현", EventType.IMPACT, 120, true),
            Recording("5", "2026-01-11", "16:20:11", CameraPosition.BOW, "전면", EventType.AUTO, 300, false),
            Recording("6", "2026-01-11", "13:55:40", CameraPosition.STERN, "후면", EventType.MANUAL, 240, false),
            Recording("7", "2026-01-10", "15:10:25", CameraPosition.PORT, "좌현", EventType.AUTO, 300, false),
        )
    }

    // 필터 적용
    val filteredRecordings = remember(allRecordings, selectedEventType, selectedCamera) {
        allRecordings.filter { recording ->
            (selectedEventType == null || recording.eventType == selectedEventType) &&
            (selectedCamera == null || recording.camera == selectedCamera)
        }
    }

    // 날짜별 그룹화
    val groupedRecordings = remember(filteredRecordings) {
        filteredRecordings.groupBy { it.date }
    }

    val activeFilterCount = (if (selectedEventType != null) 1 else 0) + (if (selectedCamera != null) 1 else 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraTheme.backgroundColor)
    ) {
        // 상단 헤더
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1B2A))
                .border(1.dp, CameraTheme.borderColor, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "녹화 기록",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Box {
                    Button(
                        onClick = { filterOpen = !filterOpen },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CameraTheme.surfaceColor,
                            contentColor = Color(0xFF1A202C) // 검은색/진한 회색
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("필터", fontSize = 14.sp)
                        }
                    }
                    if (activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .background(CameraTheme.primaryColor, CircleShape)
                                .size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeFilterCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 필터 패널
            if (filterOpen) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CameraTheme.surfaceColor),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "필터 설정",
                                color = CameraTheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { filterOpen = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "닫기",
                                    tint = CameraTheme.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 이벤트 유형 필터
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "이벤트 유형",
                                    color = CameraTheme.textSecondary,
                                    fontSize = 12.sp
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { selectedEventType = null },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedEventType == null) CameraTheme.primaryColor else CameraTheme.surfaceColor,
                                            contentColor = if (selectedEventType == null) Color.White else Color(0xFF1A202C)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("전체", fontSize = 12.sp)
                                    }
                                    EventType.values().forEach { type ->
                                        Button(
                                            onClick = { selectedEventType = type },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedEventType == type) CameraTheme.primaryColor else CameraTheme.surfaceColor,
                                                contentColor = if (selectedEventType == type) Color.White else Color(0xFF1A202C)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(type.label, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // 카메라 필터
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "카메라",
                                    color = CameraTheme.textSecondary,
                                    fontSize = 12.sp
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { selectedCamera = null },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedCamera == null) CameraTheme.primaryColor else CameraTheme.surfaceColor,
                                            contentColor = if (selectedCamera == null) Color.White else Color(0xFF1A202C)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("전체", fontSize = 12.sp)
                                    }
                                    CameraPosition.values().forEach { position ->
                                        Button(
                                            onClick = { selectedCamera = position },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedCamera == position) CameraTheme.primaryColor else CameraTheme.surfaceColor,
                                                contentColor = if (selectedCamera == position) Color.White else Color(0xFF1A202C)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(position.label, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 기록 목록
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (groupedRecordings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = CameraTheme.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "녹화 기록이 없습니다",
                            color = CameraTheme.textSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                groupedRecordings.forEach { (date, recordings) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 날짜 헤더
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = CameraTheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = formatDate(date),
                                color = CameraTheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "(${recordings.size}개)",
                                color = CameraTheme.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // 녹화 항목들
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            recordings.forEach { recording ->
                                RecordingCard(
                                    recording = recording,
                                    onClick = { /* TODO: 재생 */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(dateStr: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: return dateStr
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val target = Calendar.getInstance().apply { time = date }

    return when {
        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "오늘"
        target.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "어제"
        else -> "${target.get(Calendar.MONTH) + 1}월 ${target.get(Calendar.DAY_OF_MONTH)}일"
    }
}

