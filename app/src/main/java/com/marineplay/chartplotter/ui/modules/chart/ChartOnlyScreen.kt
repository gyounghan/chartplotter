package com.marineplay.chartplotter.ui.modules.chart

import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
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
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.MainActivity
import kotlinx.coroutines.runBlocking
import com.marineplay.chartplotter.helpers.PointHelper
import com.marineplay.chartplotter.ui.components.dialogs.*
import com.marineplay.chartplotter.ui.components.map.ChartPlotterMap
import com.marineplay.chartplotter.ui.modules.chart.components.MapControls
import com.marineplay.chartplotter.ui.modules.chart.components.MenuPanel
import com.marineplay.chartplotter.ui.modules.chart.components.MapOverlays
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
import android.content.Intent
import android.net.Uri
import org.maplibre.android.geometry.LatLngBounds
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChartOnlyScreen(
    viewModel: MainViewModel,
    activity: ComponentActivity,
    pointHelper: PointHelper,
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

        mapLibreMap?.let { map ->
            // 배치 업데이트: 한 번의 getStyle 콜백에서 모든 항적 처리 (성능 최적화)
            map.getStyle { style ->
                // 여러 항적 동시 기록 지원: 각 항적마다 별도의 소스 ID 사용
                trackUiState.recordingTracks.forEach { (trackId, recordingState) ->
                    // 항적 정보 가져오기 (캐시 사용으로 최적화됨)
                    val track = viewModel.getTrack(trackId) ?: return@forEach
                    
                    // 화면에 표시할 최대 점 수 제한 (성능 최적화: 최근 2000개만 표시)
                    val displayPoints = if (recordingState.points.size > 2000) {
                        recordingState.points.takeLast(2000)
                    } else {
                        recordingState.points
                    }
                    
                    val points = displayPoints.map { LatLng(it.latitude, it.longitude) }
                    // 선과 점 마커를 함께 표시 (점이 1개여도 선 함수가 처리)
                    PMTilesLoader.addTrackLine(
                        map,
                        "current_track_$trackId", // 각 항적마다 고유한 소스 ID
                        points,
                        track.color
                    )
                }
                
                // 하위 호환성: 기존 current_track도 처리 (단일 항적 기록)
                if (trackUiState.trackPoints.isNotEmpty() && trackUiState.currentRecordingTrack != null) {
                    val displayPoints = if (trackUiState.trackPoints.size > 2000) {
                        trackUiState.trackPoints.takeLast(2000)
                    } else {
                        trackUiState.trackPoints
                    }
                    val points = displayPoints.map { LatLng(it.latitude, it.longitude) }
                    // 선과 점 마커를 함께 표시
                    PMTilesLoader.addTrackLine(
                        map,
                        "current_track",
                        points,
                        trackUiState.currentRecordingTrack!!.color
                    )
                }
                
                // 현재 항적 레이어 추가 후 선박 레이어를 다시 맨 위로 이동
                locationManager?.moveShipLayerToTop(style)
            }
        }
    }

    fun updateTrackDisplay() {
        mapLibreMap?.let { map ->
            PMTilesLoader.removeAllTracks(map)

            val trackUiState = viewModel.trackUiState
            val highlightedRecord = trackUiState.highlightedTrackRecord
            
            // 모든 항적을 표시하되, 하이라이트된 항적만 효과를 부여
            val tracksToDisplay = viewModel.getTracks().filter { it.isVisible }
            tracksToDisplay.forEach { track ->
                val isHighlighted = highlightedRecord != null &&
                        highlightedRecord.first == track.id &&
                        highlightedRecord.second != null
                
                // 하이라이트된 항적인 경우 해당 날짜 포인트만 하이라이트 효과로 표시
                if (isHighlighted) {
                    val highlightedDate = highlightedRecord!!.second
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    
                    // 해당 날짜 포인트만 필터링
                    val highlightedPoints = track.points.filter { point ->
                        val pointDate = dateFormat.format(java.util.Date(point.timestamp))
                        pointDate == highlightedDate
                    }
                    
                    // 하이라이트된 날짜 포인트 표시
                    if (highlightedPoints.isNotEmpty()) {
                        val points = highlightedPoints.map { LatLng(it.latitude, it.longitude) }
                        PMTilesLoader.addTrackLine(
                            map,
                            "track_${track.id}_highlighted",
                            points,
                            track.color,
                            true // 하이라이트 효과
                        )
                    }
                    
                    // 나머지 날짜 포인트는 일반 표시
                    val otherPoints = track.points.filter { point ->
                        val pointDate = dateFormat.format(java.util.Date(point.timestamp))
                        pointDate != highlightedDate
                    }
                    
                    if (otherPoints.isNotEmpty()) {
                        // 최근 2000개 포인트만 표시
                        val displayPoints = if (otherPoints.size > 2000) {
                            otherPoints.takeLast(2000)
                        } else {
                            otherPoints
                        }
                        
                        val points = displayPoints.map { LatLng(it.latitude, it.longitude) }
                        PMTilesLoader.addTrackLine(
                            map,
                            "track_${track.id}",
                            points,
                            track.color,
                            false // 하이라이트 효과 없음
                        )
                    }
                } else {
                    // 하이라이트되지 않은 항적: 일반 표시
                    val displayPoints = if (track.points.size > 2000) {
                        track.points.takeLast(2000)
                    } else {
                        track.points
                    }
                    
                    if (displayPoints.isNotEmpty()) {
                        val points = displayPoints.map { LatLng(it.latitude, it.longitude) }
                        PMTilesLoader.addTrackLine(
                            map,
                            "track_${track.id}",
                            points,
                            track.color,
                            false // 하이라이트 효과 없음
                        )
                    }
                }
            }

            if (trackUiState.recordingTracks.isNotEmpty() ||
                (trackUiState.isRecordingTrack && trackUiState.currentRecordingTrack != null)) {
                updateCurrentTrackDisplay()
            }
            
            // 항적 레이어 추가 후 선박 레이어를 다시 맨 위로 이동
            map.getStyle { style ->
                locationManager?.moveShipLayerToTop(style)
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

    // 항적 상태 변화 관찰하여 자동으로 표시 업데이트 (디바운싱 적용)
    LaunchedEffect(trackUiState.recordingTracks) {
        // 100ms 지연으로 여러 업데이트를 한 번에 처리 (성능 최적화)
        delay(100)
        updateCurrentTrackDisplay()
        
        // 앱 시작 시 자동 기록을 위한 타이머 시작 (isRecording=true인 항적에 대해서만)
        trackUiState.recordingTracks.forEach { (trackId, recordingState) ->
            val track = viewModel.getTracks().find { it.id == trackId }
            if (track != null && track.intervalType == "time") {
                // MainActivity의 타이머 시작
                (activity as MainActivity).startTrackTimerForAutoRecording(track, viewModel)
            }
        }
    }

    // 뒤로가기 처리: 설정 화면이나 메뉴가 열려있으면 닫기
    BackHandler(enabled = mapUiState.showMenu) {
        if (mapUiState.showMenu) {
            viewModel.updateShowMenu(false)
            viewModel.updateCurrentMenu("main")
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

    // 항적 설정 다이얼로그 (항적별 설정 수정)
    if (dialogUiState.showTrackSettingsDialog && trackUiState.selectedTrackForSettings != null) {
        val track = trackUiState.selectedTrackForSettings!!
        var intervalType by remember { mutableStateOf(track.intervalType) }
        var timeIntervalText by remember { mutableStateOf((track.timeInterval / 1000L).toString()) }
        var distanceIntervalText by remember { mutableStateOf(track.distanceInterval.toString()) }
        
        AlertDialog(
            onDismissRequest = {
                viewModel.updateShowTrackSettingsDialog(false)
                viewModel.updateSelectedTrackForSettings(null)
            },
            title = { Text("${track.name} 설정") },
            text = {
                Column {
                    Text("기록 간격 설정:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 시간/거리 선택
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = intervalType == "time",
                            onClick = { intervalType = "time" }
                        )
                        Text("시간", modifier = Modifier.padding(start = 4.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = intervalType == "distance",
                            onClick = { intervalType = "distance" }
                        )
                        Text("거리", modifier = Modifier.padding(start = 4.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (intervalType == "time") {
                        TextField(
                            value = timeIntervalText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) timeIntervalText = it },
                            label = { Text("시간 간격 (초)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextField(
                            value = distanceIntervalText,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) distanceIntervalText = it },
                            label = { Text("거리 간격 (미터)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val timeInterval = if (intervalType == "time") {
                            (timeIntervalText.toLongOrNull() ?: 5L) * 1000L
                        } else {
                            track.timeInterval
                        }
                        val distanceInterval = if (intervalType == "distance") {
                            distanceIntervalText.toDoubleOrNull() ?: 10.0
                        } else {
                            track.distanceInterval
                        }
                        
                        kotlinx.coroutines.runBlocking {
                            viewModel.updateTrackSettings(
                                track.id,
                                intervalType,
                                timeInterval,
                                distanceInterval
                            )
                        }
                        viewModel.updateShowTrackSettingsDialog(false)
                        viewModel.updateSelectedTrackForSettings(null)
                        updateTrackDisplay()
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.updateShowTrackSettingsDialog(false)
                        viewModel.updateSelectedTrackForSettings(null)
                    }
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
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 항적 색상 표시
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(track.color, CircleShape)
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                            
                                            Column {
                                                Text(
                                                    text = track.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "포인트 ${track.points.size}개",
                                                    fontSize = 12.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 표시/숨김 스위치
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(60.dp)
                                            ) {
                                                Text(
                                                    text = "표시",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                                Switch(
                                                    checked = track.isVisible,
                                                    onCheckedChange = {
                                                        val success = viewModel.setTrackVisibility(
                                                            track.id,
                                                            it
                                                        )
                                                        if (success) {
                                                            updateTrackDisplay()
                                                        }
                                                        // 실패 시 (제한 초과) 다이얼로그는 ViewModel에서 표시됨
                                                    }
                                                )
                                            }

                                            // 기록 on/off 스위치 (단일 항적만 기록 가능)
                                            val isRecording = viewModel.trackUiState.recordingTracks.containsKey(track.id)
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(60.dp)
                                            ) {
                                                Text(
                                                    text = "기록",
                                                    fontSize = 10.sp,
                                                    color = Color.White
                                                )
                                                Switch(
                                                    checked = isRecording,
                                                    onCheckedChange = {
                                                        // 기록 시작/중지: 다른 항적이 기록 중이면 자동으로 중지되고 저장됨
                                                        viewModel.toggleTrackRecording(track.id)
                                                        updateTrackDisplay()
                                                    }
                                                )
                                            }

                                            // 설정 버튼
                                            TextButton(
                                                onClick = {
                                                    // 항적 설정 다이얼로그 표시
                                                    viewModel.updateSelectedTrackForSettings(track)
                                                    viewModel.updateShowTrackSettingsDialog(true)
                                                }
                                            ) {
                                                Text("설정", fontSize = 12.sp)
                                            }

                                            // 삭제 버튼
                                            TextButton(
                                                onClick = {
                                                    if (isRecording) {
                                                        viewModel.stopTrackRecording(track.id)
                                                    }
                                                    viewModel.deleteTrack(track.id)
                                                    updateTrackDisplay()
                                                }
                                            ) {
                                                Text("삭제", fontSize = 12.sp, color = Color.Red)
                                            }
                                        }
                                    }

                                    // 항적 날짜 목록
                                    val trackDates = remember(track.id) {
                                        kotlinx.coroutines.runBlocking {
                                            viewModel.getDatesByTrackId(track.id)
                                        }
                                    }
                                    if (trackDates.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        trackDates.forEach { date ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = date,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                                // 하이라이트 상태 확인
                                                val isCurrentlyHighlighted = trackUiState.highlightedTrackRecord != null &&
                                                        trackUiState.highlightedTrackRecord!!.first == track.id &&
                                                        trackUiState.highlightedTrackRecord!!.second == date
                                                
                                                TextButton(
                                                    onClick = {
                                                        if (isCurrentlyHighlighted) {
                                                            // 이미 하이라이트된 경우: 취소
                                                            viewModel.updateHighlightedTrackRecord(null)
                                                        } else {
                                                            // 하이라이트 처리 (날짜별)
                                                            viewModel.updateHighlightedTrackRecord(
                                                                Pair(track.id, date)
                                                            )
                                                        }
                                                        updateTrackDisplay()
                                                    }
                                                ) {
                                                    Text(
                                                        if (isCurrentlyHighlighted) "취소" else "보기",
                                                        fontSize = 10.sp,
                                                        color = if (isCurrentlyHighlighted) Color.Red else Color.White
                                                    )
                                                }
                                                TextButton(
                                                    onClick = {
                                                        viewModel.deleteTrackPointsByDate(
                                                            track.id,
                                                            date
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
            var intervalType by remember { mutableStateOf("time") }
            var timeIntervalText by remember { mutableStateOf("5") }
            var distanceIntervalText by remember { mutableStateOf("10") }
            
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                        .size(48.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (newTrackColor == color) 3.dp else 1.dp,
                                            color = if (newTrackColor == color) Color.White else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { newTrackColor = color }
                                        .padding(if (newTrackColor == color) 2.dp else 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("기록 기준:")
                        Row {
                            RadioButton(
                                selected = intervalType == "time",
                                onClick = { intervalType = "time" }
                            )
                            Text("시간", modifier = Modifier.padding(start = 4.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = intervalType == "distance",
                                onClick = { intervalType = "distance" }
                            )
                            Text("거리", modifier = Modifier.padding(start = 4.dp))
                        }
                        if (intervalType == "time") {
                            TextField(
                                value = timeIntervalText,
                                onValueChange = { if (it.all { char -> char.isDigit() }) timeIntervalText = it },
                                label = { Text("시간 간격 (초)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            TextField(
                                value = distanceIntervalText,
                                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) distanceIntervalText = it },
                                label = { Text("거리 간격 (미터)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTrackName.isNotBlank()) {
                                val timeInterval = if (intervalType == "time") {
                                    (timeIntervalText.toLongOrNull() ?: 5L) * 1000L
                                } else {
                                    5000L
                                }
                                val distanceInterval = if (intervalType == "distance") {
                                    distanceIntervalText.toDoubleOrNull() ?: 10.0
                                } else {
                                    10.0
                                }
                                viewModel.addTrack(
                                    newTrackName, 
                                    newTrackColor,
                                    intervalType,
                                    timeInterval,
                                    distanceInterval
                                )
                                newTrackName = ""
                                newTrackColor = Color.Red
                                intervalType = "time"
                                timeIntervalText = "5"
                                distanceIntervalText = "10"
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

    // 항적 표시 제한 알림 다이얼로그
    if (dialogUiState.showTrackLimitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackLimitDialog(false) },
            title = { Text("항적 표시 제한") },
            text = {
                Text("화면에 표시할 수 있는 항적 기록은 최대 10개입니다.\n다른 항적을 숨기고 다시 시도해주세요.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.updateShowTrackLimitDialog(false) }
                ) {
                    Text("확인")
                }
            }
        )
    }

    // 항적 날짜 목록 다이얼로그
    if (dialogUiState.showTrackRecordListDialog && trackUiState.selectedTrackForRecords != null) {
        val trackDates = remember(trackUiState.selectedTrackForRecords!!.id) {
            kotlinx.coroutines.runBlocking {
                viewModel.getDatesByTrackId(trackUiState.selectedTrackForRecords!!.id)
            }
        }
        
        AlertDialog(
            onDismissRequest = {
                viewModel.updateShowTrackRecordListDialog(false)
                viewModel.updateSelectedTrackForRecords(null)
            },
            title = { Text("${trackUiState.selectedTrackForRecords!!.name} - 날짜별 항적") },
            text = {
                LazyColumn {
                    items(trackDates) { date ->
                        val isHighlighted = trackUiState.highlightedTrackRecord != null &&
                                trackUiState.highlightedTrackRecord!!.first == trackUiState.selectedTrackForRecords!!.id &&
                                trackUiState.highlightedTrackRecord!!.second == date
                        
                        val pointsCount = remember(date) {
                            kotlinx.coroutines.runBlocking {
                                viewModel.getTrackPointsByDate(trackUiState.selectedTrackForRecords!!.id, date).size
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (isHighlighted) {
                                        // 이미 하이라이트된 경우: 취소
                                        viewModel.updateHighlightedTrackRecord(null)
                                    } else {
                                        // 하이라이트 처리
                                        viewModel.updateHighlightedTrackRecord(
                                            Pair(
                                                trackUiState.selectedTrackForRecords!!.id,
                                                date
                                            )
                                        )
                                    }
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = date,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "점 ${pointsCount}개",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                                // 하이라이트 상태 표시
                                if (isHighlighted) {
                                    Text(
                                        text = "✓ 하이라이트",
                                        fontSize = 11.sp,
                                        color = Color.Yellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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

    // 시스템 설정은 SystemSetting 앱에서만 관리됩니다.
    // 차트플로터 앱은 설정값을 읽어서만 사용합니다.

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


    // 전체 화면을 Box로 감싸서 레이어링
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 차트플로터 메인 화면 (항상 렌더링, 설정 화면 아래에 위치)
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
                    dialogUiState.showTrackRecordListDialog ||
                    dialogUiState.showTrackLimitDialog,
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

                            // 항적 기록 점 추가 (MainActivity의 addTrackPointIfNeeded 호출)
                            if (activity is MainActivity) {
                                (activity as MainActivity).addTrackPointIfNeeded(lat, lng, viewModel)
                            } else {
                                // MainActivity가 아닌 경우 기본 처리
                                addTrackPointIfNeeded(lat, lng)
                            }

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
                        
                        // 앱 시작 시 항적 표시 (항적 화면 노출이 켜져있으면 최근 기록 표시)
                        updateTrackDisplay()
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
                        ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager?.startLocationUpdates()
                        // 첫 번째 위치 정보를 받으면 자동으로 그 위치로 이동 (onLocationChanged에서 처리)
                        Log.d("[ChartPlotterScreen]", "위치 추적 시작 - 첫 번째 위치에서 자동 이동")
                    } else {
                        // MainActivity의 권한 요청 메서드 호출
                        if (activity is MainActivity) {
                            (activity as MainActivity).requestLocationPermission()
                            Log.d("[ChartPlotterScreen]", "위치 권한 요청")
                        } else {
                            Log.w("[ChartPlotterScreen]", "위치 권한이 없습니다. MainActivity에서 권한을 요청해야 합니다.")
                        }
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
            activity = activity,
            updateMapRotation = { updateMapRotation() },
            updateTrackDisplay = { updateTrackDisplay() }
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
                // 위치 권한 확인
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 위치 업데이트가 시작되지 않았으면 시작
                    if (locationManager?.hasLocationPermission() == true) {
                        locationManager?.startLocationUpdates()
                    }
                    
                    // 현재 위치로 이동
                    locationManager?.getCurrentLocation()?.let { currentLocation ->
                        mapLibreMap?.let { map ->
                            val cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(currentLocation)
                                .zoom(map.cameraPosition.zoom)
                                .bearing(map.cameraPosition.bearing)
                                .build()
                            map.cameraPosition = cameraPosition
                            Log.d("[ChartPlotterScreen]", "현재 위치로 이동: ${currentLocation.latitude}, ${currentLocation.longitude}")
                        }
                    }
                    
                    // 자동 추적 시작
                    locationManager?.startAutoTracking()
                    viewModel.updateShowCursor(false)
                    viewModel.updateCursorLatLng(null)
                    viewModel.updateCursorScreenPosition(null)
                } else {
                    // 권한이 없으면 권한 요청
                    if (activity is MainActivity) {
                        (activity as MainActivity).requestLocationPermission()
                        Log.d("[ChartPlotterScreen]", "현재 위치 버튼: 위치 권한 요청")
                    }
                }
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
        
        // 설정 화면은 SystemSetting 앱에서만 제공됩니다.
    }
}

