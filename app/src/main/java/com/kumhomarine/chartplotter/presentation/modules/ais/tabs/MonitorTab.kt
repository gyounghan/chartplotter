package com.kumhomarine.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.domain.entities.AISVessel
import com.kumhomarine.chartplotter.domain.entities.RiskLevel
import com.kumhomarine.chartplotter.presentation.modules.ais.AISTheme
import com.kumhomarine.chartplotter.presentation.modules.ais.components.TargetListPanel
import com.kumhomarine.chartplotter.presentation.modules.ais.data.MockData
import com.kumhomarine.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel

private enum class MonitorFilter(val label: String) {
    ALL("전체"),
    GUARD("경계범위"),
    OUTSIDE("그외 범위")
}

/**
 * 모니터 탭 (AIS design MonitorScreen 기준)
 * 레이더, CPA 경고, 범위 컨트롤, 표적 목록
 */
@Composable
fun MonitorTab(viewModel: AISViewModel) {
    val vessels by viewModel.vessels.collectAsState()
    val settings by MockData.settings.collectAsState()
    var range by remember { mutableStateOf(3.0) }
    var selectedFilter by remember { mutableStateOf(MonitorFilter.ALL) }
    val guardZoneEnabled = settings.guardEnabled
    val guardZoneRadius = settings.guardRadius
    val guardZoneBowExtension = settings.guardBowExtension
    val alarmEnabled = settings.alarmEnabled
    val (ownLat, ownLon) = viewModel.getCurrentLocation()
    val hasOwnPosition = ownLat != null && ownLon != null

    val ranges = listOf(0.5, 1.0, 3.0, 6.0)
    val now = System.currentTimeMillis()
    val activeVessels = vessels.filter { vessel ->
        val hasValidPosition = vessel.latitude != null && vessel.longitude != null
        val isRecent = (now - vessel.lastUpdate) <= 10 * 60 * 1000L
        val hasValidDistance = vessel.distance.isFinite() && vessel.distance > 0.01
        hasValidPosition && isRecent && hasValidDistance
    }
    val visibleVessels = activeVessels.filter { it.distance <= range }
    val guardBoundaryVessels = if (guardZoneEnabled) {
        visibleVessels.filter { vessel ->
            vessel.distance in 0.0..guardZoneRadius
        }
    } else {
        emptyList()
    }
    val outsideBoundaryVessels = if (guardZoneEnabled) {
        visibleVessels.filter { vessel ->
            vessel.distance > guardZoneRadius
        }
    } else {
        visibleVessels
    }
    val filteredVessels = when (selectedFilter) {
        MonitorFilter.ALL -> visibleVessels
        MonitorFilter.GUARD -> guardBoundaryVessels
        MonitorFilter.OUTSIDE -> outsideBoundaryVessels
    }
    val closestVessel = filteredVessels.minByOrNull { it.cpa }
    val intrusionTargets = if (guardZoneEnabled && hasOwnPosition) {
        guardBoundaryVessels
    } else {
        emptyList()
    }
    val cpaColor = closestVessel?.let {
        when {
            it.cpa < 0.5 -> AISTheme.danger
            it.cpa < 1.0 -> AISTheme.warning
            else -> AISTheme.safe
        }
    } ?: AISTheme.safe

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(2.dp, AISTheme.borderColor)
            ) {
                RadarDisplay(
                    vessels = filteredVessels,
                    range = range,
                    guardZoneEnabled = guardZoneEnabled,
                    guardZoneRadius = guardZoneRadius,
                    guardZoneBowExtension = guardZoneBowExtension
                )

                if (alarmEnabled && hasOwnPosition && intrusionTargets.isNotEmpty()) {
                    Text(
                        text = "침입 경보 ${intrusionTargets.size}척",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(AISTheme.dangerBackground)
                            .border(1.dp, AISTheme.danger)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = AISTheme.danger
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "범위",
                        fontSize = 12.sp,
                        color = AISTheme.textPrimary,
                        modifier = Modifier
                            .background(AISTheme.cardBackgroundLight)
                            .border(1.dp, AISTheme.borderColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ranges.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .width(64.dp)
                                    .clickable { range = r }
                                    .background(
                                        if (range == r) AISTheme.borderColor else AISTheme.cardBackgroundLight
                                    )
                                    .border(
                                        2.dp,
                                        if (range == r) AISTheme.safe else AISTheme.borderColor
                                    ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$r",
                                    fontSize = 12.sp,
                                    color = if (range == r) AISTheme.safe else AISTheme.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            closestVessel?.let { vessel ->
                Row(
                    modifier = Modifier
                        .height(96.dp)
                        .fillMaxWidth()
                        .background(AISTheme.backgroundColor)
                        .border(2.dp, cpaColor)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "최근접거리",
                            fontSize = 12.sp,
                            color = AISTheme.textSecondary
                        )
                        Text(
                            text = vessel.name,
                            fontSize = 24.sp,
                            color = cpaColor
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(
                            text = "${String.format("%.1f", vessel.cpa)} NM",
                            fontSize = 36.sp,
                            color = cpaColor
                        )
                        Text(
                            text = "${vessel.tcpa} 분",
                            fontSize = 14.sp,
                            color = AISTheme.textSecondary
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(Color.Black)
                .border(2.dp, AISTheme.borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(AISTheme.cardBackgroundLight)
                    .border(2.dp, AISTheme.borderColor)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                MonitorFilter.entries.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { selectedFilter = filter }
                            .background(
                                if (selectedFilter == filter) AISTheme.borderColor else AISTheme.cardBackgroundLight
                            )
                            .border(
                                2.dp,
                                if (selectedFilter == filter) AISTheme.safe else AISTheme.borderColor
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 11.sp,
                            color = if (selectedFilter == filter) AISTheme.safe else AISTheme.textPrimary
                        )
                    }
                }
            }

            TargetListPanel(
                vessels = filteredVessels,
                guardZoneEnabled = guardZoneEnabled,
                guardZoneRadius = guardZoneRadius,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RadarDisplay(
    vessels: List<AISVessel>,
    range: Double,
    guardZoneEnabled: Boolean,
    guardZoneRadius: Double,
    guardZoneBowExtension: Double
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width == 0 || size.height == 0) return@Canvas
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(centerX, centerY) - 18

            drawRect(color = androidx.compose.ui.graphics.Color.Black)

            drawCircle(
                color = AISTheme.borderColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
            for (i in 1..4) {
                drawCircle(
                    color = AISTheme.borderColor,
                    radius = radius * i / 4,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1f)
                )
            }

            drawLine(
                color = AISTheme.borderColor,
                start = Offset(centerX, centerY - radius),
                end = Offset(centerX, centerY + radius),
                strokeWidth = 1f
            )
            drawLine(
                color = AISTheme.borderColor,
                start = Offset(centerX - radius, centerY),
                end = Offset(centerX + radius, centerY),
                strokeWidth = 1f
            )

            if (guardZoneEnabled) {
                val guardRadiusPx = ((guardZoneRadius / range) * radius).toFloat()
                drawCircle(
                    color = AISTheme.warning,
                    radius = guardRadiusPx,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2f)
                )

                val bowOuterRadiusPx = (((guardZoneRadius + guardZoneBowExtension) / range) * radius).toFloat()
                if (guardZoneBowExtension > 0.0 && bowOuterRadiusPx > guardRadiusPx) {
                    // 선수(상단) 방향 확장 영역을 부채꼴로 표시
                    drawArc(
                        color = AISTheme.warning.copy(alpha = 0.22f),
                        startAngle = -120f,
                        sweepAngle = 60f,
                        useCenter = true,
                        topLeft = Offset(centerX - bowOuterRadiusPx, centerY - bowOuterRadiusPx),
                        size = androidx.compose.ui.geometry.Size(bowOuterRadiusPx * 2, bowOuterRadiusPx * 2)
                    )
                    drawArc(
                        color = AISTheme.warning,
                        startAngle = -120f,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = Offset(centerX - bowOuterRadiusPx, centerY - bowOuterRadiusPx),
                        size = androidx.compose.ui.geometry.Size(bowOuterRadiusPx * 2, bowOuterRadiusPx * 2),
                        style = Stroke(width = 2f)
                    )
                }
            }

            drawCircle(
                color = AISTheme.safe,
                radius = 12f,
                center = Offset(centerX, centerY)
            )

            vessels.filter { it.distance <= range }.forEach { vessel ->
                val bearingRad = Math.toRadians(vessel.bearing.toDouble())
                val distScale = (vessel.distance / range) * radius
                val x = centerX + (distScale * kotlin.math.sin(bearingRad)).toFloat()
                val y = centerY - (distScale * kotlin.math.cos(bearingRad)).toFloat()
                val isInGuard = guardZoneEnabled && vessel.distance <= guardZoneRadius
                val color = when {
                    vessel.riskLevel == RiskLevel.CRITICAL -> AISTheme.danger // 빨간색 우선 유지
                    isInGuard -> AISTheme.warning // 경계 내: 주황
                    else -> AISTheme.safe // 경계 밖: 초록
                }
                drawCircle(color = color, radius = 8f, center = Offset(x, y))
            }
        }
    }
}
