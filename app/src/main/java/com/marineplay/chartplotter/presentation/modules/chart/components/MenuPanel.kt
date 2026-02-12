package com.marineplay.chartplotter.presentation.modules.chart.components

import android.util.Log
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.*
import com.marineplay.chartplotter.SavedPoint
import com.marineplay.chartplotter.data.models.Route
import com.marineplay.chartplotter.data.models.RoutePoint
import com.marineplay.chartplotter.viewmodel.MainViewModel
import com.marineplay.chartplotter.viewmodel.SettingsViewModel
import com.marineplay.chartplotter.PMTilesLoader
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.geometry.LatLng
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow

/**
 * 메뉴 패널 컴포넌트
 */
@Composable
fun MenuPanel(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    mapLibreMap: MapLibreMap?,
    locationManager: LocationManager?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    getNextAvailablePointNumber: () -> Int,
    updateMapRotation: () -> Unit,
    updateTrackDisplay: () -> Unit,
    activity: ComponentActivity? = null
) {
    val mapUiState = viewModel.mapUiState
    val trackUiState = viewModel.trackUiState

    if (mapUiState.showMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(Color.DarkGray)
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { /* 메뉴창 내부 클릭 시 지도 클릭 이벤트 차단 */ }
                    }
            ) {
                Column {
                    // 메뉴 헤더 (제목 + 닫기/뒤로가기 버튼)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (mapUiState.currentMenu) {
                                "main" -> "메뉴"
                                "point" -> "포인트"
                                "ais" -> "AIS"
                                "navigation" -> "항해"
                                "track" -> "항적"
                                "display" -> "화면표시"
                                "route" -> "경로"
                                else -> "메뉴"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = {
                                when {
                                    mapUiState.currentMenu == "main" -> {
                                        viewModel.updateShowMenu(false)
                                        viewModel.updateCurrentMenu("main")
                                    }
                                    else -> {
                                        viewModel.updateCurrentMenu("main")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (mapUiState.currentMenu == "main") Icons.Default.Close else Icons.Default.ArrowBack,
                                contentDescription = if (mapUiState.currentMenu == "main") "메뉴 닫기" else "뒤로가기",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 메인 메뉴
                    if (mapUiState.currentMenu == "main") {
                        MenuMainContent(viewModel)
                    }

                    // 포인트 메뉴
                    if (mapUiState.currentMenu == "point") {
                        MenuPointContent(
                            viewModel = viewModel,
                            mapLibreMap = mapLibreMap,
                            loadPointsFromLocal = loadPointsFromLocal,
                            getNextAvailablePointNumber = getNextAvailablePointNumber
                        )
                    }

                    // 항해 메뉴
                    if (mapUiState.currentMenu == "navigation") {
                        MenuNavigationContent(
                            viewModel = viewModel,
                            mapLibreMap = mapLibreMap,
                            loadPointsFromLocal = loadPointsFromLocal,
                            locationManager = locationManager
                        )
                    }

                    // 항적 메뉴
                    if (mapUiState.currentMenu == "track") {
                        MenuTrackContent(
                            viewModel = viewModel,
                            trackUiState = trackUiState,
                            updateTrackDisplay = updateTrackDisplay
                        )
                    }

                    // AIS 메뉴
                    if (mapUiState.currentMenu == "ais") {
                        MenuAisContent(viewModel)
                    }

                    // 화면표시 방법설정 메뉴
                    if (mapUiState.currentMenu == "display") {
                        MenuDisplayContent(
                            viewModel = viewModel,
                            mapUiState = mapUiState,
                            loadPointsFromLocal = loadPointsFromLocal,
                            updateMapRotation = updateMapRotation
                        )
                    }

                    // 경로 메뉴
                    if (mapUiState.currentMenu == "route") {
                        MenuRouteContent(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            mapLibreMap = mapLibreMap,
                            locationManager = locationManager
                        )
                    }

                    // 시스템 설정은 SystemSetting 앱에서만 제공됩니다.
                }
            }
        }
    }
}

@Composable
private fun MenuMainContent(viewModel: MainViewModel) {
    Text(
        "포인트",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("point") },
        color = Color.White
    )
    Text(
        "항해",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("navigation") },
        color = Color.White
    )
    Text(
        "항적",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("track") },
        color = Color.White
    )
    Text(
        "AIS",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("ais") },
        color = Color.White
    )
    Text(
        "화면표시 방법설정",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("display") },
        color = Color.White
    )
    Text(
        "경로",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("route") },
        color = Color.White
    )
    // 고급 설정은 SystemSetting 앱에서만 제공됩니다.
}

@Composable
private fun MenuPointContent(
    viewModel: MainViewModel,
    mapLibreMap: MapLibreMap?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    getNextAvailablePointNumber: () -> Int
) {
    val mapUiState = viewModel.mapUiState
    
    Text(
        "포인트 생성",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val targetLatLng =
                    if (mapUiState.showCursor && mapUiState.cursorLatLng != null) {
                        mapUiState.cursorLatLng
                    } else {
                        mapLibreMap?.cameraPosition?.target
                    }

                targetLatLng?.let { latLng ->
                    viewModel.updateCurrentLatLng(latLng)
                    viewModel.updateCenterCoordinates(
                        "위도: ${String.format("%.6f", latLng.latitude)}\n경도: ${String.format("%.6f", latLng.longitude)}"
                    )
                    viewModel.updatePointName("Point${getNextAvailablePointNumber()}")
                    viewModel.updateSelectedColor(Color.Red)
                } ?: run {
                    viewModel.updateCenterCoordinates("좌표를 가져올 수 없습니다.")
                    viewModel.updateCurrentLatLng(null)
                }
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                viewModel.updateShowDialog(true)
            },
        color = Color.White
    )
    Text(
        "포인트 삭제",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                viewModel.updateShowPointDeleteList(true)
            },
        color = Color.White
    )
    Text(
        "포인트 변경",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                // TODO: 포인트 변경 화면 구현
            },
        color = Color.White
    )
}

@Composable
private fun MenuNavigationContent(
    viewModel: MainViewModel,
    mapLibreMap: MapLibreMap?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    locationManager: LocationManager?
) {
    val mapUiState = viewModel.mapUiState
    val routes = remember { mutableStateOf(viewModel.getAllRoutes()) }
    
    // 경로 목록 새로고침
    LaunchedEffect(Unit) {
        routes.value = viewModel.getAllRoutes()
    }
    
    Text(
        "항해 시작",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val savedPoints = loadPointsFromLocal()
                if (savedPoints.isNotEmpty()) {
                    viewModel.updateShowPointSelectionDialog(true)
                } else {
                    Log.d("[MenuPanel]", "저장된 포인트가 없어서 항해를 시작할 수 없습니다.")
                }
            },
        color = Color.White
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        "경로로 항해 시작",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    if (routes.value.isEmpty()) {
        Text(
            "저장된 경로가 없습니다.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(8.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.height(200.dp)
        ) {
            items(routes.value) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            // 실제 GPS 위치를 우선 사용, 없으면 지도 중심점 사용
                            val currentLocation = locationManager?.getCurrentLocationObject()
                            val currentLatLng = if (currentLocation != null) {
                                LatLng(currentLocation.latitude, currentLocation.longitude)
                            } else {
                                mapLibreMap?.cameraPosition?.target
                            }
                            
                            // 항해 시작: 경로상의 가장 가까운 점부터 자동 연결
                            viewModel.setRouteAsNavigation(route, currentLatLng)
                            
                            // 항해 경로 업데이트
                            mapLibreMap?.let { map ->
                                val mapUiState = viewModel.mapUiState
                                val waypoints = mapUiState.waypoints.map {
                                    LatLng(it.latitude, it.longitude)
                                }
                                val destination = mapUiState.navigationPoint
                                
                                if (destination != null) {
                                    // 실제 GPS 위치가 있으면 그것을 사용, 없으면 지도 중심점 사용
                                    val startPoint = currentLatLng ?: map.cameraPosition.target
                                    
                                    if (startPoint != null) {
                                        PMTilesLoader.addNavigationRoute(
                                            map,
                                            startPoint,
                                            waypoints,
                                            LatLng(destination.latitude, destination.longitude)
                                        )
                                        PMTilesLoader.addNavigationMarker(
                                            map,
                                            LatLng(destination.latitude, destination.longitude),
                                            destination.name
                                        )
                                    }
                                }
                            }
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = route.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "포인트 ${route.points.size}개",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "목적지 변경",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val savedPoints = loadPointsFromLocal()
                if (savedPoints.isNotEmpty()) {
                    viewModel.updateShowPointSelectionDialog(true)
                } else {
                    Log.d("[MenuPanel]", "저장된 포인트가 없어서 목적지를 변경할 수 없습니다.")
                }
            },
        color = Color.White
    )
    Text(
        "경유지 관리",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowWaypointDialog(true)
            },
        color = Color.White
    )
    Text(
        "항해 중지",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateMapDisplayMode("노스업")
                viewModel.updateCoursePoint(null)
                viewModel.updateNavigationPoint(null)
                viewModel.updateWaypoints(emptyList())
                viewModel.clearNavigationRoute() // 현재 항해 경로 초기화
                mapLibreMap?.let { map ->
                    PMTilesLoader.removeNavigationLine(map)
                    PMTilesLoader.removeNavigationMarker(map)
                }
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = Color.White
    )
    
    if (mapUiState.mapDisplayMode == "코스업" && (mapUiState.coursePoint != null || mapUiState.navigationPoint != null)) {
        Text(
            text = "항해 중: ${mapUiState.coursePoint?.name ?: mapUiState.navigationPoint?.name ?: "커서 위치"}",
            fontSize = 14.sp,
            color = Color.Yellow,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MenuTrackContent(
    viewModel: MainViewModel,
    trackUiState: com.marineplay.chartplotter.viewmodel.TrackUiState,
    updateTrackDisplay: () -> Unit
) {
    Text(
        "항적 목록",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // 항적 목록: 모든 항적을 보고 관리 (추가, 삭제, 표시/숨김, 기록 on/off)
                viewModel.updateShowTrackListDialog(true)
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = Color.White
    )
    
    if (trackUiState.isRecordingTrack && trackUiState.currentRecordingTrack != null) {
        Text(
            "항적 기록 중지",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val currentTrack = trackUiState.currentRecordingTrack
                    if (currentTrack != null) {
                        viewModel.stopTrackRecording(currentTrack.id)
                        updateTrackDisplay()
                    }
                    viewModel.updateShowMenu(false)
                    viewModel.updateCurrentMenu("main")
                },
            color = Color.Red
        )
        Text(
            text = "기록 중: ${trackUiState.currentRecordingTrack!!.name}",
            fontSize = 14.sp,
            color = Color.Yellow,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MenuAisContent(viewModel: MainViewModel) {
    Text(
        "AIS ON/OFF",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                // TODO: AIS ON/OFF 구현
            },
        color = Color.White
    )
    Text(
        "AIS 설정",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                // TODO: AIS 설정 화면 구현
            },
        color = Color.White
    )
}

@Composable
private fun MenuDisplayContent(
    viewModel: MainViewModel,
    mapUiState: com.marineplay.chartplotter.viewmodel.MapUiState,
    loadPointsFromLocal: () -> List<SavedPoint>,
    updateMapRotation: () -> Unit
) {
    Text(
        "노스업",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 노스업")
                viewModel.updateMapDisplayMode("노스업")
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = if (mapUiState.mapDisplayMode == "노스업") Color.Yellow else Color.White
    )
    Text(
        "헤딩업",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 헤딩업")
                viewModel.updateMapDisplayMode("헤딩업")
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = if (mapUiState.mapDisplayMode == "헤딩업") Color.Yellow else Color.White
    )
    Text(
        "코스업",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 코스업")
                
                if (mapUiState.navigationPoint != null) {
                    viewModel.updateCoursePoint(mapUiState.navigationPoint)
                    viewModel.updateMapDisplayMode("코스업")
                    updateMapRotation()
                    Log.d("[MenuPanel]", "항해 포인트를 코스업으로 적용: ${mapUiState.navigationPoint!!.name}")
                } else {
                    val savedPoints = loadPointsFromLocal()
                    if (savedPoints.isNotEmpty()) {
                        viewModel.updateShowPointSelectionDialog(true)
                    } else {
                        android.util.Log.d("[MenuPanel]", "코스업을 위해 포인트를 먼저 생성하세요")
                    }
                }
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = if (mapUiState.mapDisplayMode == "코스업") Color.Yellow else Color.White
    )
}

// 시스템 설정은 SystemSetting 앱에서만 제공됩니다.

@Composable
private fun MenuRouteContent(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    mapLibreMap: MapLibreMap?,
    locationManager: LocationManager?
) {
    val mapUiState = viewModel.mapUiState
    val routes = remember { mutableStateOf(viewModel.getAllRoutes()) }
    
    // 경로 목록 새로고침
    LaunchedEffect(Unit) {
        routes.value = viewModel.getAllRoutes()
    }
    
    Text(
        "경로 생성",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // 경로 생성 설명 다이얼로그 표시
                viewModel.updateShowRouteCreateDialog(true)
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = Color.White
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 경로 표시 토글
    val systemSettings = settingsViewModel.systemSettings
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "경로 표시",
            color = Color.White,
            fontSize = 14.sp
        )
        Switch(
            checked = systemSettings.routeVisible,
            onCheckedChange = { settingsViewModel.updateRouteVisible(it) }
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        "경로 목록",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    if (routes.value.isEmpty()) {
        Text(
            "저장된 경로가 없습니다.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(8.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.height(300.dp)
        ) {
            items(routes.value) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = route.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "포인트 ${route.points.size}개",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            // 편집
                            IconButton(
                                onClick = {
                                    viewModel.selectRoute(route)
                                    viewModel.setEditingRoute(true)
                                    viewModel.setEditingRoutePoints(route.points)
                                    viewModel.updateShowMenu(false)
                                    viewModel.updateCurrentMenu("main")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "편집",
                                    tint = Color.Blue
                                )
                            }
                            
                            // 삭제
                            IconButton(
                                onClick = {
                                    viewModel.deleteRoute(route.id)
                                    routes.value = viewModel.getAllRoutes()
                                    // 지도에서도 제거
                                    mapLibreMap?.let { map ->
                                        PMTilesLoader.removeRouteLine(map, route.id)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

