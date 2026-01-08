package com.marineplay.chartplotter.ui

import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.*
import com.marineplay.chartplotter.helpers.PointHelper
import com.marineplay.chartplotter.ui.components.dialogs.*
import com.marineplay.chartplotter.ui.components.map.ChartPlotterMap
import com.marineplay.chartplotter.ui.screens.components.MapControls
import com.marineplay.chartplotter.ui.screens.components.MenuPanel
import com.marineplay.chartplotter.ui.screens.components.MapOverlays
import com.marineplay.chartplotter.viewmodel.MainViewModel
import com.marineplay.chartplotter.SavedPoint
import com.marineplay.chartplotter.domain.mappers.PointMapper
import com.marineplay.chartplotter.domain.usecases.UpdateNavigationRouteUseCase
import com.marineplay.chartplotter.ui.utils.ChartPlotterHelpers
import kotlinx.coroutines.delay
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Check
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import org.maplibre.android.geometry.LatLngBounds
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChartOnlyScreen(
    viewModel: MainViewModel,
    activity: ComponentActivity,
    pointHelper: PointHelper,
    trackManager: TrackManager,
    onMapLibreMapChange: (MapLibreMap?) -> Unit = {},
    onLocationManagerChange: (LocationManager?) -> Unit = {}
) {
    // 지도 및 위치 관리자 상태
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var locationManager by remember { mutableStateOf<LocationManager?>(null) }
    
    // UseCase 인스턴스
    val updateNavigationRouteUseCase = remember { UpdateNavigationRouteUseCase() }

    // MainActivity에 mapLibreMap과 locationManager 전달
    LaunchedEffect(mapLibreMap) {
        onMapLibreMapChange(mapLibreMap)
    }

    LaunchedEffect(locationManager) {
        onLocationManagerChange(locationManager)
    }

    // 헬퍼 함수들 (ChartPlotterHelpers 사용)
    fun getNextAvailablePointNumber(): Int {
        return ChartPlotterHelpers.getNextAvailablePointNumber(viewModel)
    }

    fun loadPointsFromLocal(): List<SavedPoint> {
        return ChartPlotterHelpers.loadPointsFromLocal(viewModel)
    }

    fun registerPoint() {
        val pointUiState = viewModel.pointUiState
        pointUiState.currentLatLng?.let { latLng ->
            val autoPointName = "Point${getNextAvailablePointNumber()}"
            val finalPointName =
                if (pointUiState.pointName.isBlank()) autoPointName else pointUiState.pointName

            // UseCase를 통해 포인트 등록
            val savedPoints = viewModel.registerPoint(
                latLng = latLng,
                name = finalPointName,
                color = pointUiState.selectedColor,
                iconType = pointUiState.selectedIconType
            )

            // 지도에 포인트 표시
            mapLibreMap?.getStyle { style ->
                val convertedPoints = PointMapper.toUiPoints(savedPoints)
                locationManager?.updatePointsOnMap(convertedPoints)
            }

            viewModel.updatePointCount(savedPoints.size)
            Log.d("[ChartPlotterScreen]", "포인트 등록 완료: $finalPointName")
            viewModel.updateShowDialog(false)
            viewModel.updateShowCursor(false)
            viewModel.updateCursorLatLng(null)
            viewModel.updateCursorScreenPosition(null)
        }
    }

    fun deletePoint(point: SavedPoint) {
        try {
            // SavedPoint를 PointHelper.SavedPoint로 변환
            val pointHelperPoint = PointMapper.toHelperPoint(point)

            // UseCase를 통해 포인트 삭제
            val savedPoints = viewModel.deletePoint(pointHelperPoint)

            // 지도에 포인트 업데이트
            mapLibreMap?.getStyle { style ->
                val convertedPoints = PointMapper.toUiPoints(savedPoints)
                locationManager?.updatePointsOnMap(convertedPoints)
            }

            viewModel.updatePointCount(savedPoints.size)
            Log.d("[ChartPlotterScreen]", "포인트 삭제 완료: ${point.name}")
            viewModel.updateShowPointManageDialog(false)
        } catch (e: Exception) {
            Log.e("[ChartPlotterScreen]", "포인트 삭제 실패: ${e.message}")
        }
    }

    fun updateCurrentTrackDisplay() {
        val trackUiState = viewModel.trackUiState
        if (trackUiState.trackPoints.isEmpty() || trackUiState.currentRecordingTrack == null) return

        mapLibreMap?.let { map ->
            val points = trackUiState.trackPoints.map { LatLng(it.latitude, it.longitude) }
            PMTilesLoader.addTrackLine(
                map,
                "current_track",
                points,
                trackUiState.currentRecordingTrack!!.color
            )
        }
    }

    fun updateTrackDisplay() {
        mapLibreMap?.let { map ->
            PMTilesLoader.removeAllTracks(map)

            viewModel.getTracks().forEach { track ->
                if (track.isVisible) {
                    track.records.forEach { record ->
                        val points = record.points.map { LatLng(it.latitude, it.longitude) }
                        val trackUiState = viewModel.trackUiState
                        val isHighlighted = trackUiState.highlightedTrackRecord != null &&
                                trackUiState.highlightedTrackRecord!!.first == track.id &&
                                trackUiState.highlightedTrackRecord!!.second == record.id
                        PMTilesLoader.addTrackLine(
                            map,
                            "track_${track.id}_${record.id}",
                            points,
                            track.color,
                            isHighlighted
                        )
                    }
                }
            }

            val trackUiState = viewModel.trackUiState
            if (trackUiState.isRecordingTrack && trackUiState.currentRecordingTrack != null) {
                updateCurrentTrackDisplay()
            }
        }
    }

    fun updatePoint(originalPoint: SavedPoint, newName: String, newColor: Color) {
        try {
            val existingPoints = pointHelper.loadPointsFromLocal().toMutableList()
            val pointIndex = existingPoints.indexOfFirst { it.timestamp == originalPoint.timestamp }

            if (pointIndex != -1) {
                val updatedPoint = existingPoints[pointIndex].copy(
                    name = newName,
                    color = AndroidColor.valueOf(newColor.toArgb())
                )
                existingPoints[pointIndex] = updatedPoint
                pointHelper.savePointsToLocal(existingPoints)

                mapLibreMap?.getStyle { style ->
                    val savedPoints = PointMapper.toUiPoints(existingPoints)
                    locationManager?.updatePointsOnMap(savedPoints)
                }

                Log.d("[ChartPlotterScreen]", "포인트 업데이트 완료: $newName")
            }

            viewModel.updateShowEditDialog(false)
        } catch (e: Exception) {
            Log.e("[ChartPlotterScreen]", "포인트 업데이트 실패: ${e.message}")
        }
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return ChartPlotterHelpers.calculateBearing(lat1, lon1, lat2, lon2)
    }

    fun updateMapRotation() {
        mapLibreMap?.let { map ->
            val mapUiState = viewModel.mapUiState
            when (mapUiState.mapDisplayMode) {
                "노스업" -> {
                    val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(map.cameraPosition.target)
                        .zoom(map.cameraPosition.zoom)
                        .bearing(0.0)
                        .build()
                    map.cameraPosition = newPosition
                    PMTilesLoader.removeCourseLine(map)
                }

                "헤딩업" -> {
                    val heading = locationManager?.getCurrentBearing() ?: 0f
                    val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(map.cameraPosition.target)
                        .zoom(map.cameraPosition.zoom)
                        .bearing(heading.toDouble())
                        .build()
                    map.cameraPosition = newPosition
                    PMTilesLoader.removeCourseLine(map)
                }

                "코스업" -> {
                    mapUiState.coursePoint?.let { point ->
                        val currentLocation = locationManager?.getCurrentLocationObject()
                        if (currentLocation != null) {
                            val bearing = calculateBearing(
                                currentLocation.latitude, currentLocation.longitude,
                                point.latitude, point.longitude
                            )
                            val currentLatLng =
                                LatLng(currentLocation.latitude, currentLocation.longitude)
                            val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(currentLatLng)
                                .zoom(map.cameraPosition.zoom)
                                .bearing(bearing.toDouble())
                                .build()
                            map.cameraPosition = newPosition
                            viewModel.updateShowCursor(false)
                            viewModel.updateCursorLatLng(null)
                            viewModel.updateCursorScreenPosition(null)
                        }
                    }
                }

                else -> {
                    // 기본값: 노스업
                    val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(map.cameraPosition.target)
                        .zoom(map.cameraPosition.zoom)
                        .bearing(0.0)
                        .build()
                    map.cameraPosition = newPosition
                    PMTilesLoader.removeCourseLine(map)
                }
            }
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return ChartPlotterHelpers.calculateDistance(lat1, lon1, lat2, lon2)
    }

    fun calculateScreenDistance(latLng1: LatLng, latLng2: LatLng, map: MapLibreMap): Double {
        return ChartPlotterHelpers.calculateScreenDistance(latLng1, latLng2, map)
    }

    fun startTrackRecording(track: Track) {
        viewModel.startTrackRecording(track)
        Log.d("[ChartPlotterScreen]", "항적 기록 시작: ${track.name}")
    }

    fun stopTrackRecording() {
        val record = viewModel.stopTrackRecording()
        if (record != null) {
            Log.d(
                "[ChartPlotterScreen]",
                "항적 기록 저장 완료: ${viewModel.trackUiState.trackPoints.size}개 점"
            )
            updateTrackDisplay()
        }
        Log.d("[ChartPlotterScreen]", "항적 기록 중지")
    }

    fun addTrackPointIfNeeded(latitude: Double, longitude: Double) {
        val newPoint = viewModel.addTrackPointIfNeeded(latitude, longitude)
        if (newPoint != null) {
            updateCurrentTrackDisplay()
        }
    }


    fun createQuickPoint() {
        Log.d("[ChartPlotterScreen]", "createQuickPoint() 호출됨")
        val mapUiState = viewModel.mapUiState
        val pointUiState = viewModel.pointUiState
        
        Log.d("[ChartPlotterScreen]", "커서 상태: showCursor=${mapUiState.showCursor}, cursorLatLng=${mapUiState.cursorLatLng}")
        
        mapUiState.cursorLatLng?.let { latLng ->
            val autoPointName = "Point${getNextAvailablePointNumber()}"
            Log.d("[ChartPlotterScreen]", "포인트 생성 시작: $autoPointName, 좌표: ${latLng.latitude}, ${latLng.longitude}")

            pointHelper.addPoint(
                autoPointName,
                latLng.latitude,
                latLng.longitude,
                AndroidColor.valueOf(pointUiState.selectedColor.toArgb()),
                pointUiState.selectedIconType
            )

            mapLibreMap?.getStyle { style ->
                val savedPoints = PointMapper.toUiPoints(pointHelper.loadPointsFromLocal())
                locationManager?.updatePointsOnMap(savedPoints)
            }

            viewModel.updatePointCount(pointHelper.loadPointsFromLocal().size)
            Log.d("[ChartPlotterScreen]", "빠른 포인트 생성 완료: $autoPointName")

            viewModel.updateShowCursor(false)
            viewModel.updateCursorLatLng(null)
            viewModel.updateCursorScreenPosition(null)
        } ?: run {
            Log.w("[ChartPlotterScreen]", "커서 좌표가 없어서 포인트를 생성할 수 없습니다.")
        }
    }

    // ViewModel에서 상태 가져오기
    val pointUiState = viewModel.pointUiState
    val mapUiState = viewModel.mapUiState
    val gpsUiState = viewModel.gpsUiState
    val trackUiState = viewModel.trackUiState
    val dialogUiState = viewModel.dialogUiState

    // UI 줌 버튼 상태 관리
    var isZoomInPressed by remember { mutableStateOf(false) }
    var isZoomOutPressed by remember { mutableStateOf(false) }

    // 🚀 UI 줌 인 버튼 롱클릭 반복 확대 (가속도 효과)
    LaunchedEffect(isZoomInPressed) {
        if (isZoomInPressed) {
            var iteration = 0
            while (isZoomInPressed) {
                mapLibreMap?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom + 0.1).coerceAtMost(20.0)

                    // 커서가 있으면 커서 위치를 중앙으로 맞추고 줌 인
                    if (mapUiState.showCursor && mapUiState.cursorLatLng != null) {
                        val cursorLatLngValue = mapUiState.cursorLatLng!!

                        // 커서 위치를 지도 중앙으로 즉시 이동하고 줌 인 (애니메이션 없이)
                        val cameraUpdate =
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                org.maplibre.android.camera.CameraPosition.Builder()
                                    .target(cursorLatLngValue)
                                    .zoom(newZoom)
                                    .build()
                            )
                        map.moveCamera(cameraUpdate) // animateCamera 대신 moveCamera 사용 (즉시 이동)

                        // 커서 화면 위치를 중앙으로 업데이트
                        val centerScreenPoint = map.projection.toScreenLocation(cursorLatLngValue)
                        viewModel.updateCursorScreenPosition(centerScreenPoint)

                        Log.d(
                            "[MainActivity]",
                            "줌 인: 커서 위치(${cursorLatLngValue.latitude}, ${cursorLatLngValue.longitude})를 중앙으로 맞추고 줌 $currentZoom -> $newZoom"
                        )
                    } else {
                        // 커서가 없으면 일반 줌 인
                        val cameraUpdate =
                            org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                        map.animateCamera(cameraUpdate, 300)
                    }
                    Log.d("[MainActivity]", "줌 인: $currentZoom -> $newZoom")
                }

                // 가속도 효과: 처음에는 느리게(500ms), 점점 빨라져서 최소 50ms까지
                val delayTime = (100L / (1.0 + iteration * 0.15)).toLong().coerceAtLeast(15L)
                delay(delayTime)
                iteration++
            }
        }
    }

    // 🚀 UI 줌 아웃 버튼 롱클릭 반복 축소 (가속도 효과)
    LaunchedEffect(isZoomOutPressed) {
        if (isZoomOutPressed) {
            var iteration = 0
            while (isZoomOutPressed) {
                mapLibreMap?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom - 0.1).coerceAtLeast(0.0)

                    // 커서가 있으면 커서 위치를 중앙으로 맞추고 줌 아웃
                    if (mapUiState.showCursor && mapUiState.cursorLatLng != null) {
                        val cursorLatLngValue = mapUiState.cursorLatLng!!

                        // 커서 위치를 지도 중앙으로 즉시 이동하고 줌 아웃 (애니메이션 없이)
                        val cameraUpdate =
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                org.maplibre.android.camera.CameraPosition.Builder()
                                    .target(cursorLatLngValue)
                                    .zoom(newZoom)
                                    .build()
                            )
                        map.moveCamera(cameraUpdate) // animateCamera 대신 moveCamera 사용 (즉시 이동)

                        // 커서 화면 위치를 중앙으로 업데이트
                        val centerScreenPoint = map.projection.toScreenLocation(cursorLatLngValue)
                        viewModel.updateCursorScreenPosition(centerScreenPoint)

                        Log.d(
                            "[MainActivity]",
                            "줌 아웃: 커서 위치(${cursorLatLngValue.latitude}, ${cursorLatLngValue.longitude})를 중앙으로 맞추고 줌 $currentZoom -> $newZoom"
                        )
                    } else {
                        // 커서가 없으면 일반 줌 아웃
                        val cameraUpdate =
                            org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                        map.animateCamera(cameraUpdate, 300)
                    }
                    Log.d("[MainActivity]", "줌 아웃: $currentZoom -> $newZoom")
                }

                // 가속도 효과: 처음에는 느리게(500ms), 점점 빨라져서 최소 50ms까지
                val delayTime = (100L / (1.0 + iteration * 0.15)).toLong().coerceAtLeast(15L)
                delay(delayTime)
                iteration++
            }
        }
    }

    // 지도 표시 모드 변경 시 회전 업데이트
    LaunchedEffect(mapUiState.mapDisplayMode) {
        updateMapRotation()
    }

    // 코스업 모드에서 포인트 변경 시 회전 업데이트
    LaunchedEffect(mapUiState.coursePoint) {
        if (mapUiState.mapDisplayMode == "코스업") {
            updateMapRotation()
        }
    }

    // 포인트 등록 다이얼로그 표시
    if (dialogUiState.showDialog) {
        PointRegistrationDialog(
            centerCoordinates = pointUiState.centerCoordinates,
            pointName = pointUiState.pointName,
            onPointNameChange = { viewModel.updatePointName(it) },
            selectedColor = pointUiState.selectedColor,
            onColorChange = { viewModel.updateSelectedColor(it) },
            selectedIconType = pointUiState.selectedIconType,
            onIconTypeChange = { viewModel.updateSelectedIconType(it) },
            getNextAvailablePointNumber = { getNextAvailablePointNumber() },
            onRegister = { registerPoint() },
            onDismiss = { viewModel.updateShowDialog(false) }
        )
    }

    // 포인트 관리 다이얼로그 표시
    if (dialogUiState.showPointManageDialog && pointUiState.selectedPoint != null) {
        PointManageDialog(
            point = pointUiState.selectedPoint!!,
            onDelete = { deletePoint(pointUiState.selectedPoint!!) },
            onEdit = {
                viewModel.updateShowPointManageDialog(false)
                viewModel.updateShowEditDialog(true)
            },
            onDismiss = { viewModel.updateShowPointManageDialog(false) }
        )
    }

    // 포인트 편집 다이얼로그 표시
    if (dialogUiState.showEditDialog && pointUiState.selectedPoint != null) {
        PointEditDialog(
            point = pointUiState.selectedPoint!!,
            pointName = pointUiState.editPointName,
            onPointNameChange = { viewModel.updateEditPointName(it) },
            selectedColor = pointUiState.editSelectedColor,
            onColorChange = { viewModel.updateEditSelectedColor(it) },
            onSave = {
                updatePoint(
                    pointUiState.selectedPoint!!,
                    pointUiState.editPointName,
                    pointUiState.editSelectedColor
                )
            },
            onDismiss = { viewModel.updateShowEditDialog(false) }
        )
    }

    // 포인트 삭제 목록 다이얼로그 표시
    if (dialogUiState.showPointDeleteList) {
        PointDeleteListDialog(
            points = loadPointsFromLocal(),
            onDeletePoint = { point -> deletePoint(point) },
            onDismiss = { viewModel.updateShowPointDeleteList(false) }
        )
    }

    // 항적 설정 다이얼로그
    if (dialogUiState.showTrackSettingsDialog) {
        var intervalType by remember { mutableStateOf(viewModel.getTrackSettings().intervalType) }
        var timeInterval by remember { mutableStateOf(viewModel.getTrackSettings().timeInterval.toString()) }
        var distanceInterval by remember { mutableStateOf(viewModel.getTrackSettings().distanceInterval.toString()) }

        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackSettingsDialog(false) },
            title = { Text("항적 설정") },
            text = {
                Column {
                    Text("기록 간격 설정:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 시간 간격 선택
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = intervalType == "time",
                            onClick = { intervalType = "time" }
                        )
                        Text("시간 간격", modifier = Modifier.clickable { intervalType = "time" })
                        Spacer(modifier = Modifier.width(8.dp))
                        if (intervalType == "time") {
                            TextField(
                                value = timeInterval,
                                onValueChange = { timeInterval = it },
                                label = { Text("밀리초") },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 거리 간격 선택
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = intervalType == "distance",
                            onClick = { intervalType = "distance" }
                        )
                        Text(
                            "거리 간격",
                            modifier = Modifier.clickable { intervalType = "distance" })
                        Spacer(modifier = Modifier.width(8.dp))
                        if (intervalType == "distance") {
                            TextField(
                                value = distanceInterval,
                                onValueChange = { distanceInterval = it },
                                label = { Text("미터") },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val settings = TrackSettings(
                            intervalType = intervalType,
                            timeInterval = if (intervalType == "time") timeInterval.toLongOrNull()
                                ?: 5000L else viewModel.getTrackSettings().timeInterval,
                            distanceInterval = if (intervalType == "distance") distanceInterval.toDoubleOrNull()
                                ?: 10.0 else viewModel.getTrackSettings().distanceInterval
                        )
                        viewModel.saveTrackSettings(settings)
                        viewModel.updateShowTrackSettingsDialog(false)
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.updateShowTrackSettingsDialog(false) }
                ) {
                    Text("취소")
                }
            }
        )
    }

    // 항적 목록 다이얼로그
    if (dialogUiState.showTrackListDialog) {
        var newTrackName by remember { mutableStateOf("") }
        var newTrackColor by remember { mutableStateOf(Color.Red) }
        var showNewTrackDialog by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackListDialog(false) },
            title = { Text("항적 목록") },
            text = {
                Column {
                    // 새 항적 추가 버튼
                    Button(
                        onClick = { showNewTrackDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("새 항적 추가")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 항적 목록
                    LazyColumn {
                        items(viewModel.getTracks()) { track ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (track.isVisible) track.color.copy(alpha = 0.3f) else Color.Gray.copy(
                                        alpha = 0.2f
                                    )
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = track.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "기록 ${track.records.size}개",
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 표시/숨김 스위치
                                            Switch(
                                                checked = track.isVisible,
                                                onCheckedChange = {
                                                    viewModel.setTrackVisibility(
                                                        track.id,
                                                        it
                                                    )
                                                    updateTrackDisplay()
                                                }
                                            )

                                            // 기록 시작 버튼
                                            if (!trackUiState.isRecordingTrack) {
                                                TextButton(
                                                    onClick = {
                                                        startTrackRecording(track)
                                                        viewModel.updateShowTrackListDialog(
                                                            false
                                                        )
                                                    }
                                                ) {
                                                    Text("기록", fontSize = 12.sp)
                                                }
                                            }

                                            // 삭제 버튼
                                            TextButton(
                                                onClick = {
                                                    viewModel.deleteTrack(track.id)
                                                    updateTrackDisplay()
                                                }
                                            ) {
                                                Text("삭제", fontSize = 12.sp, color = Color.Red)
                                            }
                                        }
                                    }

                                    // 항적 기록 목록
                                    if (track.records.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        track.records.forEach { record ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = record.title,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                                TextButton(
                                                    onClick = {
                                                        // 하이라이트 처리
                                                        viewModel.updateHighlightedTrackRecord(
                                                            Pair(track.id, record.id)
                                                        )
                                                        updateTrackDisplay()
                                                        viewModel.updateSelectedTrackForRecords(
                                                            track
                                                        )
                                                        viewModel.updateShowTrackRecordListDialog(
                                                            true
                                                        )
                                                    }
                                                ) {
                                                    Text("보기", fontSize = 10.sp)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        viewModel.deleteTrackRecord(
                                                            track.id,
                                                            record.id
                                                        )
                                                        updateTrackDisplay()
                                                    }
                                                ) {
                                                    Text(
                                                        "삭제",
                                                        fontSize = 10.sp,
                                                        color = Color.Red
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.updateShowTrackListDialog(false) }
                ) {
                    Text("닫기")
                }
            }
        )

        // 새 항적 추가 다이얼로그
        if (showNewTrackDialog) {
            AlertDialog(
                onDismissRequest = { showNewTrackDialog = false },
                title = { Text("새 항적 추가") },
                text = {
                    Column {
                        TextField(
                            value = newTrackName,
                            onValueChange = { newTrackName = it },
                            label = { Text("항적 이름") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("색상 선택:")
                        Row {
                            listOf(
                                Color.Red,
                                Color.Blue,
                                Color.Green,
                                Color.Yellow,
                                Color.Cyan,
                                Color.Magenta
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color, CircleShape)
                                        .clickable { newTrackColor = color }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTrackName.isNotBlank()) {
                                viewModel.addTrack(newTrackName, newTrackColor)
                                newTrackName = ""
                                newTrackColor = Color.Red
                                showNewTrackDialog = false
                            }
                        }
                    ) {
                        Text("추가")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showNewTrackDialog = false }
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }

    // 항적 기록 목록 다이얼로그
    if (dialogUiState.showTrackRecordListDialog && trackUiState.selectedTrackForRecords != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.updateShowTrackRecordListDialog(false)
                viewModel.updateSelectedTrackForRecords(null)
            },
            title = { Text("${trackUiState.selectedTrackForRecords!!.name} - 항적 기록") },
            text = {
                LazyColumn {
                    items(trackUiState.selectedTrackForRecords!!.records) { record ->
                        val isHighlighted = trackUiState.highlightedTrackRecord != null &&
                                trackUiState.highlightedTrackRecord!!.first == trackUiState.selectedTrackForRecords!!.id &&
                                trackUiState.highlightedTrackRecord!!.second == record.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // 하이라이트 처리
                                    viewModel.updateHighlightedTrackRecord(
                                        Pair(
                                            trackUiState.selectedTrackForRecords!!.id,
                                            record.id
                                        )
                                    )
                                    updateTrackDisplay()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighlighted) {
                                    Color.Yellow.copy(alpha = 0.5f) // 하이라이트된 경우 노란색 배경
                                } else {
                                    trackUiState.selectedTrackForRecords!!.color.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = record.title,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "점 ${record.points.size}개",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            // 하이라이트 해제
                            viewModel.updateHighlightedTrackRecord(null)
                            updateTrackDisplay()
                        }
                    ) {
                        Text("하이라이트 해제")
                    }
                    TextButton(
                        onClick = {
                            viewModel.updateShowTrackRecordListDialog(false)
                            viewModel.updateSelectedTrackForRecords(null)
                        }
                    ) {
                        Text("닫기")
                    }
                }
            }
        )
    }

    // 경유지 관리 다이얼로그
    if (dialogUiState.showWaypointDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.updateShowWaypointDialog(false) },
            title = { Text("경유지 관리") },
            text = {
                Column {
                    // 경유지 추가 버튼
                    Button(
                        onClick = {
                            viewModel.updateIsAddingWaypoint(true)
                            viewModel.updateShowWaypointDialog(false) // 다이얼로그 닫기
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("경유지 추가")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 경유지 목록
                    if (mapUiState.waypoints.isEmpty()) {
                        Text(
                            "경유지가 없습니다.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        LazyColumn {
                            items(mapUiState.waypoints.size) { index ->
                                val waypoint = mapUiState.waypoints[index]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = waypoint.color.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${index + 1}. ${waypoint.name}",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${waypoint.latitude}, ${waypoint.longitude}",
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }

                                        Row {
                                            // 위로 이동
                                            if (index > 0) {
                                                TextButton(
                                                    onClick = {
                                                        val updatedWaypoints =
                                                            mapUiState.waypoints.toMutableList()
                                                        val temp = updatedWaypoints[index]
                                                        updatedWaypoints[index] =
                                                            updatedWaypoints[index - 1]
                                                        updatedWaypoints[index - 1] = temp
                                                        viewModel.updateWaypoints(
                                                            updatedWaypoints
                                                        )
                                                        // 경로 업데이트
                                                        updateNavigationRouteUseCase.execute(
                                                            mapLibreMap,
                                                            locationManager?.getCurrentLocationObject(),
                                                            updatedWaypoints,
                                                            mapUiState.navigationPoint
                                                        )
                                                    }
                                                ) {
                                                    Text("↑", fontSize = 12.sp)
                                                }
                                            }

                                            // 아래로 이동
                                            if (index < mapUiState.waypoints.size - 1) {
                                                TextButton(
                                                    onClick = {
                                                        val updatedWaypoints =
                                                            mapUiState.waypoints.toMutableList()
                                                        val temp = updatedWaypoints[index]
                                                        updatedWaypoints[index] =
                                                            updatedWaypoints[index + 1]
                                                        updatedWaypoints[index + 1] = temp
                                                        viewModel.updateWaypoints(
                                                            updatedWaypoints
                                                        )
                                                        // 경로 업데이트
                                                        updateNavigationRouteUseCase.execute(
                                                            mapLibreMap,
                                                            locationManager?.getCurrentLocationObject(),
                                                            updatedWaypoints,
                                                            mapUiState.navigationPoint
                                                        )
                                                    }
                                                ) {
                                                    Text("↓", fontSize = 12.sp)
                                                }
                                            }

                                            // 삭제
                                            TextButton(
                                                onClick = {
                                                    val updatedWaypoints =
                                                        mapUiState.waypoints.toMutableList()
                                                    updatedWaypoints.removeAt(index)
                                                    viewModel.updateWaypoints(updatedWaypoints)
                                                    // 경로 업데이트
                                                    updateNavigationRouteUseCase.execute(
                                                        mapLibreMap,
                                                        locationManager?.getCurrentLocationObject(),
                                                        updatedWaypoints,
                                                        mapUiState.navigationPoint
                                                    )
                                                }
                                            ) {
                                                Text("삭제", fontSize = 12.sp, color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.updateShowWaypointDialog(false) }
                ) {
                    Text("닫기")
                }
            }
        )
    }

    // 포인트 선택 다이얼로그 (코스업용 및 경유지 추가용)
    if (dialogUiState.showPointSelectionDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.updateShowPointSelectionDialog(false)
            },
            title = { Text("코스업 포인트 선택") },
            text = {
                Column {
                    Text("코스업으로 사용할 포인트를 선택하세요:")
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn {
                        items(loadPointsFromLocal()) { point ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        // 항해 메뉴에서 호출된 경우 항해 포인트로 설정
                                        if (mapUiState.currentMenu == "navigation") {
                                            // 기존 항해 선과 마커 제거
                                            mapLibreMap?.let { map ->
                                                PMTilesLoader.removeNavigationLine(map)
                                                PMTilesLoader.removeNavigationMarker(map)
                                            }

                                            viewModel.updateNavigationPoint(point)
                                            // 항해 경로 및 마커 표시
                                            updateNavigationRouteUseCase.execute(
                                                mapLibreMap,
                                                locationManager?.getCurrentLocationObject(),
                                                mapUiState.waypoints,
                                                point
                                            )
                                            mapLibreMap?.let { map ->
                                                val navigationLatLng = LatLng(point.latitude, point.longitude)
                                                PMTilesLoader.addNavigationMarker(
                                                    map,
                                                    navigationLatLng,
                                                    point.name
                                                )
                                            }

                                            // 코스업 모드가 켜져 있다면 새로운 항해 목적지로 코스업 적용
                                            if (mapUiState.mapDisplayMode == "코스업") {
                                                viewModel.updateCoursePoint(point)
                                                updateMapRotation()
                                                Log.d(
                                                    "[MainActivity]",
                                                    "항해 목적지 변경으로 코스업 재적용: ${point.name}"
                                                )
                                            }
                                        } else {
                                            // 코스업 메뉴에서 호출된 경우 코스업 포인트로 설정
                                            viewModel.updateCoursePoint(point)
                                            viewModel.updateMapDisplayMode("코스업")
                                            updateMapRotation()
                                        }
                                        viewModel.updateShowPointSelectionDialog(false)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (mapUiState.coursePoint == point) Color.Yellow else Color.White
                                )
                            ) {
                                Text(
                                    text = "${point.name} (${
                                        String.format(
                                            "%.6f",
                                            point.latitude
                                        )
                                    }, ${String.format("%.6f", point.longitude)})",
                                    modifier = Modifier.padding(8.dp),
                                    color = if (mapUiState.coursePoint == point) Color.Black else Color.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateShowPointSelectionDialog(false)
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            // 메뉴창이 열려있을 때는 플로팅 버튼 숨김
            if (!mapUiState.showMenu) {
                // 현재 위치 버튼 (우측 하단)
                FloatingActionButton(
                    onClick = {
                        locationManager?.startAutoTracking()
                        // 현재 위치로 이동할 때 커서 숨김
                        viewModel.updateShowCursor(false)
                        viewModel.updateCursorLatLng(null)
                        viewModel.updateCursorScreenPosition(null)
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "내 위치로 이동",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        ChartPlotterMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            isDialogShown = dialogUiState.showDialog ||
                    dialogUiState.showPointManageDialog ||
                    dialogUiState.showEditDialog ||
                    dialogUiState.showPointDeleteList ||
                    dialogUiState.showPointSelectionDialog ||
                    dialogUiState.showWaypointDialog ||
                    dialogUiState.showTrackSettingsDialog ||
                    dialogUiState.showTrackListDialog ||
                    dialogUiState.showTrackRecordListDialog,
            showCursor = mapUiState.showCursor,
            cursorLatLng = mapUiState.cursorLatLng,
            cursorScreenPosition = mapUiState.cursorScreenPosition,
            onTouchEnd = { latLng, screenPoint ->
                viewModel.updateCursorLatLng(latLng)
                viewModel.updateCursorScreenPosition(screenPoint)
                viewModel.updateShowCursor(true)
            },
            onTouchStart = {
                // 드래그 시작 시 커서 표시
                viewModel.updateShowCursor(true)
            },
            onMapReady = { map ->

                map.uiSettings.apply {
                    isCompassEnabled = false  // 나침반 완전히 숨김
                }
                /* ✅ 줌 제한 */
                map.setMinZoomPreference(6.0)     // 최소 z=4
                map.setMaxZoomPreference(22.0)    // (원하시면 더 키우거나 줄이기)

                /* ✅ 터치 관련 UI 설정 - 지도 이동 허용, 회전만 비활성화 */
                map.uiSettings.isScrollGesturesEnabled = true
                map.uiSettings.isZoomGesturesEnabled = true
                map.uiSettings.isTiltGesturesEnabled = false
                map.uiSettings.isDoubleTapGesturesEnabled = true
                map.uiSettings.isQuickZoomGesturesEnabled = true
                map.uiSettings.isRotateGesturesEnabled = false

                /* ✅ Attribution과 Logo 숨기기 - 지도 이동 후 나타나는 원 제거 */
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false

                map.uiSettings.isFlingVelocityAnimationEnabled = false

                // 목적지 마커 추가 (지도 스타일 로드 완료 후)
                map.getStyle { style ->
                    // 약간의 지연을 두고 마커 추가 (스타일 완전 로드 대기)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        // 목적지 마커는 더 이상 사용하지 않음

                        // 항적 표시
                        // ViewModel은 setContent 블록에서 생성되므로 여기서는 직접 접근 불가
                        // 이 부분은 나중에 수정 필요
                    }, 500) // 0.5초 지연
                }

                /* ✅ 카메라 타겟 범위 제한: 한·중·일 대략 커버 */
                val regionBounds = LatLngBounds.Builder()
                    // NE, SW 2점만으로 범위 구성
                    .include(LatLng(42.0, 150.0))  // 북동 (대략 일본 북부~쿠릴 열도 부근까지)
                    .include(LatLng(24.0, 120.0))   // 남서 (중국 남부~베트남 북부 위도까지)
                    .build()

                map.setLatLngBoundsForCameraTarget(regionBounds)
                if (!mapUiState.isMapInitialized) {
                    mapLibreMap = map
                    viewModel.updateIsMapInitialized(true)
                    locationManager = LocationManager(
                        activity,
                        map,
                        onGpsLocationUpdate = { lat, lng, available ->
                            viewModel.updateGpsLocation(lat, lng, available)

                            // 항적 기록 점 추가
                            addTrackPointIfNeeded(lat, lng)

                            // 경유지 자동 제거: 현재 위치에서 10m 이내인 경유지 제거
                            val waypointsToRemove = mutableListOf<SavedPoint>()
                            mapUiState.waypoints.forEach { waypoint ->
                                val distance = calculateDistance(
                                    lat, lng,
                                    waypoint.latitude, waypoint.longitude
                                )
                                if (distance <= 10.0) { // 10m 이내
                                    waypointsToRemove.add(waypoint)
                                    Log.d(
                                        "[MainActivity]",
                                        "경유지 도달: ${waypoint.name} (거리: ${
                                            String.format(
                                                "%.2f",
                                                distance
                                            )
                                        }m)"
                                    )
                                }
                            }

                            // 도달한 경유지 제거
                            if (waypointsToRemove.isNotEmpty()) {
                                val updatedWaypoints = mapUiState.waypoints.toMutableList()
                                updatedWaypoints.removeAll(waypointsToRemove)
                                viewModel.updateWaypoints(updatedWaypoints)
                                Log.d("[MainActivity]", "경유지 ${waypointsToRemove.size}개 제거됨")
                            }

                            // 항해 경로 업데이트 (모든 모드에서 navigationPoint가 있으면)
                            updateNavigationRouteUseCase.execute(
                                map,
                                locationManager?.getCurrentLocationObject(),
                                mapUiState.waypoints,
                                mapUiState.navigationPoint
                            )
                        },
                        onBearingUpdate = { bearing ->
                            // COG 정보 업데이트
                            viewModel.updateCog(bearing)
                            // 헤딩업 모드일 때만 지도 회전 업데이트
                            if (mapUiState.mapDisplayMode == "헤딩업") {
//                                            Log.d("[MainActivity]", "헤딩업 모드: 보트 방향 ${bearing}도로 지도 회전")
                                updateMapRotation()
                            } else {
//                                            Log.v("[MainActivity]", "보트 방향 ${bearing}도 감지됨 (현재 모드: ${mapUiState.mapDisplayMode})")
                            }
                        }
                    )

                    // 센서 초기화
                    locationManager?.initializeSensors()

                    // GPS와 방향 정보 제공 여부 확인
                    locationManager?.checkAvailability()?.let { status ->
                        Log.d("[MainActivity]", "=== GPS 및 방향 정보 상태 ===")
                        Log.d("[MainActivity]", "GPS 제공 가능: ${status.gpsAvailable}")
                        Log.d("[MainActivity]", "  - 위치 권한: ${status.locationPermissionGranted}")
                        Log.d("[MainActivity]", "  - GPS 프로바이더: ${status.gpsEnabled}")
                        Log.d("[MainActivity]", "  - 네트워크 위치: ${status.networkLocationEnabled}")
                        Log.d("[MainActivity]", "방향 정보 제공 가능: ${status.bearingAvailable}")
                        Log.d("[MainActivity]", "  - 방향 센서: ${status.orientationSensorAvailable}")
                        Log.d(
                            "[MainActivity]",
                            "  - 회전 벡터 센서: ${status.rotationVectorSensorAvailable}"
                        )
                        Log.d("[MainActivity]", "================================")
                    }

                    // PMTiles 로드 후 선박 아이콘과 포인트 마커 추가를 위해 스타일 로드 완료를 기다림
                    map.getStyle { style ->
                        locationManager?.addShipToMap(style)
                        locationManager?.addPointsToMap(style)

                        // 저장된 포인트들을 지도에 표시
                        val savedPoints = loadPointsFromLocal()
                        locationManager?.updatePointsOnMap(savedPoints)
                    }

                    // 지도 터치/드래그 감지하여 자동 추적 중지 (수동 회전은 비활성화)
                    map.addOnCameraMoveListener {
                        locationManager?.stopAutoTracking()
                        // 수동 회전은 비활성화 - 지도 표시 모드에 따라 자동 회전만 허용
                    }

                    // 카메라 이동이 완전히 끝난 후 커서 GPS 좌표 업데이트 (줌 인/아웃 시 흔들림 방지)
                    map.addOnCameraIdleListener {
                        // 커서가 표시되고 있을 때, 맵 이동 완료 후 커서의 GPS 좌표 업데이트
                        if (mapUiState.showCursor && mapUiState.cursorScreenPosition != null) {
                            val screenPoint = mapUiState.cursorScreenPosition!!
                            try {
                                val updatedLatLng = map.projection.fromScreenLocation(
                                    android.graphics.PointF(screenPoint.x, screenPoint.y)
                                )
                                viewModel.updateCursorLatLng(updatedLatLng)
                                Log.d(
                                    "[MainActivity]",
                                    "맵 이동 완료 후 커서 GPS 좌표 업데이트: ${updatedLatLng.latitude}, ${updatedLatLng.longitude}"
                                )
                            } catch (e: Exception) {
                                Log.e("[MainActivity]", "커서 GPS 좌표 업데이트 실패: ${e.message}")
                            }
                        }
                    }

                    // 지도 클릭 이벤트 처리 (포인트 마커 클릭 감지 + 터치 위치에 커서 표시)
                    map.addOnMapClickListener { latLng ->
                        // 경유지 추가 모드인 경우: 커서만 표시
                        if (dialogUiState.isAddingWaypoint) {
                            val screenPoint = map.projection.toScreenLocation(latLng)
                            viewModel.updateCursorLatLng(latLng)
                            viewModel.updateCursorScreenPosition(screenPoint)
                            viewModel.updateShowCursor(true)
                            Log.d(
                                "[MainActivity]",
                                "경유지 추가 모드: 커서 위치 설정 ${latLng.latitude}, ${latLng.longitude}"
                            )
                            true // 기본 지도 클릭 이벤트 방지
                        } else {
                            // 기존 로직: 포인트 클릭 감지 및 커서 표시
                            // 클릭된 위치에서 포인트 레이어의 피처들을 쿼리
                            val screenPoint = map.projection.toScreenLocation(latLng)
                            val features = map.queryRenderedFeatures(
                                android.graphics.PointF(screenPoint.x, screenPoint.y),
                                "points-symbol"
                            )

                            // 항상 터치한 위치에 커서 표시
                            viewModel.updateCursorLatLng(latLng)
                            viewModel.updateCursorScreenPosition(screenPoint)
                            viewModel.updateShowCursor(true)

                            if (features.isNotEmpty()) {
                                // 포인트가 클릭되었음
                                val feature = features.first()
                                val pointName = feature.getStringProperty("name") ?: ""
                                val pointId = feature.getStringProperty("id") ?: ""

                                // 저장된 포인트 목록에서 해당 포인트 찾기
                                val savedPoints = loadPointsFromLocal()
                                val clickedPoint = savedPoints.find { point ->
                                    "${point.latitude}_${point.longitude}_${point.timestamp}" == pointId
                                }

                                clickedPoint?.let { point ->
                                    viewModel.updateSelectedPoint(point)
                                    viewModel.updateEditPointName(point.name)
                                    viewModel.updateEditSelectedColor(point.color)
                                    viewModel.updateShowPointManageDialog(true)
                                }

                                Log.d(
                                    "[MainActivity]",
                                    "포인트 클릭 + 커서 표시: ${latLng.latitude}, ${latLng.longitude}"
                                )

                                true // 기본 지도 클릭 이벤트 방지
                            } else {
                                Log.d(
                                    "[MainActivity]",
                                    "터치 위치에 커서 표시: ${latLng.latitude}, ${latLng.longitude}"
                                )

                                false // 기본 지도 클릭 이벤트 허용
                            }
                        }
                    }


                    // 위치 권한 확인 및 요청
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager?.startLocationUpdates()
                        // 첫 번째 위치 정보를 받으면 자동으로 그 위치로 이동 (onLocationChanged에서 처리)
                        Log.d("[ChartPlotterScreen]", "위치 추적 시작 - 첫 번째 위치에서 자동 이동")
                    } else {
                        Log.w("[ChartPlotterScreen]", "위치 권한이 없습니다. MainActivity에서 권한을 요청해야 합니다.")
                    }

                }
            }
        )

        // 우측 상단 메뉴 버튼은 MapControls로 이동됨 (제거됨)

        // 아이콘 선택 UI (커서가 표시될 때만 보임, 지도 좌측 상단)
        if (mapUiState.showCursor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 24.dp, end = 16.dp, start = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이전 아이콘 버튼 (<)
                    FloatingActionButton(
                        onClick = {
                            val newIconType = when (pointUiState.selectedIconType) {
                                "circle" -> "square"
                                "triangle" -> "circle"
                                "square" -> "triangle"
                                else -> "circle"
                            }
                            viewModel.updateSelectedIconType(newIconType)
                        },
                        shape = RoundedCornerShape(8.dp),
                        containerColor = Color(0xC6E2E2E2),
                        contentColor = Color.Black,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ),
                    ) {
                        Text(
                            text = "<",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 현재 선택된 아이콘 표시
                    Box(
                        modifier = Modifier
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (pointUiState.selectedIconType) {
                            "circle" -> {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.Black, CircleShape)
                                )
                            }

                            "triangle" -> {
                                Text(
                                    text = "▲",
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            }

                            "square" -> {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.Black, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }

                    // 다음 아이콘 버튼 (>)
                    FloatingActionButton(
                        onClick = {
                            val newIconType = when (pointUiState.selectedIconType) {
                                "circle" -> "triangle"
                                "triangle" -> "square"
                                "square" -> "circle"
                                else -> "circle"
                            }
                            viewModel.updateSelectedIconType(newIconType)
                        },
                        shape = RoundedCornerShape(8.dp),
                        containerColor = Color(0xC6E2E2E2),
                        contentColor = Color.Black,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ),
                    ) {
                        Text(
                            text = ">",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 메뉴 패널
        MenuPanel(
            viewModel = viewModel,
            mapLibreMap = mapLibreMap,
            locationManager = locationManager,
            loadPointsFromLocal = { loadPointsFromLocal() },
            getNextAvailablePointNumber = { getNextAvailablePointNumber() },
            updateMapRotation = { updateMapRotation() },
            stopTrackRecording = { stopTrackRecording() }
        )

        // 오버레이 (GPS 정보, 커서 정보)
        MapOverlays(viewModel = viewModel)

        // 지도 컨트롤 버튼들
        MapControls(
            viewModel = viewModel,
            mapLibreMap = mapLibreMap,
            locationManager = locationManager,
            onZoomIn = { viewModel.zoomIn(mapLibreMap) },
            onZoomOut = { viewModel.zoomOut(mapLibreMap) },
            onCurrentLocation = {
                locationManager?.startAutoTracking()
                viewModel.updateShowCursor(false)
                viewModel.updateCursorLatLng(null)
                viewModel.updateCursorScreenPosition(null)
            },
            onAddWaypoint = {
                mapUiState.cursorLatLng?.let { latLng ->
                    val newWaypoint = SavedPoint(
                        name = "경유지 ${mapUiState.waypoints.size + 1}",
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        color = Color.Green,
                        iconType = "circle",
                        timestamp = System.currentTimeMillis()
                    )
                    val updatedWaypoints = mapUiState.waypoints.toMutableList().apply { add(newWaypoint) }
                    viewModel.updateWaypoints(updatedWaypoints)

                    // 경로 업데이트
                    updateNavigationRouteUseCase.execute(
                        mapLibreMap,
                        locationManager?.getCurrentLocationObject(),
                        updatedWaypoints,
                        mapUiState.navigationPoint
                    )
                }
            },
            onCompleteWaypoint = {
                viewModel.updateIsAddingWaypoint(false)
                viewModel.updateShowCursor(false)
                viewModel.updateCursorLatLng(null)
                viewModel.updateCursorScreenPosition(null)
                viewModel.updateShowWaypointDialog(true)
            },
            onNavigate = {
                mapUiState.cursorLatLng?.let { latLng ->
                    mapLibreMap?.let { map ->
                        PMTilesLoader.removeNavigationLine(map)
                        PMTilesLoader.removeNavigationMarker(map)
                    }

                    val newNavigationPoint = SavedPoint(
                        name = "커서 위치",
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        color = Color.Blue,
                        iconType = "circle",
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.updateNavigationPoint(newNavigationPoint)

                    updateNavigationRouteUseCase.execute(
                        mapLibreMap,
                        locationManager?.getCurrentLocationObject(),
                        mapUiState.waypoints,
                        newNavigationPoint
                    )

                    val mapForMarker = mapLibreMap
                    if (mapForMarker != null) {
                        PMTilesLoader.addNavigationMarker(mapForMarker, latLng, "커서 위치")
                    }

                    if (mapUiState.mapDisplayMode == "코스업") {
                        viewModel.updateCoursePoint(newNavigationPoint)
                        updateMapRotation()
                    }

                    viewModel.updateShowCursor(false)
                    viewModel.updateCursorLatLng(null)
                    viewModel.updateCursorScreenPosition(null)
                }
            },
            onMenuClick = {
                viewModel.updateShowMenu(true)
                viewModel.updateCurrentMenu("main")
            },
            onCreateQuickPoint = { createQuickPoint() }
        )

        // 경유지 추가 모드 안내 메시지
        if (dialogUiState.isAddingWaypoint) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "경유지 추가 모드",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "지도를 터치하여 경유지를 추가하세요",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateIsAddingWaypoint(false)
                                    viewModel.updateShowCursor(false)
                                    viewModel.updateCursorLatLng(null)
                                    viewModel.updateCursorScreenPosition(null)
                                }
                            ) {
                                Text("취소")
                            }
                            Button(
                                onClick = {
                                    // 현재 커서 위치가 있으면 경유지로 추가
                                    mapUiState.cursorLatLng?.let { latLng ->
                                        val newWaypoint = SavedPoint(
                                            name = "경유지 ${mapUiState.waypoints.size + 1}",
                                            latitude = latLng.latitude,
                                            longitude = latLng.longitude,
                                            color = Color.Yellow, // 경유지는 노란색으로 표시
                                            iconType = "circle",
                                            timestamp = System.currentTimeMillis()
                                        )
                                        val updatedWaypoints = mapUiState.waypoints.toMutableList()
                                        updatedWaypoints.add(newWaypoint)
                                        viewModel.updateWaypoints(updatedWaypoints)

                                        // 경로 업데이트
                                        updateNavigationRouteUseCase.execute(
                                            mapLibreMap,
                                            locationManager?.getCurrentLocationObject(),
                                            updatedWaypoints,
                                            mapUiState.navigationPoint
                                        )

                                        Log.d(
                                            "[MainActivity]",
                                            "완료 버튼으로 경유지 추가됨: ${latLng.latitude}, ${latLng.longitude}"
                                        )
                                    }

                                    // 경유지 추가 모드 종료
                                    viewModel.updateIsAddingWaypoint(false)
                                    viewModel.updateShowCursor(false)
                                    viewModel.updateCursorLatLng(null)
                                    viewModel.updateCursorScreenPosition(null)
                                }
                            ) {
                                Text("완료")
                            }
                        }
                    }
                }
            }
        }

        // 좌측 상단/하단 오버레이는 MapOverlays로 이동됨
    }
}