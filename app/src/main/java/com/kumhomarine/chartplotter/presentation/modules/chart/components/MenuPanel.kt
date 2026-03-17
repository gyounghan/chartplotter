package com.kumhomarine.chartplotter.presentation.modules.chart.components

import android.util.Log
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.R
import com.kumhomarine.chartplotter.*
import com.kumhomarine.chartplotter.SavedPoint
import com.kumhomarine.chartplotter.data.models.Route
import com.kumhomarine.chartplotter.data.models.RoutePoint
import com.kumhomarine.chartplotter.presentation.viewmodel.MainViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.SettingsViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.TrackViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.RouteViewModel
import com.kumhomarine.chartplotter.PMTilesLoader
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.geometry.LatLng
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop

private val NavyColor = Color(0xFF001F3F)
private val NavyCardColor = Color(0xE6001F3F)
private val RouteCardBg = Color(0xFFF5F5F5)

/**
 * 메뉴 패널 컴포넌트
 */
@Composable
fun MenuPanel(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    trackViewModel: TrackViewModel,
    routeViewModel: RouteViewModel,
    mapLibreMap: MapLibreMap?,
    locationManager: LocationManager?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    getNextAvailablePointNumber: () -> Int,
    onDeletePoint: (SavedPoint) -> Unit,
    onEditPoint: (SavedPoint) -> Unit,
    updateMapRotation: () -> Unit,
    updateTrackDisplay: () -> Unit,
    activity: ComponentActivity? = null
) {
    val mapUiState = viewModel.mapUiState
    val trackUiState = trackViewModel.trackUiState
    val routeUiState = routeViewModel.routeUiState

    // 경로 생성 모드일 때는 메뉴 사이드바 숨김
    if (mapUiState.showMenu && !routeUiState.isEditingRoute) {
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
                    .background(Color(0xFF0A1628))
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
                                "main" -> stringResource(R.string.menu)
                                "point" -> stringResource(R.string.menu_marker)
                                "ais" -> stringResource(R.string.menu_ais)
                                "navigation" -> stringResource(R.string.menu_navigation)
                                "track" -> stringResource(R.string.menu_track)
                                "display" -> stringResource(R.string.menu_display)
                                "route" -> stringResource(R.string.menu_route)
                                else -> stringResource(R.string.menu)
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
                                contentDescription = if (mapUiState.currentMenu == "main") stringResource(R.string.menu_close) else stringResource(R.string.menu_back),
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 메인 메뉴
                    if (mapUiState.currentMenu == "main") {
                        MenuMainContent(viewModel)
                    }

                    // 마커(포인트) 메뉴
                    if (mapUiState.currentMenu == "point") {
                        MenuPointContent(
                            viewModel = viewModel,
                            mapLibreMap = mapLibreMap,
                            loadPointsFromLocal = loadPointsFromLocal,
                            getNextAvailablePointNumber = getNextAvailablePointNumber,
                            onDeletePoint = onDeletePoint,
                            onEditPoint = onEditPoint
                        )
                    }

                    // 항해 메뉴
                    if (mapUiState.currentMenu == "navigation") {
                        MenuNavigationContent(
                            viewModel = viewModel,
                            routeViewModel = routeViewModel,
                            mapLibreMap = mapLibreMap,
                            loadPointsFromLocal = loadPointsFromLocal,
                            locationManager = locationManager
                        )
                    }

                    // 항적 메뉴
                    if (mapUiState.currentMenu == "track") {
                        MenuTrackContent(
                            viewModel = viewModel,
                            trackViewModel = trackViewModel,
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
                            routeViewModel = routeViewModel,
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
        stringResource(R.string.menu_marker),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("point") },
        color = Color.White
    )
    Text(
        stringResource(R.string.menu_navigation),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("navigation") },
        color = Color.White
    )
    Text(
        stringResource(R.string.menu_track),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("track") },
        color = Color.White
    )
    Text(
        stringResource(R.string.menu_ais),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("ais") },
        color = Color.White
    )
    Text(
        stringResource(R.string.menu_display),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("display") },
        color = Color.White
    )
    Text(
        stringResource(R.string.menu_route),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("route") },
        color = Color.White
    )
    Text(
        stringResource(R.string.menu_settings),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowSettingsScreen(true)
            },
        color = Color.White
    )
}

@Composable
private fun MenuPointContent(
    viewModel: MainViewModel,
    mapLibreMap: MapLibreMap?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    getNextAvailablePointNumber: () -> Int,
    onDeletePoint: (SavedPoint) -> Unit,
    onEditPoint: (SavedPoint) -> Unit
) {
    val mapUiState = viewModel.mapUiState
    val latitudeLabel = stringResource(R.string.latitude)
    val longitudeLabel = stringResource(R.string.longitude)
    val coordsUnavailable = stringResource(R.string.coords_unavailable)
    
    Text(
        stringResource(R.string.marker_create),
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
                        "$latitudeLabel: ${String.format("%.6f", latLng.latitude)}\n$longitudeLabel: ${String.format("%.6f", latLng.longitude)}"
                    )
                    viewModel.updatePointName("Point${getNextAvailablePointNumber()}")
                    viewModel.updateSelectedColor(Color.Red)
                } ?: run {
                    viewModel.updateCenterCoordinates(coordsUnavailable)
                    viewModel.updateCurrentLatLng(null)
                }
                viewModel.updateShowDialog(true)
            },
        color = Color.White
    )
    val points = loadPointsFromLocal()
    if (points.isEmpty()) {
        Text(
            stringResource(R.string.point_edit_empty),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(points) { point ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = RouteCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = point.name,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)}",
                                fontSize = 11.sp,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                viewModel.updateSelectedPoint(point)
                                viewModel.updateEditPointName(point.name)
                                viewModel.updateEditSelectedColor(point.color)
                                viewModel.updateShowPointEditSelectionDialog(false)
                                viewModel.updateShowEditDialog(true)
                                onEditPoint(point)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = NavyColor
                                )
                            }
                            IconButton(onClick = {
                                onDeletePoint(point)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
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

@Composable
private fun MenuNavigationContent(
    viewModel: MainViewModel,
    routeViewModel: RouteViewModel,
    mapLibreMap: MapLibreMap?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    locationManager: LocationManager?
) {
    val mapUiState = viewModel.mapUiState
    val isNavigating = mapUiState.navigationPoint != null || mapUiState.coursePoint != null
    
    if (!isNavigating) {
        Text(
            stringResource(R.string.nav_start_from_marker),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable {
                    val savedPoints = loadPointsFromLocal()
                    if (savedPoints.isNotEmpty()) {
                        viewModel.updateShowPointSelectionDialog(true)
                    } else {
                        Log.d("[MenuPanel]", "저장된 마커가 없어서 항해를 시작할 수 없습니다.")
                    }
                },
            color = Color.White
        )
        Text(
            stringResource(R.string.nav_start_from_route),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable {
                    viewModel.updateShowRouteSelectionForNavDialog(true)
                },
            color = Color.White
        )
    } else {
        Text(
            stringResource(R.string.dest_change),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    val savedPoints = loadPointsFromLocal()
                    if (savedPoints.isNotEmpty()) {
                        viewModel.updateShowPointSelectionDialog(true)
                    } else {
                        Log.d("[MenuPanel]", "저장된 마커가 없어서 목적지를 변경할 수 없습니다.")
                    }
                },
            color = Color.White
        )
        Text(
            stringResource(R.string.waypoint_manage),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    viewModel.updateShowWaypointDialog(true)
                },
            color = Color.White
        )
        Text(
            stringResource(R.string.nav_stop),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    viewModel.updateMapDisplayMode("노스업")
                    viewModel.updateCoursePoint(null)
                    viewModel.updateNavigationPoint(null)
                    viewModel.updateWaypoints(emptyList())
                    viewModel.clearNavigationRoute()
                    mapLibreMap?.let { map ->
                        PMTilesLoader.removeNavigationLine(map)
                        PMTilesLoader.removeNavigationMarker(map)
                    }
                },
            color = Color.White
        )
    }
    
    if (mapUiState.mapDisplayMode == "코스업" && (mapUiState.coursePoint != null || mapUiState.navigationPoint != null)) {
        Text(
            text = "${stringResource(R.string.navigation_in_progress)}: ${mapUiState.coursePoint?.name ?: mapUiState.navigationPoint?.name ?: stringResource(R.string.cursor_position)}",
            fontSize = 14.sp,
            color = Color.Yellow,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MenuTrackContent(
    viewModel: MainViewModel,
    trackViewModel: TrackViewModel,
    trackUiState: com.kumhomarine.chartplotter.presentation.viewmodel.TrackUiState,
    updateTrackDisplay: () -> Unit
) {
    Text(
        stringResource(R.string.track_create),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowTrackCreateDialog(true)
            },
        color = Color.White
    )
    Text(
        stringResource(R.string.track_list),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    val tracks = remember { mutableStateOf(trackViewModel.getTracks()) }
    LaunchedEffect(Unit) {
        tracks.value = trackViewModel.getTracks()
    }
    if (tracks.value.isEmpty()) {
        Text(
            stringResource(R.string.no_tracks_saved),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(tracks.value) { track ->
                val isRecording = trackUiState.recordingTracks.containsKey(track.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = RouteCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.points_count, track.points.size),
                                fontSize = 11.sp,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                trackViewModel.updateSelectedTrackForSettings(track)
                                viewModel.updateShowTrackSettingsDialog(true)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = NavyColor
                                )
                            }
                            IconButton(onClick = {
                                if (isRecording) trackViewModel.stopTrackRecording(track.id)
                                trackViewModel.deleteTrack(track.id)
                                tracks.value = trackViewModel.getTracks()
                                updateTrackDisplay()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = Color.Red
                                )
                            }
                            IconButton(onClick = {
                                trackViewModel.toggleTrackRecording(track.id)
                                tracks.value = trackViewModel.getTracks()
                                updateTrackDisplay()
                            }) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isRecording) stringResource(R.string.track_stop_recording) else "기록 시작",
                                    tint = if (isRecording) Color.Red else Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuAisContent(viewModel: MainViewModel) {
    val mapUiState = viewModel.mapUiState
    Text(
        stringResource(R.string.ais_toggle),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateAisVisible(!mapUiState.aisVisible)
            },
        color = if (mapUiState.aisVisible) Color.Yellow else Color.White
    )
}

@Composable
private fun MenuDisplayContent(
    viewModel: MainViewModel,
    mapUiState: com.kumhomarine.chartplotter.presentation.viewmodel.MapUiState,
    loadPointsFromLocal: () -> List<SavedPoint>,
    updateMapRotation: () -> Unit
) {
    Text(
        stringResource(R.string.mode_north_up),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 노스업")
                viewModel.updateMapDisplayMode("노스업")
            },
        color = if (mapUiState.mapDisplayMode == "노스업") Color.Yellow else Color.White
    )
    Text(
        stringResource(R.string.mode_heading_up),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 헤딩업")
                viewModel.updateMapDisplayMode("헤딩업")
            },
        color = if (mapUiState.mapDisplayMode == "헤딩업") Color.Yellow else Color.White
    )
    Text(
        stringResource(R.string.mode_course_up),
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
            },
        color = if (mapUiState.mapDisplayMode == "코스업") Color.Yellow else Color.White
    )
}

// 시스템 설정은 SystemSetting 앱에서만 제공됩니다.

@Composable
private fun MenuRouteContent(
    viewModel: MainViewModel,
    routeViewModel: RouteViewModel,
    settingsViewModel: SettingsViewModel,
    mapLibreMap: MapLibreMap?,
    locationManager: LocationManager?
) {
    val mapUiState = viewModel.mapUiState
    val routes = remember { mutableStateOf(routeViewModel.getAllRoutes()) }
    
    // 경로 목록 새로고침
    LaunchedEffect(Unit) {
        routes.value = routeViewModel.getAllRoutes()
    }
    
    Text(
        stringResource(R.string.route_create),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // 경로 생성 설명 다이얼로그 표시
                routeViewModel.updateShowRouteCreateDialog(true)
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
            stringResource(R.string.route_display),
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
        stringResource(R.string.route_list),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    if (routes.value.isEmpty()) {
        Text(
            stringResource(R.string.no_routes_saved),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
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
                        containerColor = RouteCardBg
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = route.name,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.points_count, route.points.size),
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            // 편집
                            IconButton(
                                onClick = {
                                    routeViewModel.selectRoute(route)
                                    routeViewModel.setEditingRoute(true)
                                    routeViewModel.setEditingRoutePoints(route.points)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = NavyColor
                                )
                            }
                            
                            // 삭제
                            IconButton(
                                onClick = {
                                    routeViewModel.deleteRoute(route.id)
                                    routes.value = routeViewModel.getAllRoutes()
                                    // 지도에서도 제거
                                    mapLibreMap?.let { map ->
                                        PMTilesLoader.removeRouteLine(map, route.id)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
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

