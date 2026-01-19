package com.marineplay.chartplotter.ui.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.ais.AISTheme
import com.marineplay.chartplotter.ui.modules.ais.models.AISVessel
import com.marineplay.chartplotter.ui.modules.ais.models.RiskLevel

/**
 * 위험 선박 카드
 */
@Composable
fun VesselRiskCard(
    vessel: AISVessel,
    modifier: Modifier = Modifier
) {
    val riskColor = when (vessel.riskLevel) {
        RiskLevel.CRITICAL -> AISTheme.danger
        RiskLevel.WARNING -> AISTheme.warning
        RiskLevel.SAFE -> AISTheme.safe
    }
    
    val riskBg = when (vessel.riskLevel) {
        RiskLevel.CRITICAL -> AISTheme.dangerBackground
        RiskLevel.WARNING -> AISTheme.warningBackground
        RiskLevel.SAFE -> AISTheme.safeBackground
    }

    val direction = getApproachDirection(vessel.bearing)
    val description = "${direction}에서 접근 중, ${vessel.tcpa}분 후 교차 예상"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                AISTheme.cardBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .border(2.dp, riskColor, RoundedCornerShape(8.dp))
            .padding(20.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vessel.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AISTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MMSI: ${vessel.mmsi}",
                    fontSize = 14.sp,
                    color = AISTheme.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(riskBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = vessel.riskLevel.label,
                    fontSize = 14.sp,
                    color = riskColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 주요 지표
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricItem(
                label = "현재 거리",
                value = "${String.format("%.1f", vessel.distance)} NM",
                color = AISTheme.textPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "CPA",
                value = "${String.format("%.2f", vessel.cpa)} NM",
                color = riskColor,
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "TCPA",
                value = "${vessel.tcpa}분",
                color = riskColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 추가 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = AISTheme.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "침로 ${vessel.course}°",
                    fontSize = 14.sp,
                    color = AISTheme.textSecondary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = AISTheme.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${String.format("%.1f", vessel.speed)} kts",
                    fontSize = 14.sp,
                    color = AISTheme.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 위험 설명
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(riskBg, RoundedCornerShape(4.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = riskColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = AISTheme.textPrimary
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AISTheme.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun getApproachDirection(bearing: Int): String {
    return when {
        bearing >= 337 || bearing < 22 -> "정선수"
        bearing >= 22 && bearing < 67 -> "우선수"
        bearing >= 67 && bearing < 112 -> "우현"
        bearing >= 112 && bearing < 157 -> "우선미"
        bearing >= 157 && bearing < 202 -> "정선미"
        bearing >= 202 && bearing < 247 -> "좌선미"
        bearing >= 247 && bearing < 292 -> "좌현"
        else -> "좌선수"
    }
}

