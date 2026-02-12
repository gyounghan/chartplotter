package com.marineplay.chartplotter.presentation.modules.chart.components

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
import com.marineplay.chartplotter.viewmodel.SettingsViewModel

/**
 * 설정 화면 - 2단 레이아웃
 * 좌측: 카테고리 목록 (시스템, 기능, 서비스 등)
 * 우측: 선택된 카테고리의 세부 옵션
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
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
                    title = "무선",
                    icon = Icons.Default.Bluetooth,
                    isSelected = selectedCategory == "무선",
                    onClick = { selectedCategory = "무선" }
                )
                SettingsCategoryItem(
                    title = "네트워크",
                    icon = Icons.Default.Cloud,
                    isSelected = selectedCategory == "네트워크",
                    onClick = { selectedCategory = "네트워크" }
                )
                SettingsCategoryItem(
                    title = "선박",
                    icon = Icons.Default.DirectionsBoat,
                    isSelected = selectedCategory == "선박",
                    onClick = { selectedCategory = "선박" }
                )
                SettingsCategoryItem(
                    title = "카메라",
                    icon = Icons.Default.CameraAlt,
                    isSelected = selectedCategory == "카메라",
                    onClick = { selectedCategory = "카메라" }
                )
                SettingsCategoryItem(
                    title = "경보",
                    icon = Icons.Default.Notifications,
                    isSelected = selectedCategory == "경보",
                    onClick = { selectedCategory = "경보" }
                )
                SettingsCategoryItem(
                    title = "단위",
                    icon = Icons.Default.Settings,
                    isSelected = selectedCategory == "단위",
                    onClick = { selectedCategory = "단위" }
                )
            }
            
            // 우측: 세부 옵션
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
            ) {
                when (selectedCategory) {
                    "시스템" -> SystemSettingsContent(settingsViewModel, viewModel)
                    "항해" -> NavigationSettingsContent(settingsViewModel, viewModel)
                    "지도" -> MapSettingsContent(settingsViewModel)
                    "무선" -> WirelessSettingsContent(settingsViewModel)
                    "네트워크" -> NetworkSettingsContent(settingsViewModel)
                    "선박" -> VesselSettingsContent(settingsViewModel)
                    "카메라" -> CameraSettingsContent(settingsViewModel)
                    "경보" -> AlertSettingsContent(settingsViewModel)
                    "단위" -> UnitSettingsContent(settingsViewModel)
                    else -> SystemSettingsContent(settingsViewModel, viewModel)
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
private fun SystemSettingsContent(viewModel: SettingsViewModel, mainViewModel: MainViewModel) {
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
                mainViewModel.updateShowVesselSettingsDialog(true)
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
                        mainViewModel.updateShowDeclinationDialog(true)
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
                mainViewModel.updateShowResetConfirmDialog(true)
            },
            hasSubMenu = true
        )
        
        // 전원제어
        SettingsRow(
            title = "전원제어",
            value = null,
            onClick = { mainViewModel.updateCurrentMenu("system_power") },
            hasSubMenu = true
        )
        
        // 고급
        SettingsRow(
            title = "고급",
            value = null,
            onClick = { mainViewModel.updateCurrentMenu("system_advanced") },
            hasSubMenu = true
        )
        
        // 연결 및 등록
        SettingsRow(
            title = "연결 및 등록",
            value = if (systemSettings.mobileConnected) "연결됨" else "연결 안됨",
            onClick = { mainViewModel.updateCurrentMenu("system_connection") }
        )
        
        // 정보
        SettingsRow(
            title = "정보",
            value = null,
            onClick = {
                mainViewModel.updateShowInfoDialog(true)
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
private fun FeaturesSettingsContent(viewModel: SettingsViewModel) {
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
private fun ServiceSettingsContent(viewModel: SettingsViewModel) {
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
private fun NavigationSettingsContent(viewModel: SettingsViewModel, mainViewModel: MainViewModel) {
    val systemSettings = viewModel.systemSettings
    var showArrivalRadiusDialog by remember { mutableStateOf(false) }
    var showXteLimitDialog by remember { mutableStateOf(false) }
    
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
                mainViewModel.updateShowTrackListDialog(true)
            },
            hasSubMenu = true
        )
        
        // 기록형태 설정은 제거됨 (항적별 설정은 항적 목록에서 직접 수정)
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
private fun MapSettingsContent(viewModel: SettingsViewModel) {
    val systemSettings = viewModel.systemSettings
    
    var showDistanceCircleDialog by remember { mutableStateOf(false) }
    var showExtensionLengthDialog by remember { mutableStateOf(false) }
    var showBoat3DOptions by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 3D보트 선택
        SettingsRow(
            title = "3D보트 선택",
            value = if (systemSettings.boat3DEnabled) "3D" else "2D",
            onClick = { showBoat3DOptions = !showBoat3DOptions }
        )
        if (showBoat3DOptions) {
            listOf("2D", "3D").forEach { option ->
                Text(
                    option,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateBoat3DEnabled(option == "3D")
                            showBoat3DOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if ((option == "3D" && systemSettings.boat3DEnabled) || 
                                (option == "2D" && !systemSettings.boat3DEnabled)) 
                        Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 거리원
        SettingsRow(
            title = "거리원",
            value = "${systemSettings.distanceCircleRadius.toInt()}m",
            onClick = { showDistanceCircleDialog = true }
        )
        
        // 해딩연장
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateHeadingLineEnabled(!systemSettings.headingLineEnabled)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "해딩연장",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.headingLineEnabled,
                onCheckedChange = { viewModel.updateHeadingLineEnabled(it) }
            )
        }
        
        // 코스 연장
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateCourseLineEnabled(!systemSettings.courseLineEnabled)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "코스 연장",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.courseLineEnabled,
                onCheckedChange = { viewModel.updateCourseLineEnabled(it) }
            )
        }
        
        // 연장 길이
        SettingsRow(
            title = "연장 길이",
            value = "${systemSettings.extensionLength.toInt()}m",
            onClick = { showExtensionLengthDialog = true }
        )
        
        // 위경도선
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateGridLineEnabled(!systemSettings.gridLineEnabled)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "위경도선",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.gridLineEnabled,
                onCheckedChange = { viewModel.updateGridLineEnabled(it) }
            )
        }
        
        Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        
        // 목적지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateDestinationVisible(!systemSettings.destinationVisible)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "목적지",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.destinationVisible,
                onCheckedChange = { viewModel.updateDestinationVisible(it) }
            )
        }
        
        // 경로
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateRouteVisible(!systemSettings.routeVisible)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "경로",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.routeVisible,
                onCheckedChange = { viewModel.updateRouteVisible(it) }
            )
        }
        
        // 항적
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateTrackVisible(!systemSettings.trackVisible)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "항적",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.trackVisible,
                onCheckedChange = { viewModel.updateTrackVisible(it) }
            )
        }
        
        // 지도 감춤
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateMapHidden(!systemSettings.mapHidden)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "지도 감춤",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.mapHidden,
                onCheckedChange = { viewModel.updateMapHidden(it) }
            )
        }
    }
    
    // 거리원 설정 다이얼로그
    if (showDistanceCircleDialog) {
        var radiusText by remember { mutableStateOf(systemSettings.distanceCircleRadius.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showDistanceCircleDialog = false },
            title = { Text("거리 가늠원 설정", color = Color.White) },
            text = {
                Column {
                    Text("거리원 반경 (미터)", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
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
                            viewModel.updateDistanceCircleRadius(it.toFloat())
                            showDistanceCircleDialog = false
                        }
                    }
                ) {
                    Text("확인", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDistanceCircleDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
    
    // 연장 길이 설정 다이얼로그
    if (showExtensionLengthDialog) {
        var lengthText by remember { mutableStateOf(systemSettings.extensionLength.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showExtensionLengthDialog = false },
            title = { Text("연장선 길이 설정", color = Color.White) },
            text = {
                Column {
                    Text("연장선 길이 (미터)", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = lengthText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) lengthText = it },
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
                        lengthText.toIntOrNull()?.let {
                            viewModel.updateExtensionLength(it.toFloat())
                            showExtensionLengthDialog = false
                        }
                    }
                ) {
                    Text("확인", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtensionLengthDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
}

@Composable
private fun WirelessSettingsContent(viewModel: SettingsViewModel) {
    val systemSettings = viewModel.systemSettings
    
    var showBluetoothOptionsDialog by remember { mutableStateOf(false) }
    var showWifiNetworksDialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 블루투스
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateBluetoothEnabled(!systemSettings.bluetoothEnabled)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "블루투스",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.bluetoothEnabled,
                onCheckedChange = { viewModel.updateBluetoothEnabled(it) }
            )
        }
        
        // 블루투스옵션
        SettingsRow(
            title = "블루투스옵션",
            value = "${systemSettings.bluetoothPairedDevices.size}개 장치",
            onClick = { showBluetoothOptionsDialog = true },
            hasSubMenu = true
        )
        
        // WiFi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateWifiEnabled(!systemSettings.wifiEnabled)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WiFi",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.wifiEnabled,
                onCheckedChange = { viewModel.updateWifiEnabled(it) }
            )
        }
        
        // WiFi 네트워크
        SettingsRow(
            title = "WiFi 네트워크",
            value = systemSettings.wifiConnectedNetwork ?: "연결 안됨",
            onClick = { showWifiNetworksDialog = true },
            hasSubMenu = true
        )
    }
    
    // 블루투스 옵션 다이얼로그
    if (showBluetoothOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showBluetoothOptionsDialog = false },
            title = { Text("블루투스 옵션", color = Color.White) },
            text = {
                Column {
                    if (systemSettings.bluetoothPairedDevices.isEmpty()) {
                        Text(
                            "페어링 된 장치가 없습니다.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    } else {
                        systemSettings.bluetoothPairedDevices.forEach { device ->
                            Text(
                                device,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBluetoothOptionsDialog = false }) {
                    Text("확인", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
    
    // WiFi 네트워크 다이얼로그
    if (showWifiNetworksDialog) {
        AlertDialog(
            onDismissRequest = { showWifiNetworksDialog = false },
            title = { Text("WiFi 네트워크", color = Color.White) },
            text = {
                Column {
                    // 더미 WiFi 네트워크 목록
                    val wifiNetworks = listOf(
                        "Network1 (연결됨)",
                        "Network2",
                        "Network3"
                    )
                    wifiNetworks.forEach { network ->
                        Text(
                            network,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWifiNetworksDialog = false }) {
                    Text("확인", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
}

@Composable
private fun NetworkSettingsContent(viewModel: SettingsViewModel) {
    val systemSettings = viewModel.systemSettings
    
    var showNmea2000Dialog by remember { mutableStateOf(false) }
    var showNmea0183Dialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // NMEA2000
        SettingsRow(
            title = "NMEA2000",
            value = if (systemSettings.nmea2000Enabled) "활성화" else "비활성화",
            onClick = { showNmea2000Dialog = true },
            hasSubMenu = true
        )
        
        // NMEA0183
        SettingsRow(
            title = "NMEA0183",
            value = if (systemSettings.nmea0183Enabled) "활성화" else "비활성화",
            onClick = { showNmea0183Dialog = true },
            hasSubMenu = true
        )
    }
    
    // NMEA2000 설정 다이얼로그
    if (showNmea2000Dialog) {
        AlertDialog(
            onDismissRequest = { showNmea2000Dialog = false },
            title = { Text("NMEA 2000 설정", color = Color.White) },
            text = {
                Column {
                    Text(
                        "NMEA 2000 설정 옵션",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("활성화", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = systemSettings.nmea2000Enabled,
                            onCheckedChange = { viewModel.updateNmea2000Enabled(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNmea2000Dialog = false }) {
                    Text("확인", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
    
    // NMEA0183 설정 다이얼로그
    if (showNmea0183Dialog) {
        AlertDialog(
            onDismissRequest = { showNmea0183Dialog = false },
            title = { Text("NMEA 0183 설정", color = Color.White) },
            text = {
                Column {
                    Text(
                        "NMEA 0183 설정 옵션",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("활성화", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = systemSettings.nmea0183Enabled,
                            onCheckedChange = { viewModel.updateNmea0183Enabled(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNmea0183Dialog = false }) {
                    Text("확인", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
}

@Composable
private fun VesselSettingsContent(viewModel: SettingsViewModel) {
    val systemSettings = viewModel.systemSettings
    
    var showMmsiDialog by remember { mutableStateOf(false) }
    var showAisCourseExtensionDialog by remember { mutableStateOf(false) }
    var showVesselTrackingOptions by remember { mutableStateOf(false) }
    var showRecordLengthDialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    // 선박 및 추적 물표 옵션
    val trackingOptions = listOf("관심 선박", "위험물표")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // MMSI
        SettingsRow(
            title = "MMSI",
            value = systemSettings.mmsi.ifEmpty { "미설정" },
            onClick = { showMmsiDialog = true }
        )
        
        // 코스 연장
        SettingsRow(
            title = "코스 연장",
            value = "${systemSettings.aisCourseExtension.toInt()}m",
            onClick = { showAisCourseExtensionDialog = true }
        )
        
        // 선박 및 추적 물표
        SettingsRow(
            title = "선박 및 추적 물표",
            value = null,
            onClick = { showVesselTrackingOptions = !showVesselTrackingOptions },
            hasSubMenu = true
        )
        if (showVesselTrackingOptions) {
            trackingOptions.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val currentValue = systemSettings.vesselTrackingSettings[option] ?: true
                            viewModel.updateVesselTrackingSetting(option, !currentValue)
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = systemSettings.vesselTrackingSettings[option] ?: true,
                        onCheckedChange = { viewModel.updateVesselTrackingSetting(option, it) }
                    )
                }
            }
        }
        
        // 기록 길이
        SettingsRow(
            title = "기록 길이",
            value = "${systemSettings.recordLength}분",
            onClick = { showRecordLengthDialog = true }
        )
    }
    
    // MMSI 설정 다이얼로그
    if (showMmsiDialog) {
        var mmsiText by remember { mutableStateOf(systemSettings.mmsi) }
        AlertDialog(
            onDismissRequest = { showMmsiDialog = false },
            title = { Text("MMSI 번호 설정", color = Color.White) },
            text = {
                Column {
                    Text("MMSI 번호", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = mmsiText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) mmsiText = it },
                        label = { Text("MMSI") },
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
                        viewModel.updateMmsi(mmsiText)
                        showMmsiDialog = false
                    }
                ) {
                    Text("확인", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMmsiDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
    
    // AIS 코스 연장 설정 다이얼로그
    if (showAisCourseExtensionDialog) {
        var extensionText by remember { mutableStateOf(systemSettings.aisCourseExtension.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showAisCourseExtensionDialog = false },
            title = { Text("AIS 코스 연장 설정", color = Color.White) },
            text = {
                Column {
                    Text("AIS 코스 연장 (미터)", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = extensionText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) extensionText = it },
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
                        extensionText.toIntOrNull()?.let {
                            viewModel.updateAisCourseExtension(it.toFloat())
                            showAisCourseExtensionDialog = false
                        }
                    }
                ) {
                    Text("확인", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAisCourseExtensionDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
    
    // 기록 길이 설정 다이얼로그
    if (showRecordLengthDialog) {
        var lengthText by remember { mutableStateOf(systemSettings.recordLength.toString()) }
        AlertDialog(
            onDismissRequest = { showRecordLengthDialog = false },
            title = { Text("기록 길이 설정", color = Color.White) },
            text = {
                Column {
                    Text("기록 길이 (분)", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    TextField(
                        value = lengthText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) lengthText = it },
                        label = { Text("분") },
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
                        lengthText.toIntOrNull()?.let {
                            viewModel.updateRecordLength(it)
                            showRecordLengthDialog = false
                        }
                    }
                ) {
                    Text("확인", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordLengthDialog = false }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
}

@Composable
private fun CameraSettingsContent(viewModel: SettingsViewModel) {
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
private fun AlertSettingsContent(viewModel: SettingsViewModel) {
    val systemSettings = viewModel.systemSettings
    
    var showAlertHistoryDialog by remember { mutableStateOf(false) }
    var showAlertActiveDialog by remember { mutableStateOf(false) }
    var showAlertSettingsOptions by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    // 더미 경보 목록
    val alertTypes = listOf("XTE 경보", "도착 경보", "수심 경보", "연료 경보", "엔진 경보")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 기록
        SettingsRow(
            title = "기록",
            value = null,
            onClick = { showAlertHistoryDialog = true },
            hasSubMenu = true
        )
        
        // 작동
        SettingsRow(
            title = "작동",
            value = null,
            onClick = { showAlertActiveDialog = true },
            hasSubMenu = true
        )
        
        // 설정
        SettingsRow(
            title = "설정",
            value = null,
            onClick = { showAlertSettingsOptions = !showAlertSettingsOptions },
            hasSubMenu = true
        )
        if (showAlertSettingsOptions) {
            alertTypes.forEach { alertType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val currentValue = systemSettings.alertSettings[alertType] ?: true
                            viewModel.updateAlertSetting(alertType, !currentValue)
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        alertType,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = systemSettings.alertSettings[alertType] ?: true,
                        onCheckedChange = { viewModel.updateAlertSetting(alertType, it) }
                    )
                }
            }
        }
        
        // 경보 사용 가능
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateAlertEnabled(!systemSettings.alertEnabled)
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "경보 사용 가능",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = systemSettings.alertEnabled,
                onCheckedChange = { viewModel.updateAlertEnabled(it) }
            )
        }
    }
    
    // 경보 기록 다이얼로그
    if (showAlertHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showAlertHistoryDialog = false },
            title = { Text("경보 기록", color = Color.White) },
            text = {
                Column {
                    // 더미 경보 기록 데이터
                    val alertHistory = listOf(
                        "2024-01-15 10:30 - XTE 경보",
                        "2024-01-15 09:15 - 도착 경보",
                        "2024-01-14 16:45 - 수심 경보"
                    )
                    alertHistory.forEach { history ->
                        Text(
                            history,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertHistoryDialog = false }) {
                    Text("확인", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
    
    // 현재 경보 다이얼로그
    if (showAlertActiveDialog) {
        AlertDialog(
            onDismissRequest = { showAlertActiveDialog = false },
            title = { Text("현재 경보", color = Color.White) },
            text = {
                Column {
                    // 더미 현재 경보 데이터
                    val activeAlerts = listOf(
                        "XTE 경보: 활성화",
                        "도착 경보: 비활성화",
                        "수심 경보: 활성화"
                    )
                    activeAlerts.forEach { alert ->
                        Text(
                            alert,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertActiveDialog = false }) {
                    Text("확인", color = Color.White)
                }
            },
            containerColor = Color(0xFF2A2A2A),
            textContentColor = Color.White
        )
    }
}

@Composable
private fun UnitSettingsContent(viewModel: SettingsViewModel) {
    val systemSettings = viewModel.systemSettings
    
    var showDistanceUnitOptions by remember { mutableStateOf(false) }
    var showSmallDistanceUnitOptions by remember { mutableStateOf(false) }
    var showSpeedUnitOptions by remember { mutableStateOf(false) }
    var showWindSpeedUnitOptions by remember { mutableStateOf(false) }
    var showDepthUnitOptions by remember { mutableStateOf(false) }
    var showAltitudeUnitOptions by remember { mutableStateOf(false) }
    var showAltitudeDatumOptions by remember { mutableStateOf(false) }
    var showHeadingUnitOptions by remember { mutableStateOf(false) }
    var showTemperatureUnitOptions by remember { mutableStateOf(false) }
    var showCapacityUnitOptions by remember { mutableStateOf(false) }
    var showFuelEfficiencyUnitOptions by remember { mutableStateOf(false) }
    var showPressureUnitOptions by remember { mutableStateOf(false) }
    var showAtmosphericPressureUnitOptions by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 거리
        SettingsRow(
            title = "거리",
            value = systemSettings.distanceUnit,
            onClick = { showDistanceUnitOptions = !showDistanceUnitOptions }
        )
        if (showDistanceUnitOptions) {
            listOf("nm", "km", "mi").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateDistanceUnit(unit)
                            showDistanceUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.distanceUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 거리소
        SettingsRow(
            title = "거리소",
            value = systemSettings.smallDistanceUnit,
            onClick = { showSmallDistanceUnitOptions = !showSmallDistanceUnitOptions }
        )
        if (showSmallDistanceUnitOptions) {
            listOf("m", "ft", "yd").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSmallDistanceUnit(unit)
                            showSmallDistanceUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.smallDistanceUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 선속
        SettingsRow(
            title = "선속",
            value = systemSettings.speedUnit,
            onClick = { showSpeedUnitOptions = !showSpeedUnitOptions }
        )
        if (showSpeedUnitOptions) {
            listOf("노트", "시속", "mph").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSpeedUnit(unit)
                            showSpeedUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.speedUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 풍속
        SettingsRow(
            title = "풍속",
            value = systemSettings.windSpeedUnit,
            onClick = { showWindSpeedUnitOptions = !showWindSpeedUnitOptions }
        )
        if (showWindSpeedUnitOptions) {
            listOf("노트", "시속", "mph").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateWindSpeedUnit(unit)
                            showWindSpeedUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.windSpeedUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 수심
        SettingsRow(
            title = "수심",
            value = systemSettings.depthUnit,
            onClick = { showDepthUnitOptions = !showDepthUnitOptions }
        )
        if (showDepthUnitOptions) {
            listOf("m", "ft", "패덤").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateDepthUnit(unit)
                            showDepthUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.depthUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 고도
        SettingsRow(
            title = "고도",
            value = systemSettings.altitudeUnit,
            onClick = { showAltitudeUnitOptions = !showAltitudeUnitOptions }
        )
        if (showAltitudeUnitOptions) {
            listOf("m", "ft").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateAltitudeUnit(unit)
                            showAltitudeUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.altitudeUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 고도데이텀
        SettingsRow(
            title = "고도데이텀",
            value = systemSettings.altitudeDatum,
            onClick = { showAltitudeDatumOptions = !showAltitudeDatumOptions }
        )
        if (showAltitudeDatumOptions) {
            listOf("지오이드", "wgs-84").forEach { datum ->
                Text(
                    datum,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateAltitudeDatum(datum)
                            showAltitudeDatumOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.altitudeDatum == datum) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 헤딩
        SettingsRow(
            title = "헤딩",
            value = systemSettings.headingUnit,
            onClick = { showHeadingUnitOptions = !showHeadingUnitOptions }
        )
        if (showHeadingUnitOptions) {
            listOf("M", "T").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateHeadingUnit(unit)
                            showHeadingUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.headingUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 수온
        SettingsRow(
            title = "수온",
            value = systemSettings.temperatureUnit,
            onClick = { showTemperatureUnitOptions = !showTemperatureUnitOptions }
        )
        if (showTemperatureUnitOptions) {
            listOf("C", "F").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateTemperatureUnit(unit)
                            showTemperatureUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.temperatureUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 용량
        SettingsRow(
            title = "용량",
            value = systemSettings.capacityUnit,
            onClick = { showCapacityUnitOptions = !showCapacityUnitOptions }
        )
        if (showCapacityUnitOptions) {
            listOf("L", "gal", "m³").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateCapacityUnit(unit)
                            showCapacityUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.capacityUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 연비
        SettingsRow(
            title = "연비",
            value = systemSettings.fuelEfficiencyUnit,
            onClick = { showFuelEfficiencyUnitOptions = !showFuelEfficiencyUnitOptions }
        )
        if (showFuelEfficiencyUnitOptions) {
            listOf("L/h", "gal/h", "L/100km").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateFuelEfficiencyUnit(unit)
                            showFuelEfficiencyUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.fuelEfficiencyUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 압력
        SettingsRow(
            title = "압력",
            value = systemSettings.pressureUnit,
            onClick = { showPressureUnitOptions = !showPressureUnitOptions }
        )
        if (showPressureUnitOptions) {
            listOf("bar", "psi", "kPa").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updatePressureUnit(unit)
                            showPressureUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.pressureUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
        
        // 기압
        SettingsRow(
            title = "기압",
            value = systemSettings.atmosphericPressureUnit,
            onClick = { showAtmosphericPressureUnitOptions = !showAtmosphericPressureUnitOptions }
        )
        if (showAtmosphericPressureUnitOptions) {
            listOf("hPa", "mmHg", "inHg").forEach { unit ->
                Text(
                    unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateAtmosphericPressureUnit(unit)
                            showAtmosphericPressureUnitOptions = false
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    color = if (systemSettings.atmosphericPressureUnit == unit) Color(0xFFFFD700) else Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

