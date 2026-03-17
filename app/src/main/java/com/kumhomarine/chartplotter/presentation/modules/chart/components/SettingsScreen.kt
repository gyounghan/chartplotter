package com.kumhomarine.chartplotter.presentation.modules.chart.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.R
import com.kumhomarine.chartplotter.presentation.viewmodel.MainViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.SettingsViewModel

/** 설정 화면용 TextField 색상 - 검정 배경, 흰색 텍스트 */
@Composable
private fun rememberSettingsTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF1A1A1A),
    unfocusedContainerColor = Color(0xFF1A1A1A),
    focusedLabelColor = Color.Gray,
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color.White,
    focusedIndicatorColor = Color.Gray,
    unfocusedIndicatorColor = Color.Gray
)

/**
 * 설정 화면 - 지도·선박 설정만 제공
 * 좌측: 지도 / 선박 카테고리
 * 우측: 선택된 카테고리의 세부 옵션
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("지도") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 좌측: 지도 / 선박 카테고리
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1A1A))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                SettingsCategoryItem(
                    title = "지도",
                    icon = Icons.Default.Map,
                    isSelected = selectedCategory == "지도",
                    onClick = { selectedCategory = "지도" }
                )
                SettingsCategoryItem(
                    title = "선박",
                    icon = Icons.Default.DirectionsBoat,
                    isSelected = selectedCategory == "선박",
                    onClick = { selectedCategory = "선박" }
                )
            }

            // 우측: 세부 옵션
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
            ) {
                when (selectedCategory) {
                    "지도" -> MapSettingsContent(settingsViewModel)
                    "선박" -> VesselSettingsContent(settingsViewModel)
                    else -> MapSettingsContent(settingsViewModel)
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

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
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
                        (option == "2D" && !systemSettings.boat3DEnabled)
                    ) Color(0xFFFFD700) else Color.White,
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
                        colors = rememberSettingsTextFieldColors()
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
                        colors = rememberSettingsTextFieldColors()
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
private fun VesselSettingsContent(viewModel: SettingsViewModel) {
    val systemSettings = viewModel.systemSettings

    var showAisCourseExtensionDialog by remember { mutableStateOf(false) }
    var showVesselTrackingOptions by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val trackingOptions = listOf("관심 선박", "위험물표")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 코스 연장 (ChartPlotter 로컬 저장)
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
                        colors = rememberSettingsTextFieldColors()
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
}
