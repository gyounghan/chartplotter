package com.marineplay.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.modules.ais.AISTheme
import com.marineplay.chartplotter.presentation.modules.ais.components.VesselRiskCard
import com.marineplay.chartplotter.domain.entities.RiskLevel
import com.marineplay.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel

/**
 * 위험 분석 탭
 */
@Composable
fun RiskTab(viewModel: AISViewModel) {
    val vessels by viewModel.vessels.collectAsState()
    var selectedFilter by remember { mutableStateOf<RiskLevel?>(null) }
    
    val criticalVessels = vessels.filter { it.riskLevel == RiskLevel.CRITICAL }
    val warningVessels = vessels.filter { it.riskLevel == RiskLevel.WARNING }
    val safeVessels = vessels.filter { it.riskLevel == RiskLevel.SAFE }
    val safeCount = safeVessels.size
    
    // 필터링된 선박 목록
    val filteredVessels = when (selectedFilter) {
        RiskLevel.CRITICAL -> criticalVessels
        RiskLevel.WARNING -> warningVessels
        RiskLevel.SAFE -> safeVessels
        null -> emptyList() // 필터가 없으면 모든 항목 표시하지 않음
    }

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
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        selectedFilter = if (selectedFilter == RiskLevel.CRITICAL) null else RiskLevel.CRITICAL
                    },
                isSelected = selectedFilter == RiskLevel.CRITICAL
            )
            RiskSummaryCard(
                label = "주의 필요",
                count = warningVessels.size,
                color = AISTheme.warning,
                backgroundColor = AISTheme.warningBackground,
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        selectedFilter = if (selectedFilter == RiskLevel.WARNING) null else RiskLevel.WARNING
                    },
                isSelected = selectedFilter == RiskLevel.WARNING
            )
            RiskSummaryCard(
                label = "정상",
                count = safeCount,
                color = AISTheme.safe,
                backgroundColor = AISTheme.safeBackground,
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        selectedFilter = if (selectedFilter == RiskLevel.SAFE) null else RiskLevel.SAFE
                    },
                isSelected = selectedFilter == RiskLevel.SAFE
            )
        }

        // 위험 선박 목록
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedFilter != null) {
                // 필터가 선택된 경우 해당 항목만 표시
                if (filteredVessels.isNotEmpty()) {
                    item {
                        val title = when (selectedFilter) {
                            RiskLevel.CRITICAL -> "즉시 위험"
                            RiskLevel.WARNING -> "주의 필요"
                            RiskLevel.SAFE -> "정상"
                            null -> ""
                        }
                        val color = when (selectedFilter) {
                            RiskLevel.CRITICAL -> AISTheme.danger
                            RiskLevel.WARNING -> AISTheme.warning
                            RiskLevel.SAFE -> AISTheme.safe
                            null -> AISTheme.textPrimary
                        }
                        SectionHeader(
                            title = title,
                            icon = Icons.Default.Warning,
                            color = color
                        )
                    }
                    items(filteredVessels) { vessel ->
                        VesselRiskCard(vessel = vessel)
                    }
                } else {
                    item {
                        EmptyState()
                    }
                }
            } else {
                // 필터가 없는 경우 모든 항목 표시
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

                if (safeVessels.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "정상",
                            icon = Icons.Default.CheckCircle,
                            color = AISTheme.safe
                        )
                    }
                    items(safeVessels) { vessel ->
                        VesselRiskCard(vessel = vessel)
                    }
                }

                if (criticalVessels.isEmpty() && warningVessels.isEmpty() && safeVessels.isEmpty()) {
                    item {
                        EmptyState()
                    }
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
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Column(
        modifier = modifier
            .background(
                if (isSelected) color.copy(alpha = 0.3f) else backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                if (isSelected) 2.dp else 1.dp,
                color,
                RoundedCornerShape(8.dp)
            )
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

