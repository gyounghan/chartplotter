package com.kumhomarine.chartplotter.presentation.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.domain.entities.AISVessel
import com.kumhomarine.chartplotter.domain.entities.RiskLevel
import com.kumhomarine.chartplotter.presentation.modules.ais.AISTheme

/**
 * AIS 표적 목록 패널 (AIS design TargetList 기준)
 */
@Composable
fun TargetListPanel(
    vessels: List<AISVessel>,
    guardZoneEnabled: Boolean,
    guardZoneRadius: Double,
    modifier: Modifier = Modifier
) {
    val sorted = vessels.sortedBy { it.distance }
    val getColor: (AISVessel) -> Color = { vessel ->
        val isInGuard = guardZoneEnabled && vessel.distance <= guardZoneRadius
        when {
            vessel.riskLevel == RiskLevel.CRITICAL -> AISTheme.danger
            isInGuard -> AISTheme.warning
            else -> AISTheme.safe
        }
    }

    Column(modifier = modifier) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(AISTheme.cardBackgroundLight)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 12.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = androidx.compose.ui.Alignment.CenterStart
            ) {
                Text(
                    text = "AIS 표적",
                    fontSize = 12.sp,
                    color = AISTheme.textPrimary
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(sorted) { vessel ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(1.dp, AISTheme.borderColor)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = vessel.name,
                            fontSize = 14.sp,
                            color = AISTheme.textPrimary
                        )
                        Text(
                            text = "${String.format("%.1f", vessel.distance)} NM",
                            fontSize = 14.sp,
                            color = getColor(vessel)
                        )
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = vessel.type.name,
                            fontSize = 12.sp,
                            color = AISTheme.textSecondary
                        )
                        Text(
                            text = "BRG ${vessel.bearing.toString().padStart(3, '0')}°",
                            fontSize = 12.sp,
                            color = AISTheme.textSecondary
                        )
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SPD ${String.format("%.1f", vessel.speed)} KT",
                            fontSize = 12.sp,
                            color = AISTheme.textSecondary
                        )
                        Text(
                            text = "CRS ${vessel.course.toString().padStart(3, '0')}°",
                            fontSize = 12.sp,
                            color = AISTheme.textSecondary
                        )
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CPA ${String.format("%.1f", vessel.cpa)} NM",
                            fontSize = 12.sp,
                            color = getColor(vessel)
                        )
                        Text(
                            text = "TCPA ${vessel.tcpa} MIN",
                            fontSize = 12.sp,
                            color = getColor(vessel)
                        )
                    }
                }
            }
        }
    }
}
