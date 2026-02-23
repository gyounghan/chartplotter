package com.marineplay.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.presentation.modules.ais.AISTheme
import com.marineplay.chartplotter.presentation.modules.ais.components.VesselCard
import com.marineplay.chartplotter.presentation.modules.ais.components.VesselLocationDialog
import com.marineplay.chartplotter.presentation.modules.ais.models.SortOption
import com.marineplay.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel

/**
 * 즐겨찾기 선박만 보기 탭
 */
@Composable
fun WatchlistTab(viewModel: AISViewModel) {
    val vessels by viewModel.vessels.collectAsState()
    val watchlistedVessels = vessels.filter { it.isWatchlisted }
    var sortBy by remember { mutableStateOf(SortOption.DISTANCE) }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedVessel by remember { mutableStateOf<AISVessel?>(null) }

    val sortedVessels = sortVessels(watchlistedVessels, sortBy, sortAscending)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "즐겨찾기",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AISTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "관심 선박 ${watchlistedVessels.size}척",
                fontSize = 14.sp,
                color = AISTheme.textSecondary
            )
        }

        if (watchlistedVessels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AISTheme.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "즐겨찾기한 선박이 없습니다",
                        fontSize = 16.sp,
                        color = AISTheme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "선박 목록에서 선박을 탭하면 즐겨찾기를 추가할 수 있습니다",
                        fontSize = 14.sp,
                        color = AISTheme.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedVessels, key = { it.id }) { vessel ->
                    VesselCard(
                        vessel = vessel,
                        onToggleWatchlist = { viewModel.toggleWatchlist(vessel.id) },
                        onClick = { selectedVessel = vessel }
                    )
                }
            }
        }

        selectedVessel?.let { vessel ->
            val (currentLat, currentLon) = viewModel.getCurrentLocation()
            VesselLocationDialog(
                vessel = vessel,
                currentLatitude = currentLat,
                currentLongitude = currentLon,
                onDismiss = { selectedVessel = null },
                onToggleWatchlist = { viewModel.toggleWatchlist(it) }
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
            if (ascending) vessels.sortedBy { it.distance }
            else vessels.sortedByDescending { it.distance }
        }
        SortOption.RISK -> {
            val riskOrder = mapOf(
                com.marineplay.chartplotter.domain.entities.RiskLevel.CRITICAL to 0,
                com.marineplay.chartplotter.domain.entities.RiskLevel.WARNING to 1,
                com.marineplay.chartplotter.domain.entities.RiskLevel.SAFE to 2
            )
            if (ascending) vessels.sortedBy { riskOrder[it.riskLevel] ?: 2 }
            else vessels.sortedByDescending { riskOrder[it.riskLevel] ?: 2 }
        }
        SortOption.NAME -> {
            if (ascending) vessels.sortedBy { it.name }
            else vessels.sortedByDescending { it.name }
        }
    }
    return sorted
}
