package com.marineplay.chartplotter.ui.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.ais.AISTheme
import com.marineplay.chartplotter.ui.modules.ais.components.VesselRiskCard
import com.marineplay.chartplotter.ui.modules.ais.data.MockData
import com.marineplay.chartplotter.ui.modules.ais.models.RiskLevel

/**
 * 위험 분석 탭
 */
@Composable
fun RiskTab() {
    val vessels by MockData.vessels.collectAsState()
    
    val criticalVessels = vessels.filter { it.riskLevel == RiskLevel.CRITICAL }
    val warningVessels = vessels.filter { it.riskLevel == RiskLevel.WARNING }
    val safeCount = vessels.count { it.riskLevel == RiskLevel.SAFE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
            .padding(24.dp)
    ) {
        // 헤더
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "위험 분석",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CPA/TCPA 기준 위험 선박 모니터링",
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
        }

        // 위험 요약 카드
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RiskSummaryCard(
                label = "즉시 위험",
                count = criticalVessels.size,
                color = AISTheme.danger,
                backgroundColor = AISTheme.dangerBackground,
                modifier = Modifier.weight(1f)
            )
            RiskSummaryCard(
                label = "주의 필요",
                count = warningVessels.size,
                color = AISTheme.warning,
                backgroundColor = AISTheme.warningBackground,
                modifier = Modifier.weight(1f)
            )
            RiskSummaryCard(
                label = "정상",
                count = safeCount,
                color = AISTheme.safe,
                backgroundColor = AISTheme.safeBackground,
                modifier = Modifier.weight(1f)
            )
        }

        // 위험 선박 목록
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (criticalVessels.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "즉시 위험",
                        icon = Icons.Default.Warning,
                        color = AISTheme.danger
                    )
                }
                items(criticalVessels) { vessel ->
                    VesselRiskCard(vessel = vessel)
                }
            }

            if (warningVessels.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "주의 필요",
                        icon = Icons.Default.Warning,
                        color = AISTheme.warning
                    )
                }
                items(warningVessels) { vessel ->
                    VesselRiskCard(vessel = vessel)
                }
            }

            if (criticalVessels.isEmpty() && warningVessels.isEmpty()) {
                item {
                    EmptyState()
                }
            }
        }
    }
}

@Composable
private fun RiskSummaryCard(
    label: String,
    count: Int,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = AISTheme.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = "✓",
                fontSize = 64.sp,
                color = AISTheme.safe
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "현재 위험 선박 없음",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "모든 선박이 안전 거리 유지 중",
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
        }
    }
}

