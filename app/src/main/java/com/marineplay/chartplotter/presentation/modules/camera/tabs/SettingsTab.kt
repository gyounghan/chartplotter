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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.modules.camera.CameraTheme

@Composable
fun SettingsTab() {
    var autoRecording by remember { mutableStateOf(true) }
    var eventDuration by remember { mutableStateOf(120) } // seconds
    var autoDeleteDays by remember { mutableStateOf(30) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // 저장 공간 데이터 (GB)
    val totalStorage = 500
    val usedStorage = 342
    val freeStorage = totalStorage - usedStorage
    val usagePercentage = (usedStorage.toFloat() / totalStorage * 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraTheme.backgroundColor)
    ) {
        // 상단 헤더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1B2A))
                .border(1.dp, CameraTheme.borderColor, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "설정",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // 스크롤 가능한 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 녹화 설정
            RecordingSettingsSection(
                autoRecording = autoRecording,
                eventDuration = eventDuration,
                onAutoRecordingChanged = { autoRecording = it },
                onEventDurationChanged = { eventDuration = it }
            )

            // 저장 공간 관리
            StorageManagementSection(
                totalStorage = totalStorage,
                usedStorage = usedStorage,
                freeStorage = freeStorage,
                usagePercentage = usagePercentage,
                autoDeleteDays = autoDeleteDays,
                onAutoDeleteDaysChanged = { autoDeleteDays = it },
                onManualDeleteClick = { /* TODO: 수동 삭제 로직 */ }
            )

            // 시스템 관리
            SystemManagementSection(
                showResetConfirm = showResetConfirm,
                onShowResetConfirmChanged = { showResetConfirm = it },
                onResetConfirm = { /* TODO: 초기화 로직 */ }
            )

            // 시스템 정보
            SystemInfoSection()
        }
    }
}

@Composable
private fun RecordingSettingsSection(
    autoRecording: Boolean,
    eventDuration: Int,
    onAutoRecordingChanged: (Boolean) -> Unit,
    onEventDurationChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CameraTheme.surfaceColor),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "녹화 설정",
                color = CameraTheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // 자동 녹화
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "자동 녹화",
                        color = CameraTheme.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "시스템 시작 시 자동으로 녹화를 시작합니다",
                        color = CameraTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = autoRecording,
                    onCheckedChange = onAutoRecordingChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CameraTheme.primaryColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = CameraTheme.borderColor
                    )
                )
            }

            Divider(color = CameraTheme.borderColor, thickness = 1.dp)

            // 이벤트 녹화 길이
            Column {
                Text(
                    text = "이벤트 녹화 길이",
                    color = CameraTheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "충격 또는 수동 이벤트 발생 시 저장할 영상 길이",
                    color = CameraTheme.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(60, 120, 180, 300).forEach { duration ->
                        Button(
                            onClick = { onEventDurationChanged(duration) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (eventDuration == duration) {
                                    CameraTheme.primaryColor
                                } else {
                                    CameraTheme.borderColor
                                },
                                contentColor = if (eventDuration == duration) {
                                    Color.White
                                } else {
                                    Color(0xFF1A202C) // 검은색/진한 회색
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${duration / 60}분",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageManagementSection(
    totalStorage: Int,
    usedStorage: Int,
    freeStorage: Int,
    usagePercentage: Float,
    autoDeleteDays: Int,
    onAutoDeleteDaysChanged: (Int) -> Unit,
    onManualDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CameraTheme.surfaceColor),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "저장 공간",
                color = CameraTheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // 저장 공간 시각화
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 원형 프로그레스 (파이 차트 대체)
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 배경 원
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.size(160.dp),
                        color = CameraTheme.borderColor,
                        strokeWidth = 16.dp
                    )
                    // 사용량 원
                    CircularProgressIndicator(
                        progress = usagePercentage / 100f,
                        modifier = Modifier.size(160.dp),
                        color = CameraTheme.primaryColor,
                        strokeWidth = 16.dp
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${String.format("%.1f", usagePercentage)}%",
                            color = CameraTheme.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "사용 중",
                            color = CameraTheme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${usedStorage}GB / ${totalStorage}GB",
                        color = CameraTheme.textSecondary,
                        fontSize = 14.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(CameraTheme.primaryColor, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "사용됨: ${usedStorage}GB",
                            color = CameraTheme.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(CameraTheme.borderColor, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "남은 공간: ${freeStorage}GB",
                            color = CameraTheme.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Divider(color = CameraTheme.borderColor, thickness = 1.dp)

            // 자동 삭제 설정
            Column {
                Text(
                    text = "오래된 영상 자동 삭제",
                    color = CameraTheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "잠금되지 않은 영상은 설정한 기간이 지나면 자동으로 삭제됩니다",
                    color = CameraTheme.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(7, 14, 30, 60).forEach { days ->
                        Button(
                            onClick = { onAutoDeleteDaysChanged(days) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (autoDeleteDays == days) {
                                    CameraTheme.primaryColor
                                } else {
                                    CameraTheme.borderColor
                                },
                                contentColor = if (autoDeleteDays == days) {
                                    Color.White
                                } else {
                                    Color(0xFF1A202C) // 검은색/진한 회색
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${days}일",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Divider(color = CameraTheme.borderColor, thickness = 1.dp)

            // 저장 공간 관리 버튼
            Button(
                onClick = onManualDeleteClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CameraTheme.borderColor,
                    contentColor = Color(0xFF1A202C) // 검은색/진한 회색
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "오래된 영상 수동 삭제",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SystemManagementSection(
    showResetConfirm: Boolean,
    onShowResetConfirmChanged: (Boolean) -> Unit,
    onResetConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CameraTheme.surfaceColor),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.errorColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CameraTheme.errorColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "주의 필요",
                        color = CameraTheme.errorColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "아래 작업은 시스템에 영구적인 영향을 미칩니다",
                        color = CameraTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (showResetConfirm) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CameraTheme.backgroundColor),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.errorColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "모든 설정이 초기화되고 잠금되지 않은 모든 영상이 삭제됩니다. 계속하시겠습니까?",
                            color = CameraTheme.textPrimary,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    onResetConfirm()
                                    onShowResetConfirmChanged(false)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CameraTheme.errorColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "초기화 진행",
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = { onShowResetConfirmChanged(false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CameraTheme.borderColor,
                                    contentColor = Color(0xFF1A202C) // 검은색/진한 회색
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "취소",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { onShowResetConfirmChanged(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CameraTheme.borderColor,
                        contentColor = Color(0xFF1A202C) // 검은색/진한 회색
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.errorColor.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "시스템 초기화",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemInfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CameraTheme.surfaceColor),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "시스템 정보",
                color = CameraTheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            SystemInfoRow(label = "펌웨어 버전", value = "v2.4.1")
            SystemInfoRow(label = "하드웨어 모델", value = "MB-4000 Pro")
            SystemInfoRow(label = "일련번호", value = "MB4K-2026-01-A4F2")
        }
    }
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = CameraTheme.textSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = CameraTheme.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
