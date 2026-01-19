package com.marineplay.chartplotter.ui.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.ais.AISTheme
import com.marineplay.chartplotter.ui.modules.ais.models.AISVessel
import com.marineplay.chartplotter.ui.modules.ais.models.RiskLevel

/**
 * 선박 카드
 */
@Composable
fun VesselCard(
    vessel: AISVessel,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskBadge = getRiskBadge(vessel.riskLevel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                AISTheme.cardBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, AISTheme.borderColor, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = vessel.type.emoji,
                    fontSize = 32.sp
                )
                Column {
                    Text(
                        text = vessel.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AISTheme.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${vessel.type.label} · MMSI ${vessel.mmsi}",
                        fontSize = 14.sp,
                        color = AISTheme.textSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(riskBadge.bg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = riskBadge.label,
                        fontSize = 12.sp,
                        color = riskBadge.color
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (vessel.isWatchlisted) AISTheme.info else AISTheme.cardBackgroundLight,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable(onClick = onToggleWatchlist)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "관심 선박",
                        tint = if (vessel.isWatchlisted) Color.White else AISTheme.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 지표
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricColumn(
                label = "거리",
                value = "${String.format("%.1f", vessel.distance)} NM",
                modifier = Modifier.weight(1f)
            )
            MetricColumn(
                label = "방위",
                value = "${vessel.bearing}°",
                modifier = Modifier.weight(1f)
            )
            MetricColumn(
                label = "속력",
                value = "${String.format("%.1f", vessel.speed)} kts",
                modifier = Modifier.weight(1f)
            )
            MetricColumn(
                label = "침로",
                value = "${vessel.course}°",
                modifier = Modifier.weight(1f)
            )
        }

        // 위험 정보 (위험/주의인 경우만 표시)
        if (vessel.riskLevel == RiskLevel.CRITICAL || vessel.riskLevel == RiskLevel.WARNING) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(riskBadge.bg, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "CPA: ${String.format("%.2f", vessel.cpa)} NM · TCPA: ${vessel.tcpa}분",
                    fontSize = 12.sp,
                    color = AISTheme.textPrimary
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AISTheme.textPrimary
        )
    }
}

private fun getRiskBadge(riskLevel: RiskLevel): RiskBadge {
    return when (riskLevel) {
        RiskLevel.CRITICAL -> RiskBadge(
            label = "위험",
            color = AISTheme.danger,
            bg = AISTheme.dangerBackground
        )
        RiskLevel.WARNING -> RiskBadge(
            label = "주의",
            color = AISTheme.warning,
            bg = AISTheme.warningBackground
        )
        RiskLevel.SAFE -> RiskBadge(
            label = "정상",
            color = AISTheme.safe,
            bg = AISTheme.safeBackground
        )
    }
}

private data class RiskBadge(
    val label: String,
    val color: Color,
    val bg: Color
)

