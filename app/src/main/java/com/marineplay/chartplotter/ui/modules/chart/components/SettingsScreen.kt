package com.marineplay.chartplotter.ui.modules.chart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.viewmodel.MainViewModel

/**
 * 설정 화면 - 2단 레이아웃
 * 좌측: 카테고리 목록 (시스템, 기능, 서비스 등)
 * 우측: 선택된 카테고리의 세부 옵션
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("시스템") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 좌측: 카테고리 목록
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1A1A))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "설정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 카테고리 목록
                SettingsCategoryItem(
                    title = "시스템",
                    icon = Icons.Default.Settings,
                    isSelected = selectedCategory == "시스템",
                    onClick = { selectedCategory = "시스템" }
                )
                SettingsCategoryItem(
                    title = "항해",
                    icon = Icons.Default.Place,
                    isSelected = selectedCategory == "항해",
                    onClick = { selectedCategory = "항해" }
                )
                SettingsCategoryItem(
                    title = "지도",
                    icon = Icons.Default.Map,
                    isSelected = selectedCategory == "지도",
                    onClick = { selectedCategory = "지도" }
                )
                SettingsCategoryItem(
                    title = "어탐",
                    icon = Icons.Default.WaterDrop,
                    isSelected = selectedCategory == "어탐",
                    onClick = { selectedCategory = "어탐" }
                )
                SettingsCategoryItem(
                    title = "레이더",
                    icon = Icons.Default.LocationSearching,
                    isSelected = selectedCategory == "레이더",
                    onClick = { selectedCategory = "레이더" }
                )
                SettingsCategoryItem(
                    title = "오토파일럿",
                    icon = Icons.Default.Navigation,
                    isSelected = selectedCategory == "오토파일럿",
                    onClick = { selectedCategory = "오토파일럿" }
                )
                SettingsCategoryItem(
                    title = "카메라",
                    icon = Icons.Default.CameraAlt,
                    isSelected = selectedCategory == "카메라",
                    onClick = { selectedCategory = "카메라" }
                )
                SettingsCategoryItem(
                    title = "연료",
                    icon = Icons.Default.LocalGasStation,
                    isSelected = selectedCategory == "연료",
                    onClick = { selectedCategory = "연료" }
                )
                SettingsCategoryItem(
                    title = "경보",
                    icon = Icons.Default.Notifications,
                    isSelected = selectedCategory == "경보",
                    onClick = { selectedCategory = "경보" }
                )
            }
            
            // 우측: 세부 옵션
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
            ) {
                when (selectedCategory) {
                    "시스템" -> SystemSettingsContent(viewModel)
                    "항해" -> NavigationSettingsContent(viewModel)
                    "지도" -> MapSettingsContent(viewModel)
                    "어탐" -> FishfinderSettingsContent(viewModel)
                    "레이더" -> RadarSettingsContent(viewModel)
                    "오토파일럿" -> AutopilotSettingsContent(viewModel)
                    "카메라" -> CameraSettingsContent(viewModel)
                    "연료" -> FuelSettingsContent(viewModel)
                    "경보" -> AlertSettingsContent(viewModel)
                    else -> SystemSettingsContent(viewModel)
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFF0066CC) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp
        )
        if (isSelected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SystemSettingsContent(viewModel: MainViewModel) {
    val systemSettings = viewModel.systemSettings
    var showLanguageOptions by remember { mutableStateOf(false) }
    var showFontSizeOptions by remember { mutableStateOf(false) }
    var showVolumeOptions by remember { mutableStateOf(false) }
    var showTimeOptions by remember { mutableStateOf(false) }
    var showGeodeticOptions by remember { mutableStateOf(false) }
    var showCoordinateOptions by remember { mutableStateOf(false) }
    var showDeclinationOptions by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 사용언어
        SettingsRow(
            title = "사용언어",
            value = systemSettings.language,
            onClick = { showLanguageOptions = !showLanguageOptions }
        )
        if (showLanguageOptions) {
            val languages = listOf("한국어", "영어", "일본어", "중국어")
            languages.forEach { language ->
                Text(
                    language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateLanguage(language)
                            showLanguageOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.language == language) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 자선 설정
        SettingsRow(
            title = "자선 설정",
            value = null,
            onClick = {
                viewModel.updateShowVesselSettingsDialog(true)
            },
            hasSubMenu = true
        )
        
        // 문자 크기
        SettingsRow(
            title = "문자 크기",
            value = when {
                systemSettings.fontSize <= 12f -> "작은 글자"
                systemSettings.fontSize <= 14f -> "일반"
                systemSettings.fontSize <= 16f -> "큰 글자"
                systemSettings.fontSize <= 18f -> "매우 큰 글자"
                else -> "특대 글자"
            },
            onClick = { showFontSizeOptions = !showFontSizeOptions }
        )
        if (showFontSizeOptions) {
            val fontSizes = listOf(
                Pair(12f, "작은 글자"),
                Pair(14f, "일반"),
                Pair(16f, "큰 글자"),
                Pair(18f, "매우 큰 글자"),
                Pair(20f, "특대 글자")
            )
            fontSizes.forEach { (size, label) ->
                Text(
                    label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateFontSize(size)
                            showFontSizeOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (kotlin.math.abs(systemSettings.fontSize - size) < 0.5f) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 키음
        SettingsRow(
            title = "키음",
            value = "${systemSettings.buttonVolume.toInt()}%",
            onClick = { showVolumeOptions = !showVolumeOptions }
        )
        if (showVolumeOptions) {
            val volumes = listOf(0, 25, 50, 75, 100)
            volumes.forEach { volume ->
                Text(
                    "$volume%",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateButtonVolume(volume)
                            showVolumeOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (kotlin.math.abs(systemSettings.buttonVolume - volume) <= 12) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 시간
        SettingsRow(
            title = "시간",
            value = null,
            onClick = { showTimeOptions = !showTimeOptions },
            hasSubMenu = true
        )
        if (showTimeOptions) {
            Text(
                "표시형식",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
            )
            listOf("24시간", "12시간").forEach { format ->
                Text(
                    format,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateTimeFormat(format)
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.timeFormat == format) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "날짜형식",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
            )
            listOf("YYYY-MM-DD", "MM/DD/YYYY", "DD/MM/YYYY").forEach { format ->
                Text(
                    format,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateDateFormat(format)
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.dateFormat == format) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 측지계
        SettingsRow(
            title = "측지계",
            value = systemSettings.geodeticSystem,
            onClick = { showGeodeticOptions = !showGeodeticOptions }
        )
        if (showGeodeticOptions) {
            listOf("WGS84", "GRS80", "Bessel", "Tokyo", "PZ-90").forEach { system ->
                Text(
                    system,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateGeodeticSystem(system)
                            showGeodeticOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.geodeticSystem == system) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 위경도 표시
        SettingsRow(
            title = "위경도 표시",
            value = systemSettings.coordinateFormat,
            onClick = { showCoordinateOptions = !showCoordinateOptions }
        )
        if (showCoordinateOptions) {
            listOf("도", "도분", "도분초").forEach { format ->
                Text(
                    format,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateCoordinateFormat(format)
                            showCoordinateOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.coordinateFormat == format) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 자기 변량
        SettingsRow(
            title = "자기 변량",
            value = if (systemSettings.declinationMode == "자동") "자동" else "수동: ${systemSettings.declinationValue}°",
            onClick = { showDeclinationOptions = !showDeclinationOptions }
        )
        if (showDeclinationOptions) {
            Text(
                "자동",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.updateDeclinationMode("자동")
                        showDeclinationOptions = false
                    }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                color = if (systemSettings.declinationMode == "자동") Color(0xFFFFD700) else Color.White,
                fontSize = 13.sp
            )
            Text(
                "수동",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.updateShowDeclinationDialog(true)
                        showDeclinationOptions = false
                    }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                color = if (systemSettings.declinationMode == "수동") Color(0xFFFFD700) else Color.White,
                fontSize = 13.sp
            )
        }
        
        // 기본설정으로 복원
        SettingsRow(
            title = "기본설정으로 복원",
            value = null,
            onClick = {
                viewModel.updateShowResetConfirmDialog(true)
            },
            hasSubMenu = true
        )
        
        // 전원제어
        SettingsRow(
            title = "전원제어",
            value = null,
            onClick = { viewModel.updateCurrentMenu("system_power") },
            hasSubMenu = true
        )
        
        // 고급
        SettingsRow(
            title = "고급",
            value = null,
            onClick = { viewModel.updateCurrentMenu("system_advanced") },
            hasSubMenu = true
        )
        
        // 연결 및 등록
        SettingsRow(
            title = "연결 및 등록",
            value = if (systemSettings.mobileConnected) "연결됨" else "연결 안됨",
            onClick = { viewModel.updateCurrentMenu("system_connection") }
        )
        
        // 정보
        SettingsRow(
            title = "정보",
            value = null,
            onClick = {
                viewModel.updateShowInfoDialog(true)
            },
            hasSubMenu = true
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    hasSubMenu: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (value != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
            
            if (hasSubMenu || value == null) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 다른 카테고리들의 플레이스홀더
@Composable
private fun FeaturesSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "기능 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ServiceSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "서비스 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun NavigationSettingsContent(viewModel: MainViewModel) {
    val systemSettings = viewModel.systemSettings
    val trackSettings = viewModel.getTrackSettings()
    
    var showArrivalRadiusDialog by remember { mutableStateOf(false) }
    var showXteLimitDialog by remember { mutableStateOf(false) }
    var showRecordTypeOptions by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 도착변경 (도착반경 설정)
        SettingsRow(
            title = "도착변경",
            value = "${systemSettings.arrivalRadius.toInt()}m",
            onClick = { showArrivalRadiusDialog = true }
        )
        
        // XTE 제한 (XTE 제한 반경 설정)
        SettingsRow(
            title = "XTE 제한",
            value = "${systemSettings.xteLimit.toInt()}m",
            onClick = { showXteLimitDialog = true }
        )
        
        // XTE 경보 (XTE 경보 ON/OFF)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateXteAlertEnabled(!systemSettings.xteAlertEnabled)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "XTE 경보",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.xteAlertEnabled,
                onCheckedChange = { viewModel.updateXteAlertEnabled(it) }
            )
        }
        
        // 항적 (항적 목록으로 이동)
        SettingsRow(
            title = "항적",
            value = null,
            onClick = {
                viewModel.updateShowTrackListDialog(true)
            },
            hasSubMenu = true
        )
        
        // 기록형태 (항적 기록 형태 설정 자동/거리/시간)
        SettingsRow(
            title = "기록형태",
            value = when (trackSettings.intervalType) {
                "auto" -> "자동"
                "time" -> "시간"
                "distance" -> "거리"
                else -> "자동"
            },
            onClick = { showRecordTypeOptions = !showRecordTypeOptions }
        )
        if (showRecordTypeOptions) {
            listOf("자동", "거리", "시간").forEach { type ->
                Text(
                    type,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intervalType = when (type) {
                                "자동" -> "auto"
                                "거리" -> "distance"
                                "시간" -> "time"
                                else -> "auto"
                            }
                            val newSettings = trackSettings.copy(intervalType = intervalType)
                            viewModel.saveTrackSettings(newSettings)
                            showRecordTypeOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (when (type) {
                        "자동" -> trackSettings.intervalType == "auto"
                        "거리" -> trackSettings.intervalType == "distance"
                        "시간" -> trackSettings.intervalType == "time"
                        else -> false
                    }) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
    
    // 도착반경 설정 다이얼로그
    if (showArrivalRadiusDialog) {
        var radiusText by remember { mutableStateOf(systemSettings.arrivalRadius.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showArrivalRadiusDialog = false },
            title = { Text("도착반경 설정", color = Color.White) },
            text = {
                Column {
                    Text("도착반경 (미터)", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = radiusText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) radiusText = it },
                        label = { Text("미터") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.Gray,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        radiusText.toIntOrNull()?.let {
                            viewModel.updateArrivalRadius(it.toFloat())
                            showArrivalRadiusDialog = false
                        }
                    }
                ) {
                    Text("확인", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArrivalRadiusDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
    
    // XTE 제한 반경 설정 다이얼로그
    if (showXteLimitDialog) {
        var limitText by remember { mutableStateOf(systemSettings.xteLimit.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showXteLimitDialog = false },
            title = { Text("XTE 제한 반경 설정", color = Color.White) },
            text = {
                Column {
                    Text("XTE 제한 반경 (미터)", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = limitText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) limitText = it },
                        label = { Text("미터") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.Gray,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        limitText.toIntOrNull()?.let {
                            viewModel.updateXteLimit(it.toFloat())
                            showXteLimitDialog = false
                        }
                    }
                ) {
                    Text("확인", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showXteLimitDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
}

@Composable
private fun MapSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "지도 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FishfinderSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "어탐 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun RadarSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "레이더 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AutopilotSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "오토파일럿 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun CameraSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "카메라 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FuelSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "연료 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AlertSettingsContent(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "경보 설정",
            color = Color.White,
            fontSize = 18.sp
        )
        Text(
            text = "향후 구현 예정",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

