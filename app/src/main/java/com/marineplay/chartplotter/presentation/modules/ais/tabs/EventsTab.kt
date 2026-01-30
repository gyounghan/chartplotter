package com.marineplay.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.marineplay.chartplotter.presentation.modules.ais.AISTheme
import com.marineplay.chartplotter.presentation.modules.ais.components.EventCard
import com.marineplay.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 이벤트 기록 탭
 */
@Composable
fun EventsTab(viewModel: AISViewModel) {
    val events by viewModel.events.collectAsState()
    
    val criticalEvents = events.filter { it.riskLevel == com.marineplay.chartplotter.domain.entities.RiskLevel.CRITICAL }
    val warningEvents = events.filter { it.riskLevel == com.marineplay.chartplotter.domain.entities.RiskLevel.WARNING }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
            .padding(24.dp)
    ) {
        // 헤더
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "이벤트 기록",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "위험 상황 발생 이력 · 총 ${events.size}건",
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
        }

        // 요약 통계
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                label = "즉시 위험 이벤트",
                count = criticalEvents.size,
                color = AISTheme.danger,
                backgroundColor = AISTheme.dangerBackground,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "주의 이벤트",
                count = warningEvents.size,
                color = AISTheme.warning,
                backgroundColor = AISTheme.warningBackground,
                modifier = Modifier.weight(1f)
            )
        }

        // 타임라인
        if (events.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(events) { index, event ->
                    EventCard(
                        event = event,
                        isFirst = index == 0
                    )
                }
            }
        } else {
            EmptyEventsState()
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    count: Int,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
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
            text = "${count}건",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun EmptyEventsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBoat,
                contentDescription = null,
                tint = AISTheme.textSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "기록된 이벤트 없음",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "위험 상황 발생 시 자동으로 기록됩니다",
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
        }
    }
}

