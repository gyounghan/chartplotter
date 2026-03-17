package com.kumhomarine.chartplotter.presentation.modules.ais.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.domain.entities.AISVessel
import com.kumhomarine.chartplotter.presentation.modules.ais.AISTheme
import com.kumhomarine.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel

/**
 * 감시 목록 탭 (AIS design WatchScreen 기준)
 */
@Composable
fun WatchTab(viewModel: AISViewModel) {
    val vessels by viewModel.vessels.collectAsState()
    val watchList = vessels.filter { it.isWatchlisted }
    var mmsiInput by remember { mutableStateOf("") }

    val formatLastReceived: (Long) -> String = { timestamp ->
        val seconds = (System.currentTimeMillis() - timestamp) / 1000
        when {
            seconds < 60 -> "${seconds}초 전"
            seconds < 3600 -> "${seconds / 60}분 전"
            else -> "${seconds / 3600}시간 전"
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "즐겨찾기",
                fontSize = 18.sp,
                color = AISTheme.textPrimary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(androidx.compose.ui.graphics.Color.Black)
                .border(2.dp, AISTheme.borderColor)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MMSI 추가",
                fontSize = 14.sp,
                color = AISTheme.textPrimary
            )
            OutlinedTextField(
                value = mmsiInput,
                onValueChange = {
                    if (it.length <= 9 && it.all { c -> c.isDigit() }) {
                        mmsiInput = it
                    }
                },
                placeholder = { Text("9자리 숫자", color = AISTheme.textDim) },
                modifier = Modifier
                    .width(256.dp)
                    .height(56.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AISTheme.textPrimary,
                    unfocusedTextColor = AISTheme.textPrimary,
                    focusedBorderColor = AISTheme.safe,
                    unfocusedBorderColor = AISTheme.borderColor,
                    cursorColor = AISTheme.safe
                )
            )
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .width(128.dp)
                    .padding(horizontal = 16.dp)
                    .then(
                        Modifier.background(
                            if (mmsiInput.length == 9) AISTheme.borderColor else AISTheme.cardBackgroundLight
                        )
                    )
                    .then(
                        Modifier.border(
                            2.dp,
                            if (mmsiInput.length == 9) AISTheme.safe else AISTheme.borderColor
                        )
                    )
                    .then(
                        Modifier.clickable(enabled = mmsiInput.length == 9) {
                            if (mmsiInput.length == 9) {
                                vessels.find { it.mmsi == mmsiInput }?.let {
                                    viewModel.toggleWatchlist(it.id)
                                    mmsiInput = ""
                                }
                            }
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "추가",
                    fontSize = 14.sp,
                    color = if (mmsiInput.length == 9) AISTheme.safe else AISTheme.textDim
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color.Black)
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
                Text("상태", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(60.dp))
                Text("MMSI", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(80.dp))
                Text("선명", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(80.dp))
                Text("거리", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(60.dp))
                Text("방위", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(60.dp))
                Text("최종수신", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.weight(1f))
                Text("동작", fontSize = 12.sp, color = AISTheme.textSecondary, modifier = Modifier.width(60.dp))
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(watchList) { vessel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .border(1.dp, AISTheme.borderColor)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "활성",
                            fontSize = 12.sp,
                            color = AISTheme.safe,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = vessel.mmsi,
                            fontSize = 14.sp,
                            color = AISTheme.textPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = vessel.name,
                            fontSize = 14.sp,
                            color = AISTheme.textPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", vessel.distance)} NM",
                            fontSize = 14.sp,
                            color = AISTheme.textPrimary,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = "${vessel.bearing.toString().padStart(3, '0')}°",
                            fontSize = 14.sp,
                            color = AISTheme.textPrimary,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = formatLastReceived(vessel.lastUpdate),
                            fontSize = 12.sp,
                            color = AISTheme.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                        modifier = Modifier
                            .width(80.dp)
                            .height(32.dp)
                            .border(2.dp, AISTheme.danger)
                            .padding(horizontal = 8.dp)
                            .clickable { viewModel.toggleWatchlist(vessel.id) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "제거",
                                fontSize = 12.sp,
                                color = AISTheme.danger
                            )
                        }
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
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "즐겨찾기를 통해 MMSI 번호로 특정 선박을 수동 추적할 수 있습니다. 활성 표적은 실시간으로 업데이트됩니다.",
                fontSize = 12.sp,
                color = AISTheme.textSecondary
            )
        }
    }
}
