package com.marineplay.chartplotter.presentation.modules.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.modules.camera.CameraTheme
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraInfo
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraPosition
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraStatus

@Composable
fun CameraFeedItem(
    position: CameraPosition,
    modifier: Modifier = Modifier
) {
    // 더미 카메라 정보
    val cameraInfo = remember(position) {
        when (position) {
            CameraPosition.BOW -> CameraInfo(
                position = CameraPosition.BOW,
                label = "전면",
                status = CameraStatus.CONNECTED,
                nightMode = false
            )
            CameraPosition.STERN -> CameraInfo(
                position = CameraPosition.STERN,
                label = "후면",
                status = CameraStatus.CONNECTED,
                nightMode = true
            )
            CameraPosition.PORT -> CameraInfo(
                position = CameraPosition.PORT,
                label = "좌현",
                status = CameraStatus.CONNECTED,
                nightMode = false
            )
            CameraPosition.STARBOARD -> CameraInfo(
                position = CameraPosition.STARBOARD,
                label = "우현",
                status = CameraStatus.DISCONNECTED,
                nightMode = false,
                lastConnected = "2026-01-12 10:23"
            )
        }
    }

    Box(
        modifier = modifier
            .background(
                if (cameraInfo.status == CameraStatus.CONNECTED) {
                    // 연결된 카메라는 비디오 피드처럼 보이는 그라데이션
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2A3B4D),
                            Color(0xFF1B2838),
                            Color(0xFF0A1929),
                            Color(0xFF1B2838),
                            Color(0xFF2A3B4D)
                        )
                    )
                } else {
                    // 연결 끊긴 카메라는 어두운 배경
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1B2838),
                            Color(0xFF0A1929)
                        )
                    )
                }
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (cameraInfo.status == CameraStatus.CONNECTED) {
            // 연결된 카메라: 비디오 피드처럼 보이는 패턴
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF8B97A8).copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = cameraInfo.label,
                        color = Color(0xFFE5E7EB).copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${cameraInfo.resolution} @ ${cameraInfo.fps}fps",
                        color = Color(0xFF8B97A8).copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    if (cameraInfo.nightMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "야간 모드",
                            color = Color(0xFF3B82F6).copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        } else {
            // 연결 끊긴 카메라
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF8B97A8).copy(alpha = 0.2f),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = cameraInfo.label,
                    color = Color(0xFFE5E7EB),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "연결 끊김",
                    color = CameraTheme.errorColor,
                    fontSize = 12.sp
                )
                if (cameraInfo.lastConnected != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "마지막 연결: ${cameraInfo.lastConnected}",
                        color = Color(0xFF8B97A8),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // 카메라 라벨 (좌측 하단)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .padding(start = 12.dp, bottom = 12.dp)
        ) {
            Text(
                text = cameraInfo.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

