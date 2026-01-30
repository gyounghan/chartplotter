package com.marineplay.chartplotter.presentation.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.marineplay.chartplotter.presentation.modules.ais.AISTheme
import com.marineplay.chartplotter.domain.entities.AISVessel
import kotlin.math.*

/**
 * 선박 위치 팝업 다이얼로그
 * 내 위치 기준 방위각과 거리를 표시
 */
@Composable
fun VesselLocationDialog(
    vessel: AISVessel,
    currentLatitude: Double?,
    currentLongitude: Double?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AISTheme.backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vessel.name,
                            fontSize = 22.sp,
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
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = AISTheme.textPrimary
                        )
                    }
                }
                
                // 위치 정보 표시
                if (vessel.latitude != null && vessel.longitude != null && 
                    currentLatitude != null && currentLongitude != null) {
                    val bearing = calculateBearing(
                        currentLatitude,
                        currentLongitude,
                        vessel.latitude,
                        vessel.longitude
                    )
                    val distanceMeters = calculateDistance(
                        currentLatitude,
                        currentLongitude,
                        vessel.latitude,
                        vessel.longitude
                    )
                    val distanceNauticalMiles = distanceMeters / 1852.0
                    
                    // 레이더 뷰 표시
                    RadarView(
                        bearing = bearing.toFloat(),
                        distance = distanceNauticalMiles,
                        vesselName = vessel.name,
                        vesselType = vessel.type
                    )
                    
                    // 상세 정보
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoRow(
                            label = "방위각",
                            value = "${bearing.toInt()}°",
                            description = getBearingDirection(bearing.toInt())
                        )
                        InfoRow(
                            label = "거리",
                            value = String.format("%.2f NM", distanceNauticalMiles),
                            description = String.format("%.1f km", distanceMeters / 1000.0)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "위치 정보가 없습니다",
                            color = AISTheme.textSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 레이더 뷰
 * 중심에 내 선박, 방위각과 거리에 따라 상대 위치에 선박 표시
 */
@Composable
private fun RadarView(
    bearing: Float,
    distance: Double,
    vesselName: String,
    vesselType: com.marineplay.chartplotter.domain.entities.VesselType
) {
    // 최대 표시 거리 (해리) - 레이더 범위
    val maxRange = 10.0 // 10 해리
    val scale = if (distance > maxRange) maxRange else distance
    
    // 거리에 비례한 위치 계산 (최대 반지름의 80%까지 사용)
    val maxRadius = 150.dp
    val radius = (maxRadius.value * (scale / maxRange) * 0.8).dp
    
    Box(
        modifier = Modifier.size(320.dp),
        contentAlignment = Alignment.Center
    ) {
        // 레이더 배경 (원형)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF1A1A2E), // 어두운 배경
                    CircleShape
                )
                .border(2.dp, AISTheme.borderColor, CircleShape)
        )
        
        // 거리 원 표시
        for (i in 1..3) {
            val circleRadius = (maxRadius.value * i / 3).dp
            Box(
                modifier = Modifier
                    .size(circleRadius * 2)
                    .border(1.dp, AISTheme.borderColor.copy(alpha = 0.3f), CircleShape)
            )
        }
        
        // 방향 라벨 (N, E, S, W)
        RadarLabels()
        
        // 방향선 (N, E, S, W) - 라벨 위에 표시
        RadarDirectionLines()
        
        // 중심: 내 선박 아이콘
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0000FF), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
        
        // 선박 위치 (방위각과 거리에 따라)
        if (distance <= maxRange) {
            // 방위각을 라디안으로 변환 (0도가 북쪽, 시계방향)
            val bearingRad = Math.toRadians(bearing.toDouble())
            
            // X, Y 좌표 계산 (중심 기준)
            val x = sin(bearingRad) * radius.value
            val y = -cos(bearingRad) * radius.value // Y축은 아래가 양수이므로 반대
            
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                // 선박 아이콘
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = vesselType.emoji,
                        fontSize = 24.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFF0000), CircleShape)
                            .offset(y = (-4).dp)
                    )
                }
            }
        } else {
            // 거리가 범위를 초과하는 경우 경계에 표시
            val bearingRad = Math.toRadians(bearing.toDouble())
            val edgeRadius = maxRadius.value * 0.8
            val x = sin(bearingRad) * edgeRadius
            val y = -cos(bearingRad) * edgeRadius
            
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = vesselType.emoji,
                        fontSize = 24.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFF0000), CircleShape)
                            .offset(y = (-4).dp)
                    )
                    Text(
                        text = ">${maxRange.toInt()}",
                        fontSize = 8.sp,
                        color = AISTheme.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarDirectionLines() {
    // N (0도)
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 북쪽 선
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(150.dp)
                .background(AISTheme.borderColor.copy(alpha = 0.3f))
                .offset(y = (-75).dp)
        )
        // 동쪽 선
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(1.dp)
                .background(AISTheme.borderColor.copy(alpha = 0.3f))
                .offset(x = 75.dp)
        )
        // 남쪽 선
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(150.dp)
                .background(AISTheme.borderColor.copy(alpha = 0.3f))
                .offset(y = 75.dp)
        )
        // 서쪽 선
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(1.dp)
                .background(AISTheme.borderColor.copy(alpha = 0.3f))
                .offset(x = (-75).dp)
        )
    }
}

@Composable
private fun RadarLabels() {
    val labels = listOf("N", "E", "S", "W")
    val angles = listOf(0f, 90f, 180f, 270f)
    
    labels.forEachIndexed { index, label ->
        val angle = angles[index]
        val angleRad = Math.toRadians(angle.toDouble())
        val radius = 140.dp.value
        val x = sin(angleRad) * radius
        val y = -cos(angleRad) * radius
        
        Box(
            modifier = Modifier
                .offset(x = x.dp, y = y.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (label == "N") Color(0xFFFF0000) else AISTheme.textSecondary
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AISTheme.cardBackground,
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = AISTheme.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 방위각을 방향으로 변환
 */
private fun getBearingDirection(bearing: Int): String {
    return when {
        bearing >= 348 || bearing < 12 -> "북"
        bearing >= 12 && bearing < 34 -> "북북동"
        bearing >= 34 && bearing < 56 -> "북동"
        bearing >= 56 && bearing < 78 -> "동북동"
        bearing >= 78 && bearing < 102 -> "동"
        bearing >= 102 && bearing < 124 -> "동남동"
        bearing >= 124 && bearing < 146 -> "남동"
        bearing >= 146 && bearing < 168 -> "남남동"
        bearing >= 168 && bearing < 192 -> "남"
        bearing >= 192 && bearing < 214 -> "남남서"
        bearing >= 214 && bearing < 236 -> "남서"
        bearing >= 236 && bearing < 258 -> "서남서"
        bearing >= 258 && bearing < 282 -> "서"
        bearing >= 282 && bearing < 304 -> "서북서"
        bearing >= 304 && bearing < 326 -> "북서"
        bearing >= 326 && bearing < 348 -> "북북서"
        else -> ""
    }
}

/**
 * 두 지점 간 방위각 계산 (도)
 */
private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLonRad = Math.toRadians(lon2 - lon1)
    
    val y = sin(deltaLonRad) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
    
    val bearingRad = atan2(y, x)
    val bearingDeg = Math.toDegrees(bearingRad)
    
    return ((bearingDeg % 360) + 360) % 360
}

/**
 * 두 지점 간 거리 계산 (미터) - Haversine 공식
 */
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // 지구 반지름 (미터)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

