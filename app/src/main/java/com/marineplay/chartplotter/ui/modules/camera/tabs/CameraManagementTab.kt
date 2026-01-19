package com.marineplay.chartplotter.ui.modules.camera.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.camera.CameraTheme
import com.marineplay.chartplotter.ui.modules.camera.components.CameraManagementCard
import com.marineplay.chartplotter.ui.modules.camera.models.CameraInfo
import com.marineplay.chartplotter.ui.modules.camera.models.CameraPosition
import com.marineplay.chartplotter.ui.modules.camera.models.CameraStatus

@Composable
fun CameraManagementTab() {
    // 더미 카메라 목록
    val cameras = remember {
        listOf(
            CameraInfo(
                position = CameraPosition.BOW,
                label = "전면",
                status = CameraStatus.CONNECTED,
                nightMode = false,
                resolution = "1920x1080",
                fps = 30
            ),
            CameraInfo(
                position = CameraPosition.STERN,
                label = "후면",
                status = CameraStatus.CONNECTED,
                nightMode = true,
                resolution = "1920x1080",
                fps = 30
            ),
            CameraInfo(
                position = CameraPosition.PORT,
                label = "좌현",
                status = CameraStatus.CONNECTED,
                nightMode = false,
                resolution = "1920x1080",
                fps = 30
            ),
            CameraInfo(
                position = CameraPosition.STARBOARD,
                label = "우현",
                status = CameraStatus.DISCONNECTED,
                nightMode = false,
                resolution = "1920x1080",
                fps = 30,
                lastConnected = "2026-01-12 10:23"
            )
        )
    }

    val connectedCount = cameras.count { it.status == CameraStatus.CONNECTED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraTheme.backgroundColor)
    ) {
        // 상단 헤더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1B2A))
                .border(1.dp, CameraTheme.borderColor, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "카메라 관리",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "연결됨: $connectedCount / ${cameras.size}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(CameraTheme.successColor, CircleShape)
                    )
                    Text(
                        text = "시스템 정상",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 카메라 목록
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            cameras.forEach { camera ->
                CameraManagementCard(camera = camera)
            }
        }

        // 하단 도움말
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1B2A))
                .border(1.dp, CameraTheme.borderColor, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "카메라 연결에 문제가 있는 경우, 장비 전원과 케이블 연결을 확인하세요.",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

