package com.marineplay.chartplotter.presentation.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.domain.entities.RiskLevel

@Composable
fun AISVesselDialog(
    vessel: AISVessel?,
    onDismiss: () -> Unit
) {
    if (vessel == null) return
    
    val riskLevelColor = when (vessel.riskLevel) {
        RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
        RiskLevel.WARNING -> MaterialTheme.colorScheme.tertiary
        RiskLevel.SAFE -> MaterialTheme.colorScheme.primary
    }
    
    val riskLevelText = vessel.riskLevel.label
    
    // 위경도 표시 (display 좌표 우선, 없으면 raw 좌표)
    val lat = vessel.displayLatitude ?: vessel.latitude
    val lon = vessel.displayLongitude ?: vessel.longitude
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = vessel.name.takeIf { it.isNotBlank() } ?: "AIS 선박",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // MMSI
                InfoRow("MMSI", vessel.mmsi)
                
                // 이름 (MMSI와 다를 경우만 표시)
                if (vessel.name.isNotBlank() && vessel.name != vessel.mmsi) {
                    InfoRow("이름", vessel.name)
                }
                
                // 위험 수준
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("위험 수준:", fontSize = 14.sp)
                    Text(
                        text = riskLevelText,
                        color = riskLevelColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Divider()
                
                // 위치 정보
                if (lat != null && lon != null) {
                    InfoRow("위도", String.format("%.6f°", lat))
                    InfoRow("경도", String.format("%.6f°", lon))
                }
                
                // 거리
                InfoRow("거리", String.format("%.2f 해리", vessel.distance))
                
                // 방위각
                InfoRow("방위각", "${vessel.bearing}°")
                
                Divider()
                
                // 속도 및 코스
                InfoRow("속도", String.format("%.1f 노트", vessel.speed))
                InfoRow("코스", "${vessel.course}°")
                
                Divider()
                
                // CPA/TCPA
                InfoRow("CPA", String.format("%.2f 해리", vessel.cpa))
                InfoRow("TCPA", "${vessel.tcpa} 분")
                
                // 선박 타입
                InfoRow("선박 타입", vessel.type.label + " " + vessel.type.emoji)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

