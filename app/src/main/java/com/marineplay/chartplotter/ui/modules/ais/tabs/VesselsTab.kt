package com.marineplay.chartplotter.ui.modules.ais.tabs

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
import com.marineplay.chartplotter.ui.modules.ais.AISTheme
import com.marineplay.chartplotter.ui.modules.ais.components.VesselCard
import com.marineplay.chartplotter.ui.modules.ais.data.MockData
import com.marineplay.chartplotter.ui.modules.ais.models.SortOption

/**
 * 선박 목록 탭
 */
@Composable
fun VesselsTab() {
    val vessels by MockData.vessels.collectAsState()
    var sortBy by remember { mutableStateOf(SortOption.DISTANCE) }

    val watchlistedVessels = vessels.filter { it.isWatchlisted }
    val otherVessels = vessels.filter { !it.isWatchlisted }

    val sortedWatchlisted = sortVessels(watchlistedVessels, sortBy)
    val sortedOthers = sortVessels(otherVessels, sortBy)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
            .padding(24.dp)
    ) {
        // 헤더
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "선박 목록",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "주변 AIS 선박 ${vessels.size}척",
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
        }

        // 정렬 옵션
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortButton(
                label = "거리순",
                isSelected = sortBy == SortOption.DISTANCE,
                onClick = { sortBy = SortOption.DISTANCE }
            )
            SortButton(
                label = "위험도순",
                isSelected = sortBy == SortOption.RISK,
                onClick = { sortBy = SortOption.RISK }
            )
            SortButton(
                label = "이름순",
                isSelected = sortBy == SortOption.NAME,
                onClick = { sortBy = SortOption.NAME }
            )
        }

        // 선박 목록
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sortedWatchlisted.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AISTheme.info,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "관심 선박 (${sortedWatchlisted.size})",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AISTheme.info
                        )
                    }
                }
                items(sortedWatchlisted) { vessel ->
                    VesselCard(
                        vessel = vessel,
                        onToggleWatchlist = { MockData.toggleWatchlist(vessel.id) }
                    )
                }
            }

            if (sortedOthers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "전체 선박 (${sortedOthers.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AISTheme.textPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(sortedOthers) { vessel ->
                    VesselCard(
                        vessel = vessel,
                        onToggleWatchlist = { MockData.toggleWatchlist(vessel.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) AISTheme.info else AISTheme.cardBackground,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isSelected) AISTheme.info else AISTheme.borderColor,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isSelected) Color.White else AISTheme.textSecondary
        )
    }
}

private fun sortVessels(
    vessels: List<com.marineplay.chartplotter.ui.modules.ais.models.AISVessel>,
    sortBy: SortOption
): List<com.marineplay.chartplotter.ui.modules.ais.models.AISVessel> {
    return when (sortBy) {
        SortOption.DISTANCE -> vessels.sortedBy { it.distance }
        SortOption.RISK -> {
            val riskOrder = mapOf(
                com.marineplay.chartplotter.ui.modules.ais.models.RiskLevel.CRITICAL to 0,
                com.marineplay.chartplotter.ui.modules.ais.models.RiskLevel.WARNING to 1,
                com.marineplay.chartplotter.ui.modules.ais.models.RiskLevel.SAFE to 2
            )
            vessels.sortedBy { riskOrder[it.riskLevel] ?: 2 }
        }
        SortOption.NAME -> vessels.sortedBy { it.name }
    }
}

