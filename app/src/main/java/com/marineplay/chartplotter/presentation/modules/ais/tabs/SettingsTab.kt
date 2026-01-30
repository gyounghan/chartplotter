package com.marineplay.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.modules.ais.AISTheme
import com.marineplay.chartplotter.presentation.modules.ais.components.ToggleSetting
import com.marineplay.chartplotter.presentation.modules.ais.data.MockData

/**
 * 설정 탭
 */
@Composable
fun SettingsTab() {
    val settings by MockData.settings.collectAsState()
    var localSettings by remember { mutableStateOf(settings) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            // 헤더
            Column {
                Text(
                    text = "설정",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AISTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "경보 기준 및 표시 옵션 관리",
                    fontSize = 14.sp,
                    color = AISTheme.textSecondary
                )
            }
        }

        item {
            // CPA/TCPA 경보 기준
            SettingsSection(
                title = "경보 기준",
                icon = Icons.Default.Warning,
                iconColor = AISTheme.danger
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // CPA 설정
                    Column {
                        Text(
                            text = "CPA (최근접점 거리)",
                            fontSize = 14.sp,
                            color = AISTheme.textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ThresholdInput(
                                label = "즉시 위험 (NM 이하)",
                                value = localSettings.cpaCriticalThreshold,
                                onValueChange = { localSettings = localSettings.copy(cpaCriticalThreshold = it) },
                                modifier = Modifier.weight(1f)
                            )
                            ThresholdInput(
                                label = "주의 (NM 이하)",
                                value = localSettings.cpaWarningThreshold,
                                onValueChange = { localSettings = localSettings.copy(cpaWarningThreshold = it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // TCPA 설정
                    Column {
                        Text(
                            text = "TCPA (최근접점 도달 시간)",
                            fontSize = 14.sp,
                            color = AISTheme.textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ThresholdInput(
                                label = "즉시 위험 (분 이하)",
                                value = localSettings.tcpaCriticalThreshold.toDouble(),
                                onValueChange = { localSettings = localSettings.copy(tcpaCriticalThreshold = it.toInt()) },
                                modifier = Modifier.weight(1f),
                                isInt = true
                            )
                            ThresholdInput(
                                label = "주의 (분 이하)",
                                value = localSettings.tcpaWarningThreshold.toDouble(),
                                onValueChange = { localSettings = localSettings.copy(tcpaWarningThreshold = it.toInt()) },
                                modifier = Modifier.weight(1f),
                                isInt = true
                            )
                        }
                    }
                }
            }
        }

        item {
            // 알림 설정
            SettingsSection(
                title = "알림 설정",
                icon = Icons.Default.Notifications,
                iconColor = AISTheme.info
            ) {
                ToggleSetting(
                    label = "관심 선박 우선 알림",
                    description = "관심 선박의 위험 상태 변화를 우선적으로 알림",
                    checked = localSettings.watchlistAlerts,
                    onCheckedChange = { localSettings = localSettings.copy(watchlistAlerts = it) }
                )
            }
        }

        item {
            // 선종별 표시
            SettingsSection(
                title = "선종별 표시",
                icon = Icons.Default.Visibility,
                iconColor = AISTheme.safe
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleSetting(
                        label = "화물선",
                        icon = "📦",
                        checked = localSettings.showCargoVessels,
                        onCheckedChange = { localSettings = localSettings.copy(showCargoVessels = it) }
                    )
                    ToggleSetting(
                        label = "유조선",
                        icon = "⛽",
                        checked = localSettings.showTankers,
                        onCheckedChange = { localSettings = localSettings.copy(showTankers = it) }
                    )
                    ToggleSetting(
                        label = "여객선",
                        icon = "🚢",
                        checked = localSettings.showPassengerVessels,
                        onCheckedChange = { localSettings = localSettings.copy(showPassengerVessels = it) }
                    )
                    ToggleSetting(
                        label = "어선",
                        icon = "🎣",
                        checked = localSettings.showFishingVessels,
                        onCheckedChange = { localSettings = localSettings.copy(showFishingVessels = it) }
                    )
                    ToggleSetting(
                        label = "요트",
                        icon = "⛵",
                        checked = localSettings.showPleasureVessels,
                        onCheckedChange = { localSettings = localSettings.copy(showPleasureVessels = it) }
                    )
                    ToggleSetting(
                        label = "기타 선박",
                        icon = "🚤",
                        checked = localSettings.showOtherVessels,
                        onCheckedChange = { localSettings = localSettings.copy(showOtherVessels = it) }
                    )
                }
            }
        }

        item {
            // 기타 옵션
            SettingsSection(
                title = "기타 옵션",
                icon = Icons.Default.Settings,
                iconColor = AISTheme.textSecondary
            ) {
                ToggleSetting(
                    label = "정박 선박 표시",
                    description = "속력이 낮거나 정박 중인 선박 표시",
                    checked = localSettings.showAnchoredVessels,
                    onCheckedChange = { localSettings = localSettings.copy(showAnchoredVessels = it) }
                )
            }
        }

        item {
            // AIS 수신 상태
            SettingsSection(
                title = "AIS 수신 상태",
                icon = Icons.Default.Radio,
                iconColor = AISTheme.safe
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "정상 수신 중",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AISTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "마지막 업데이트: 방금 전",
                            fontSize = 14.sp,
                            color = AISTheme.textSecondary
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(AISTheme.safe, CircleShape)
                        )
                        Text(
                            text = "활성",
                            fontSize = 14.sp,
                            color = AISTheme.safe
                        )
                    }
                }
            }
        }

        item {
            // 설정 저장 버튼
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AISTheme.info, RoundedCornerShape(8.dp))
                    .clickable { MockData.updateSettings(localSettings) }
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "설정 저장",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    AISTheme.cardBackground,
                    RoundedCornerShape(8.dp)
                )
                .border(1.dp, AISTheme.borderColor, RoundedCornerShape(8.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ThresholdInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    isInt: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = AISTheme.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            value = if (isInt) value.toInt().toString() else String.format("%.1f", value),
            onValueChange = {
                val newValue = if (isInt) {
                    it.toIntOrNull()?.toDouble() ?: value
                } else {
                    it.toDoubleOrNull() ?: value
                }
                onValueChange(newValue)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AISTheme.cardBackgroundLight,
                unfocusedContainerColor = AISTheme.cardBackgroundLight,
                focusedTextColor = AISTheme.textPrimary,
                unfocusedTextColor = AISTheme.textPrimary,
                focusedIndicatorColor = AISTheme.borderColor,
                unfocusedIndicatorColor = AISTheme.borderColor
            ),
            singleLine = true
        )
    }
}

