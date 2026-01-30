package com.marineplay.chartplotter.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuPanel(
    showMenu: Boolean,
    currentMenu: String,
    onMenuClick: (String) -> Unit,
    onClose: () -> Unit
) {
    if (showMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onClose() }
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(300.dp)
                    .padding(16.dp)
                    .clickable { }, // 메뉴 내부 클릭은 이벤트 전파 방지
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    when (currentMenu) {
                        "main" -> MainMenuContent(onMenuClick)
                        "points" -> PointsMenuContent(onMenuClick)
                        "destinations" -> DestinationsMenuContent(onMenuClick)
                        "settings" -> SettingsMenuContent(onMenuClick)
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuContent(onMenuClick: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "메뉴",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        MenuItem("포인트 관리", "points", onMenuClick)
        MenuItem("목적지 관리", "destinations", onMenuClick)
        MenuItem("설정", "settings", onMenuClick)
    }
}

@Composable
fun PointsMenuContent(onMenuClick: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "포인트 관리",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        MenuItem("포인트 등록", "add_point", onMenuClick)
        MenuItem("포인트 목록", "point_list", onMenuClick)
        MenuItem("뒤로가기", "main", onMenuClick)
    }
}

@Composable
fun DestinationsMenuContent(onMenuClick: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "목적지 관리",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        MenuItem("목적지 생성", "add_destination", onMenuClick)
        MenuItem("목적지 목록", "destination_list", onMenuClick)
        MenuItem("뒤로가기", "main", onMenuClick)
    }
}

@Composable
fun SettingsMenuContent(onMenuClick: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "설정",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        MenuItem("화면 모드", "display_mode", onMenuClick)
        MenuItem("뒤로가기", "main", onMenuClick)
    }
}

@Composable
fun MenuItem(
    text: String,
    action: String,
    onMenuClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onMenuClick(action) }
            .padding(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
