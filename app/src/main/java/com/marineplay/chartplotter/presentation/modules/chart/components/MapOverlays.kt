package com.marineplay.chartplotter.presentation.modules.chart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.viewmodel.MainViewModel

/**
 * 지도 오버레이 컴포넌트 (GPS 정보, 커서 정보 등)
 */
@Composable
fun MapOverlays(viewModel: MainViewModel) {
    val mapUiState = viewModel.mapUiState
    val gpsUiState = viewModel.gpsUiState

    // 좌측 상단: 현재 GPS, COG, 화면표시 모드
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, top = 66.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // GPS 좌표
            if (gpsUiState.isAvailable) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "위도",
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format("%.6f", gpsUiState.latitude),
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "경도",
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format("%.6f", gpsUiState.longitude),
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "GPS 신호 없음",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // COG
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "COG",
                    color = Color.Black,
                    fontSize = 12.sp
                )
                Text(
                    text = "${String.format("%.1f", gpsUiState.cog)}°",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 화면표시 모드
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "모드",
                    color = Color.Black,
                    fontSize = 12.sp
                )
                Text(
                    text = mapUiState.mapDisplayMode,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // 좌측 하단: 커서 GPS 좌표
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        if (mapUiState.showCursor && mapUiState.cursorLatLng != null) {
            Box(
                modifier = Modifier
                    .background(
                        Color.DarkGray.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "커서 GPS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "위도: ${String.format("%.6f", mapUiState.cursorLatLng!!.latitude)}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "경도: ${String.format("%.6f", mapUiState.cursorLatLng!!.longitude)}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

