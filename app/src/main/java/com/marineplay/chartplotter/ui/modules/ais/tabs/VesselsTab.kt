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
import com.marineplay.chartplotter.ui.modules.ais.components.VesselLocationDialog
import com.marineplay.chartplotter.ui.modules.ais.presentation.viewmodel.AISViewModel
import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.ui.modules.ais.models.SortOption

/**
 * 선박 목록 탭
 */
@Composable
fun VesselsTab(viewModel: AISViewModel) {
    val vessels by viewModel.vessels.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    var sortBy by remember { mutableStateOf(SortOption.DISTANCE) }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedVessel by remember { mutableStateOf<AISVessel?>(null) }

    val watchlistedVessels: List<AISVessel> = vessels.filter { it.isWatchlisted }
    val otherVessels: List<AISVessel> = vessels.filter { !it.isWatchlisted }

    val sortedWatchlisted: List<AISVessel> = sortVessels(watchlistedVessels, sortBy, sortAscending)
    val sortedOthers: List<AISVessel> = sortVessels(otherVessels, sortBy, sortAscending)

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
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortButton(
                    label = "거리순",
                    isSelected = sortBy == SortOption.DISTANCE,
                    isAscending = sortAscending,
                    onClick = { 
                        if (sortBy == SortOption.DISTANCE) {
                            sortAscending = !sortAscending
                        } else {
                            sortBy = SortOption.DISTANCE
                            sortAscending = true
                        }
                    }
                )
                SortButton(
                    label = "위험도순",
                    isSelected = sortBy == SortOption.RISK,
                    isAscending = sortAscending,
                    onClick = { 
                        if (sortBy == SortOption.RISK) {
                            sortAscending = !sortAscending
                        } else {
                            sortBy = SortOption.RISK
                            sortAscending = true
                        }
                    }
                )
                SortButton(
                    label = "이름순",
                    isSelected = sortBy == SortOption.NAME,
                    isAscending = sortAscending,
                    onClick = { 
                        if (sortBy == SortOption.NAME) {
                            sortAscending = !sortAscending
                        } else {
                            sortBy = SortOption.NAME
                            sortAscending = true
                        }
                    }
                )
            }
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
                items(
                    items = sortedWatchlisted,
                    key = { it.id }
                ) { vessel ->
                    VesselCard(
                        vessel = vessel,
                        onToggleWatchlist = { viewModel.toggleWatchlist(vessel.id) },
                        onClick = { selectedVessel = vessel }
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
                items(
                    items = sortedOthers,
                    key = { it.id }
                ) { vessel ->
                    VesselCard(
                        vessel = vessel,
                        onToggleWatchlist = { viewModel.toggleWatchlist(vessel.id) },
                        onClick = { selectedVessel = vessel }
                    )
                }
            }
        }
        
        // 선박 위치 팝업 다이얼로그
        selectedVessel?.let { vessel ->
            val (currentLat, currentLon) = viewModel.getCurrentLocation()
            VesselLocationDialog(
                vessel = vessel,
                currentLatitude = currentLat,
                currentLongitude = currentLon,
                onDismiss = { selectedVessel = null }
            )
        }
    }
}

@Composable
private fun SortButton(
    label: String,
    isSelected: Boolean,
    isAscending: Boolean,
    onClick: () -> Unit
) {
    Row(
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isSelected) Color.White else AISTheme.textSecondary
        )
        if (isSelected) {
            Icon(
                imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = if (isAscending) "오름차순" else "내림차순",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun sortVessels(
    vessels: List<AISVessel>,
    sortBy: SortOption,
    ascending: Boolean
): List<AISVessel> {
    val sorted = when (sortBy) {
        SortOption.DISTANCE -> {
            if (ascending) {
                vessels.sortedBy { it.distance }
            } else {
                vessels.sortedByDescending { it.distance }
            }
        }
        SortOption.RISK -> {
            val riskOrder = mapOf(
                com.marineplay.chartplotter.domain.entities.RiskLevel.CRITICAL to 0,
                com.marineplay.chartplotter.domain.entities.RiskLevel.WARNING to 1,
                com.marineplay.chartplotter.domain.entities.RiskLevel.SAFE to 2
            )
            if (ascending) {
                vessels.sortedBy { riskOrder[it.riskLevel] ?: 2 }
            } else {
                vessels.sortedByDescending { riskOrder[it.riskLevel] ?: 2 }
            }
        }
        SortOption.NAME -> {
            if (ascending) {
                vessels.sortedBy { it.name }
            } else {
                vessels.sortedByDescending { it.name }
            }
        }
    }
    return sorted
}

