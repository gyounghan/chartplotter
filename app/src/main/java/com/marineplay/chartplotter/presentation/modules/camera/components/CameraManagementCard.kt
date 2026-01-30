package com.marineplay.chartplotter.presentation.modules.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.modules.camera.CameraTheme
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraInfo
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraStatus

@Composable
fun CameraManagementCard(
    camera: CameraInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CameraTheme.surfaceColor
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (camera.status == CameraStatus.CONNECTED) CameraTheme.borderColor else CameraTheme.errorColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 카메라 정보
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (camera.status == CameraStatus.CONNECTED) {
                                    CameraTheme.successColor.copy(alpha = 0.2f)
                                } else {
                                    CameraTheme.errorColor.copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (camera.status == CameraStatus.CONNECTED) CameraTheme.successColor else CameraTheme.errorColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${camera.label} 카메라",
                                color = CameraTheme.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (camera.status == CameraStatus.CONNECTED) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.Error
                                    },
                                    contentDescription = null,
                                    tint = if (camera.status == CameraStatus.CONNECTED) CameraTheme.successColor else CameraTheme.errorColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (camera.status == CameraStatus.CONNECTED) "정상" else "끊김",
                                    color = if (camera.status == CameraStatus.CONNECTED) CameraTheme.successColor else CameraTheme.errorColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "위치: ${camera.position.name}",
                            color = CameraTheme.textSecondary,
                            fontSize = 14.sp
                        )
                        if (camera.status == CameraStatus.DISCONNECTED && camera.lastConnected != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "마지막 연결: ${camera.lastConnected}",
                                color = CameraTheme.errorColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 야간 모드 상태
                if (camera.status == CameraStatus.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (camera.nightMode) {
                                    Color(0xFF3B82F6).copy(alpha = 0.2f)
                                } else {
                                    CameraTheme.surfaceColor
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (camera.nightMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (camera.nightMode) Color(0xFF3B82F6) else CameraTheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (camera.nightMode) "야간 모드" else "주간 모드",
                                color = if (camera.nightMode) Color(0xFF3B82F6) else CameraTheme.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 상세 정보
            if (camera.status == CameraStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = CameraTheme.borderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "해상도",
                            color = CameraTheme.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = camera.resolution,
                            color = CameraTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column {
                        Text(
                            text = "프레임",
                            color = CameraTheme.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${camera.fps} FPS",
                            color = CameraTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 재연결 버튼
            if (camera.status == CameraStatus.DISCONNECTED) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = CameraTheme.borderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* TODO: 재연결 로직 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CameraTheme.primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "재연결 시도",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

