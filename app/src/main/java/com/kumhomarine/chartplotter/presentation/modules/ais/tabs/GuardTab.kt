package com.kumhomarine.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.presentation.modules.ais.AISTheme
import com.kumhomarine.chartplotter.presentation.modules.ais.data.MockData

/**
 * 경계 구역 탭 (AIS design GuardScreen 기준)
 */
@Composable
fun GuardTab() {
    val settings by MockData.settings.collectAsState()
    val guardEnabled = settings.guardEnabled
    val radius = settings.guardRadius
    val bowExtension = settings.guardBowExtension
    val alarmEnabled = settings.alarmEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(AISTheme.cardBackgroundLight)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "경계 구역 설정",
                fontSize = 18.sp,
                color = AISTheme.textPrimary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.Black)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "경계 구역",
                fontSize = 16.sp,
                color = AISTheme.textPrimary
            )
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .width(128.dp)
                    .clickable {
                        MockData.updateSettings(settings.copy(guardEnabled = !guardEnabled))
                    }
                    .background(
                        if (guardEnabled) AISTheme.borderColor else AISTheme.cardBackgroundLight
                    )
                    .border(
                        2.dp,
                        if (guardEnabled) AISTheme.safe else AISTheme.danger
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (guardEnabled) "활성화" else "비활성화",
                    fontSize = 14.sp,
                    color = if (guardEnabled) AISTheme.safe else AISTheme.danger
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .border(2.dp, AISTheme.borderColor)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "반경",
                    fontSize = 14.sp,
                    color = AISTheme.textPrimary
                )
                Text(
                    text = "${String.format("%.1f", radius)} NM",
                    fontSize = 24.sp,
                    color = AISTheme.safe
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = radius.toFloat(),
                onValueChange = { MockData.updateSettings(settings.copy(guardRadius = it.toDouble())) },
                valueRange = 0.5f..6f,
                enabled = guardEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = AISTheme.safe,
                    activeTrackColor = AISTheme.borderColor,
                    inactiveTrackColor = AISTheme.borderColor.copy(alpha = 0.3f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0.5 NM",
                    fontSize = 12.sp,
                    color = AISTheme.textDim
                )
                Text(
                    text = "6.0 NM",
                    fontSize = 12.sp,
                    color = AISTheme.textDim
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .border(2.dp, AISTheme.borderColor)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "선수 확장",
                    fontSize = 14.sp,
                    color = AISTheme.textPrimary
                )
                Text(
                    text = "${String.format("%.1f", bowExtension)} NM",
                    fontSize = 24.sp,
                    color = AISTheme.safe
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = bowExtension.toFloat(),
                onValueChange = { MockData.updateSettings(settings.copy(guardBowExtension = it.toDouble())) },
                valueRange = 0f..3f,
                enabled = guardEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = AISTheme.safe,
                    activeTrackColor = AISTheme.borderColor,
                    inactiveTrackColor = AISTheme.borderColor.copy(alpha = 0.3f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0.0 NM",
                    fontSize = 12.sp,
                    color = AISTheme.textDim
                )
                Text(
                    text = "3.0 NM",
                    fontSize = 12.sp,
                    color = AISTheme.textDim
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.Black)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "침입 경보",
                fontSize = 16.sp,
                color = AISTheme.textPrimary
            )
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .width(128.dp)
                    .clickable {
                        if (guardEnabled) {
                            MockData.updateSettings(settings.copy(alarmEnabled = !alarmEnabled))
                        }
                    }
                    .background(
                        if (alarmEnabled) AISTheme.borderColor else AISTheme.cardBackgroundLight
                    )
                    .border(
                        2.dp,
                        if (alarmEnabled) AISTheme.safe else AISTheme.danger
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (alarmEnabled) "활성화" else "비활성화",
                    fontSize = 14.sp,
                    color = if (alarmEnabled) AISTheme.safe else AISTheme.danger
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AISTheme.cardBackgroundLight)
                .border(2.dp, AISTheme.borderColor)
                .padding(24.dp)
        ) {
            Text(
                text = "경계 구역: 자선 주변의 원형 영역으로, AIS 표적이 진입 시 경보를 발생시킵니다.",
                fontSize = 12.sp,
                color = AISTheme.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "선수 확장: 현재 침로를 기준으로 선박 전방의 추가 보호 구역입니다.",
                fontSize = 12.sp,
                color = AISTheme.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "경보: 표적이 경계 구역을 침범할 때 청각 및 시각 경고를 제공합니다.",
                fontSize = 12.sp,
                color = AISTheme.textSecondary
            )
        }
    }
}
