package com.marineplay.chartplotter.ui.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.ais.AISTheme
import com.marineplay.chartplotter.domain.entities.RiskEvent
import com.marineplay.chartplotter.domain.entities.RiskLevel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 이벤트 카드
 */
@Composable
fun EventCard(
    event: RiskEvent,
    isFirst: Boolean,
    modifier: Modifier = Modifier
) {
    val riskColor = when (event.riskLevel) {
        RiskLevel.CRITICAL -> AISTheme.danger
        RiskLevel.WARNING -> AISTheme.warning
        RiskLevel.SAFE -> AISTheme.safe
    }
    
    val riskBg = when (event.riskLevel) {
        RiskLevel.CRITICAL -> AISTheme.dangerBackground
        RiskLevel.WARNING -> AISTheme.warningBackground
        RiskLevel.SAFE -> AISTheme.safeBackground
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 타임라인 인디케이터
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(riskBg, CircleShape)
                    .border(2.dp, riskColor, CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = riskColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!isFirst) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(AISTheme.borderColor)
                )
            }
        }

        // 이벤트 상세
        Column(
            modifier = Modifier
                .weight(1f)
                .background(
                    AISTheme.cardBackground,
                    RoundedCornerShape(8.dp)
                )
                .border(1.dp, AISTheme.borderColor, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(riskBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = event.riskLevel.label,
                        fontSize = 12.sp,
                        color = riskColor
                    )
                }
                Text(
                    text = formatRelativeTime(event.timestamp),
                    fontSize = 14.sp,
                    color = AISTheme.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 선박 이름
            Text(
                text = event.vesselName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 설명
            Text(
                text = event.description,
                fontSize = 14.sp,
                color = AISTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 지표
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricItem(
                    label = "CPA",
                    value = "${String.format("%.2f", event.cpa)} NM",
                    color = riskColor,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "TCPA",
                    value = "${event.tcpa}분",
                    color = riskColor,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "발생 시각",
                    value = formatTime(event.timestamp),
                    color = AISTheme.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 타임스탬프
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .border(1.dp, AISTheme.borderColor, RoundedCornerShape(0.dp)),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = AISTheme.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = formatDateTime(event.timestamp),
                    fontSize = 12.sp,
                    color = AISTheme.textSecondary
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = (diff / (60 * 1000)).toInt()
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}일 전"
        hours > 0 -> "${hours}시간 전"
        minutes > 0 -> "${minutes}분 전"
        else -> "방금 전"
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

