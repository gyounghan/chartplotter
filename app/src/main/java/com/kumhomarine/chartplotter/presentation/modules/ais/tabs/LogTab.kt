package com.kumhomarine.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.domain.entities.RiskEvent
import com.kumhomarine.chartplotter.domain.entities.RiskLevel
import com.kumhomarine.chartplotter.presentation.modules.ais.AISTheme
import com.kumhomarine.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 이벤트 기록 탭 (AIS design LogScreen 기준)
 */
@Composable
fun LogTab(viewModel: AISViewModel) {
    val events by viewModel.events.collectAsState()
    var selectedFilter by remember { mutableStateOf("전체") }

    val formatTimestamp: (Long) -> String = { timestamp ->
        SimpleDateFormat("MM/dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }

    fun getTypeLabel(riskLevel: RiskLevel): String = when (riskLevel) {
        RiskLevel.CRITICAL -> "CPA 경고"
        RiskLevel.WARNING -> "경계침범"
        RiskLevel.SAFE -> "즐겨찾기"
    }

    fun getSeverityColor(riskLevel: RiskLevel): Color = when (riskLevel) {
        RiskLevel.CRITICAL -> AISTheme.danger
        RiskLevel.WARNING -> AISTheme.warning
        RiskLevel.SAFE -> AISTheme.safe
    }

    fun getSeverityBg(riskLevel: RiskLevel): Color = when (riskLevel) {
        RiskLevel.CRITICAL -> AISTheme.dangerBackground
        RiskLevel.WARNING -> AISTheme.warningBackground
        RiskLevel.SAFE -> AISTheme.safeBackground
    }

    val filteredEvents = events.filter { event ->
        when (selectedFilter) {
            "전체" -> true
            "CPA 경고" -> event.riskLevel == RiskLevel.CRITICAL
            "경계침범" -> event.riskLevel == RiskLevel.WARNING
            "즐겨찾기" -> event.riskLevel == RiskLevel.SAFE
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(AISTheme.cardBackgroundLight)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "이벤트 기록",
                fontSize = 18.sp,
                color = AISTheme.textPrimary
            )
            Text(
                text = "${filteredEvents.size} 항목",
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.Black)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "필터",
                fontSize = 14.sp,
                color = AISTheme.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("전체", "CPA 경고", "경계침범", "즐겨찾기").forEach { filter ->
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .width(112.dp)
                            .clickable { selectedFilter = filter }
                            .background(
                                if (selectedFilter == filter) AISTheme.borderColor else Color.Black
                            )
                            .border(
                                2.dp,
                                if (selectedFilter == filter) AISTheme.safe else AISTheme.borderColor
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            color = if (selectedFilter == filter) AISTheme.safe else AISTheme.textPrimary
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .border(2.dp, AISTheme.borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(AISTheme.cardBackgroundLight)
                    .border(2.dp, AISTheme.borderColor)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("시간", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(100.dp))
                Text("유형", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(80.dp))
                Text("MMSI", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(80.dp))
                Text("선명", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(80.dp))
                Text("세부정보", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.weight(1f))
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filteredEvents) { _, event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(getSeverityBg(event.riskLevel))
                            .border(1.dp, AISTheme.borderColor)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(event.timestamp),
                            fontSize = 12.sp,
                            color = AISTheme.textSecondary,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = getTypeLabel(event.riskLevel),
                            fontSize = 12.sp,
                            color = getSeverityColor(event.riskLevel),
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = event.vesselId.removePrefix("ais_"),
                            fontSize = 14.sp,
                            color = AISTheme.textPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = event.vesselName,
                            fontSize = 14.sp,
                            color = AISTheme.textPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = event.description,
                            fontSize = 14.sp,
                            color = AISTheme.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(AISTheme.cardBackgroundLight)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .width(128.dp)
                        .clickable { }
                        .background(Color.Black)
                        .border(2.dp, AISTheme.borderColor)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("내보내기", fontSize = 14.sp, color = AISTheme.textPrimary)
                }
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .width(128.dp)
                        .clickable { }
                        .background(Color.Black)
                        .border(2.dp, AISTheme.danger)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("기록삭제", fontSize = 14.sp, color = AISTheme.danger)
                }
            }
        }
    }
}
