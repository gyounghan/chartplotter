package com.kumhomarine.chartplotter.presentation.modules.chart

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.R
import android.widget.Toast
import com.kumhomarine.chartplotter.*
import com.kumhomarine.chartplotter.domain.entities.Track
import com.kumhomarine.chartplotter.MainActivity
import kotlinx.coroutines.runBlocking
import com.kumhomarine.chartplotter.presentation.components.dialogs.*
import com.kumhomarine.chartplotter.presentation.components.map.ChartPlotterMap
import com.kumhomarine.chartplotter.presentation.modules.chart.components.MapControls
import com.kumhomarine.chartplotter.presentation.modules.chart.components.MenuPanel
import com.kumhomarine.chartplotter.presentation.modules.chart.components.MapOverlays
import com.kumhomarine.chartplotter.presentation.modules.chart.components.SettingsScreen
import com.kumhomarine.chartplotter.presentation.viewmodel.MainViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.SettingsViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.TrackViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.RouteViewModel
import com.kumhomarine.chartplotter.SavedPoint
import com.kumhomarine.chartplotter.domain.mappers.PointMapper
import com.kumhomarine.chartplotter.domain.usecases.UpdateNavigationRouteUseCase
import com.kumhomarine.chartplotter.domain.usecases.RouteUseCase
import com.kumhomarine.chartplotter.domain.usecases.ConnectRouteToNavigationUseCase
import com.kumhomarine.chartplotter.domain.usecases.CalculateDistanceUseCase
import com.kumhomarine.chartplotter.data.models.RoutePoint
import com.kumhomarine.chartplotter.PMTilesLoader
import com.kumhomarine.chartplotter.presentation.utils.ChartPlotterHelpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import org.maplibre.android.geometry.LatLngBounds
import androidx.compose.ui.graphics.toArgb
import com.kumhomarine.chartplotter.presentation.modules.ais.di.AISModule
import com.kumhomarine.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel
import androidx.compose.runtime.collectAsState

private val NavyColor = Color(0xFF001F3F)
private val NavyColorSemi = Color(0xE6001F3F)
private val RouteCardBg = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChartOnlyScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    trackViewModel: TrackViewModel,
    routeViewModel: RouteViewModel,
    activity: ComponentActivity,
    onMapLibreMapChange: (MapLibreMap?) -> Unit = {},
    onLocationManagerChange: (LocationManager?) -> Unit = {}
) {
    // 지도 및 위치 관리자 상태
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var locationManager by remember { mutableStateOf<LocationManager?>(null) }
    var isMapStyleLoaded by remember { mutableStateOf(false) }
    
    // 화면 진입 카운터 (화면 재진입 감지용)
    var screenEntryKey by remember { mutableStateOf(0) }
    
    // Coroutine scope
    val coroutineScope = rememberCoroutineScope()
    
    // UseCase 인스턴스
    val updateNavigationRouteUseCase = remember { UpdateNavigationRouteUseCase() }
    
    // AIS ViewModel 생성
    val aisViewModel = remember { AISModule.createAISViewModel(activity) }
    
    // 경로 점 편집 다이얼로그 상태
    var showRoutePointEditDialog by remember { mutableStateOf(false) }
    var selectedRoutePointForEdit by remember { mutableStateOf<RoutePoint?>(null) }
    
    // AIS 선박 데이터 구독
    val aisVessels by aisViewModel.vessels.collectAsState()
    val filteredAisVessels = remember(aisVessels) {
        val now = System.currentTimeMillis()
        aisVessels.filter { vessel ->
            val hasValidPosition = vessel.latitude != null && vessel.longitude != null
            val isRecent = (now - vessel.lastUpdate) <= 10 * 60 * 1000L
            hasValidPosition && isRecent
        }
    }
    
    // 선택된 AIS 선박 (다이얼로그 표시용)
    var selectedAISVessel by remember { mutableStateOf<com.kumhomarine.chartplotter.domain.entities.AISVessel?>(null) }
    
    // AIS Overlay 생성
    val aisOverlay = remember { com.kumhomarine.chartplotter.presentation.modules.chart.overlays.AISOverlay() }
    
    // ✅ AIS Overlay 시작 플래그 (초기화 완료 체크용)
    var aisOverlayStarted by remember { mutableStateOf(false) }

    // MapUiState (AIS 표시 제어용 - 상단에서 선언하여 LaunchedEffect에서 사용)
    val mapUiStateForAis = viewModel.mapUiState

    // MainActivity에 mapLibreMap과 locationManager 전달
    LaunchedEffect(mapLibreMap) {
        onMapLibreMapChange(mapLibreMap)
    }

    LaunchedEffect(locationManager) {
        onLocationManagerChange(locationManager)
    }
    
    // AIS 연결 시작
    LaunchedEffect(Unit) {
        aisViewModel.connect()
    }
    
    // 화면 진입 시마다 모든 오버레이와 레이어 재생성
    // mapLibreMap이 null이 아니고 isMapStyleLoaded가 false일 때 재초기화
    LaunchedEffect(mapLibreMap, isMapStyleLoaded, screenEntryKey) {
        if (mapLibreMap != null && !isMapStyleLoaded) {
            mapLibreMap?.let { map ->
                Log.d("[ChartOnlyScreen]", "화면 진입/재진입 - 모든 오버레이 및 레이어 재초기화 시작")
                
                val mapUiState = viewModel.mapUiState
                
                // 지도 스타일을 명시적으로 다시 로드하여 재초기화
                map.getStyle { style ->
                    isMapStyleLoaded = true
                    
                    // 선박 아이콘과 포인트 레이어 재추가
                    locationManager?.addShipToMap(style)
                    locationManager?.addPointsToMap(style)
                    
                    // 항해 경로 재생성 (navigationPoint가 있으면)
                    // 위치 정보가 없어도 목적지 마커는 먼저 표시 (destinationVisible일 때만)
                    if (mapUiState.navigationPoint != null && settingsViewModel.systemSettings.destinationVisible) {
                        val navigationLatLng = LatLng(
                            mapUiState.navigationPoint.latitude,
                            mapUiState.navigationPoint.longitude
                        )
                        PMTilesLoader.addNavigationMarker(
                            map,
                            navigationLatLng,
                            mapUiState.navigationPoint.name
                        )
                        
                        // 위치 정보가 있으면 항해 경로도 그리기
                        if (locationManager?.getCurrentLocationObject() != null) {
                            updateNavigationRouteUseCase.execute(
                                map,
                                locationManager?.getCurrentLocationObject(),
                                mapUiState.waypoints,
                                mapUiState.navigationPoint
                            )
                            Log.d("[ChartOnlyScreen]", "항해 경로 및 목적지 마커 재생성")
                        } else {
                            Log.d("[ChartOnlyScreen]", "목적지 마커 표시 (위치 정보 대기 중, GPS 수신 시 경로 자동 생성)")
                        }
                    }
                    
                    // AIS Overlay 재시작 (coroutineScope 내에서 delay 호출) - aisVisible일 때만
                    if (mapUiState.aisVisible) {
                        aisOverlay.stop()
                        aisOverlayStarted = false // ✅ 시작 플래그 리셋
                        Log.d("[ChartOnlyScreen]", "AIS Overlay 시작 준비: aisOverlayStarted=false로 리셋")
                        coroutineScope.launch {
                            delay(100)
                            // ✅ 초기 선박 데이터를 start()에 전달하여 스타일 로드 후 자동 업데이트
                            Log.d("[ChartOnlyScreen]", "AIS Overlay start() 호출: ${filteredAisVessels.size}개 선박")
                            aisOverlay.start(map, filteredAisVessels)
                            aisOverlayStarted = true // ✅ 시작 플래그 설정
                            map.getStyle { style -> locationManager?.moveShipLayerToTop(style) }
                            Log.d("[ChartOnlyScreen]", "✅ AIS Overlay start() 완료: aisOverlayStarted=true로 설정")
                        }
                    } else {
                        aisOverlay.stop()
                        aisOverlayStarted = false
                    }
                    
                    Log.d("[ChartOnlyScreen]", "화면 진입/재진입 - 모든 오버레이 및 레이어 재초기화 완료")
                }
            }
        }
    }
    
    // ✅ AIS 표시 OFF 시 Overlay 중지
    LaunchedEffect(mapUiStateForAis.aisVisible) {
        if (!mapUiStateForAis.aisVisible) {
            aisOverlay.stop()
            aisOverlayStarted = false
            Log.d("[ChartOnlyScreen]", "AIS 표시 OFF - Overlay 중지")
        }
    }

    // ✅ AIS Overlay 시작: isMapStyleLoaded가 true이고 aisOverlayStarted가 false이고 aisVisible일 때 시작
    LaunchedEffect(mapLibreMap, isMapStyleLoaded, aisOverlayStarted, mapUiStateForAis.aisVisible) {
        if (mapLibreMap != null && isMapStyleLoaded && !aisOverlayStarted && mapUiStateForAis.aisVisible) {
            mapLibreMap?.let { map ->
                Log.d("[ChartOnlyScreen]", "🚀 AIS Overlay 자동 시작: isMapStyleLoaded=true, aisOverlayStarted=false")
                aisOverlay.stop()
                coroutineScope.launch {
                    delay(100)
                    Log.d("[ChartOnlyScreen]", "AIS Overlay start() 호출: ${filteredAisVessels.size}개 선박")
                    aisOverlay.start(map, filteredAisVessels)
                    aisOverlayStarted = true
                    map.getStyle { style -> locationManager?.moveShipLayerToTop(style) }
                    Log.d("[ChartOnlyScreen]", "✅ AIS Overlay start() 완료: aisOverlayStarted=true로 설정")
                }
            }
        }
    }
    
    // AIS 선박 데이터가 업데이트되면 overlay에 반영
    // ✅ 스로틀링: 500ms에 한 번만 업데이트
    // ✅ isMapStyleLoaded가 true이고, start()가 호출된 후에만 업데이트
    var lastAisUpdateTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(filteredAisVessels, isMapStyleLoaded, aisOverlayStarted, mapUiStateForAis.aisVisible, settingsViewModel.systemSettings.aisCourseExtension) {
        // ✅ 디버깅: 조건 체크
        Log.d("[ChartOnlyScreen]", "LaunchedEffect 트리거: aisVessels=${filteredAisVessels.size}개, isMapStyleLoaded=$isMapStyleLoaded, aisOverlayStarted=$aisOverlayStarted, aisVisible=${mapUiStateForAis.aisVisible}")
        
        // ✅ 스타일이 로드되고 AIS Overlay가 시작되었고 AIS 표시가 ON일 때만 업데이트
        if (isMapStyleLoaded && aisOverlayStarted && mapUiStateForAis.aisVisible) {
            val now = System.currentTimeMillis()
            // ✅ 스로틀링: 500ms에 한 번만
            if (now - lastAisUpdateTime < 500) {
                Log.d("[ChartOnlyScreen]", "스로틀링으로 스킵: ${now - lastAisUpdateTime}ms 경과")
                return@LaunchedEffect
            }
            
            android.util.Log.d("[ChartOnlyScreen]", "✅ AIS 선박 업데이트 시도: ${filteredAisVessels.size}개, 위치 있는 선박: ${filteredAisVessels.count { it.latitude != null && it.longitude != null }}개")
            aisOverlay.updateVessels(filteredAisVessels, settingsViewModel.systemSettings.aisCourseExtension)
            lastAisUpdateTime = now
        } else {
            Log.d("[ChartOnlyScreen]", "조건 미충족으로 스킵: isMapStyleLoaded=$isMapStyleLoaded, aisOverlayStarted=$aisOverlayStarted")
        }
    }
    
    // 위치 업데이트 시작 (지도가 준비되면 즉시 시작, 스타일 로드와 무관)
    // 지도 렌더링과 위치 추적을 분리하여 지도가 먼저 표시되도록 함
    LaunchedEffect(locationManager) {
        if (locationManager != null) {
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
                Log.d("[ChartOnlyScreen]", "위치 추적 시작 (지도 준비 완료, 스타일 로드와 무관)")
            } else {
                // MainActivity의 권한 요청 메서드 호출
                if (activity is MainActivity) {
                    (activity as MainActivity).requestLocationPermission()
                    Log.d("[ChartOnlyScreen]", "위치 권한 요청")
                }
            }
        }
    }
    
    // mapLibreMap이 null이 되면 isMapStyleLoaded도 리셋
    LaunchedEffect(mapLibreMap) {
        if (mapLibreMap == null) {
            isMapStyleLoaded = false
            Log.d("[ChartOnlyScreen]", "mapLibreMap이 null이 되어 isMapStyleLoaded 리셋")
        }
    }
    
    // 컴포저블 해제 시 정리 및 재진입 감지
    DisposableEffect(Unit) {
        // 화면 진입 시 카운터 증가하여 재초기화 트리거
        screenEntryKey++
        isMapStyleLoaded = false
        Log.d("[ChartOnlyScreen]", "화면 진입 감지 (key=$screenEntryKey) - isMapStyleLoaded 리셋하여 재초기화 트리거")
        
        onDispose {
            // 위치 업데이트 중지
            locationManager?.stopLocationUpdates()
            // AIS Overlay 정리
            aisOverlay.stop()
            // 상태 리셋 (다음 진입 시 재로드 보장)
            isMapStyleLoaded = false
            Log.d("[ChartOnlyScreen]", "화면 해제 - 위치 추적 및 Overlay 중지, 상태 리셋")
        }
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
            viewModel.registerPoint(
                latLng = latLng,
                name = finalPointName,
                color = pointUiState.selectedColor,
                iconType = pointUiState.selectedIconType
            )
            
            // 등록 후 포인트 목록 다시 로드
            val savedPoints = viewModel.loadPointsFromLocal()

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
            // SavedPoint를 DataSavedPoint로 변환
            val dataPoint = PointMapper.toDataPoint(point)

            // UseCase를 통해 포인트 삭제
            viewModel.deletePoint(dataPoint)
            
            // 삭제 후 포인트 목록 다시 로드
            val savedPoints = viewModel.loadPointsFromLocal()

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
        if (!settingsViewModel.systemSettings.trackVisible) return

        val trackUiState = trackViewModel.trackUiState

        mapLibreMap?.let { map ->
            // 배치 업데이트: 한 번의 getStyle 콜백에서 모든 항적 처리 (성능 최적화)
            map.getStyle { style ->
                // 여러 항적 동시 기록 지원: 각 항적마다 별도의 소스 ID 사용
                trackUiState.recordingTracks.forEach { (trackId, recordingState) ->
                    // 항적 정보 가져오기 (캐시 사용으로 최적화됨)
                    val track = trackViewModel.getTrack(trackId) ?: return@forEach
                    
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
            if (!settingsViewModel.systemSettings.trackVisible) {
                map.getStyle { style ->
                    locationManager?.moveShipLayerToTop(style)
                }
                return@let
            }

            val trackUiState = trackViewModel.trackUiState
            val highlightedRecord = trackUiState.highlightedTrackRecord
            
            // 모든 항적을 표시하되, 하이라이트된 항적만 효과를 부여
            val tracksToDisplay = trackViewModel.getTracks().filter { it.isVisible }
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
            // SavedPoint를 DataSavedPoint로 변환
            val dataPoint = PointMapper.toDataPoint(originalPoint)

            // UseCase를 통해 포인트 업데이트
            viewModel.updatePoint(
                originalPoint = dataPoint,
                newName = newName,
                newColor = newColor
            )
            
            // 업데이트 후 포인트 목록 다시 로드
            val savedPoints = viewModel.loadPointsFromLocal()

            mapLibreMap?.getStyle { style ->
                val convertedPoints = PointMapper.toUiPoints(savedPoints)
                locationManager?.updatePointsOnMap(convertedPoints)
            }

            Log.d("[ChartPlotterScreen]", "포인트 업데이트 완료: $newName")
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
                            val pointLatLng = LatLng(point.latitude, point.longitude)
                            val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(currentLatLng)
                                .zoom(map.cameraPosition.zoom)
                                .bearing(bearing.toDouble())
                                .build()
                            map.cameraPosition = newPosition
                            viewModel.updateShowCursor(false)
                            viewModel.updateCursorLatLng(null)
                            viewModel.updateCursorScreenPosition(null)
                            if (settingsViewModel.systemSettings.courseLineEnabled) {
                                PMTilesLoader.addCourseLine(map, currentLatLng, pointLatLng)
                            } else {
                                PMTilesLoader.removeCourseLine(map)
                            }
                        } else {
                            PMTilesLoader.removeCourseLine(map)
                        }
                    } ?: run {
                        PMTilesLoader.removeCourseLine(map)
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
        val newPoint = trackViewModel.addTrackPointIfNeeded(latitude, longitude)
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

            // 포인트 등록은 ViewModel을 통해 처리
            viewModel.registerPoint(
                latLng = latLng,
                name = autoPointName,
                color = pointUiState.selectedColor,
                iconType = pointUiState.selectedIconType
            )
            
            // 등록 후 포인트 목록 다시 로드
            val savedPoints = viewModel.loadPointsFromLocal()

            mapLibreMap?.getStyle { style ->
                val convertedPoints = PointMapper.toUiPoints(savedPoints)
                locationManager?.updatePointsOnMap(convertedPoints)
            }

            viewModel.updatePointCount(savedPoints.size)
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
    val trackUiState = trackViewModel.trackUiState
    val routeUiState = routeViewModel.routeUiState
    val dialogUiState = viewModel.dialogUiState

    // 항적 상태 변화 관찰하여 자동으로 표시 업데이트 (디바운싱 적용)
    LaunchedEffect(trackUiState.recordingTracks) {
        // 100ms 지연으로 여러 업데이트를 한 번에 처리 (성능 최적화)
        delay(100)
        updateCurrentTrackDisplay()
        
        // 앱 시작 시 자동 기록을 위한 타이머 시작 (isRecording=true인 항적에 대해서만)
        trackUiState.recordingTracks.forEach { (trackId, recordingState) ->
            val track = trackViewModel.getTracks().find { it.id == trackId }
            if (track != null && track.intervalType == "time") {
                // MainActivity의 타이머 시작
                (activity as MainActivity).startTrackTimerForAutoRecording(track, trackViewModel)
            }
        }
    }

    // 뒤로가기 처리: 설정 화면이나 메뉴가 열려있으면 닫기
    BackHandler(enabled = mapUiState.showMenu || mapUiState.showSettingsScreen) {
        when {
            mapUiState.showSettingsScreen -> viewModel.updateShowSettingsScreen(false)
            mapUiState.showMenu -> {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
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

    // 포인트 편집 선택 다이얼로그 (포인트 변경 시)
    if (dialogUiState.showPointEditSelectionDialog) {
        val points = loadPointsFromLocal()
        AlertDialog(
            onDismissRequest = { viewModel.updateShowPointEditSelectionDialog(false) },
            title = { Text(stringResource(R.string.point_edit_select)) },
            text = {
                if (points.isEmpty()) {
                    Text(stringResource(R.string.point_edit_empty))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(points) { point ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.updateSelectedPoint(point)
                                        viewModel.updateEditPointName(point.name)
                                        viewModel.updateEditSelectedColor(point.color)
                                        viewModel.updateShowPointEditSelectionDialog(false)
                                        viewModel.updateShowEditDialog(true)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = "${point.name} (${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)})",
                                    modifier = Modifier.padding(8.dp),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateShowPointEditSelectionDialog(false) }) {
                    Text("취소")
                }
            }
        )
    }

    // 항적 설정 다이얼로그 (항적별 설정 수정)
    // 항적 생성 다이얼로그 (메뉴에서 항적 생성 클릭 시)
    if (dialogUiState.showTrackCreateDialog) {
        var newTrackName by remember { mutableStateOf("") }
        var newTrackColor by remember { mutableStateOf(Color.Red) }
        var intervalType by remember { mutableStateOf("time") }
        var timeIntervalText by remember { mutableStateOf("5") }
        var distanceIntervalText by remember { mutableStateOf("10") }
        
        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackCreateDialog(false) },
            title = { Text(stringResource(R.string.track_create)) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta)
                            .forEach { color ->
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
                        RadioButton(selected = intervalType == "time", onClick = { intervalType = "time" })
                        Text("시간", modifier = Modifier.padding(start = 4.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = intervalType == "distance", onClick = { intervalType = "distance" })
                        Text("거리", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (intervalType == "time") {
                        TextField(
                            value = timeIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) timeIntervalText = it },
                            label = { Text("시간 간격 (초)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextField(
                            value = distanceIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) distanceIntervalText = it },
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
                            } else 5000L
                            val distanceInterval = if (intervalType == "distance") {
                                distanceIntervalText.toDoubleOrNull() ?: 10.0
                            } else 10.0
                            trackViewModel.addTrack(newTrackName, newTrackColor, intervalType, timeInterval, distanceInterval)
                            updateTrackDisplay()
                            viewModel.updateShowTrackCreateDialog(false)
                        }
                    }
                ) {
                    Text("추가")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateShowTrackCreateDialog(false) }) {
                    Text("취소")
                }
            }
        )
    }

    if (dialogUiState.showTrackSettingsDialog && trackUiState.selectedTrackForSettings != null) {
        val track = trackUiState.selectedTrackForSettings!!
        var intervalType by remember { mutableStateOf(track.intervalType) }
        var timeIntervalText by remember { mutableStateOf((track.timeInterval / 1000L).toString()) }
        var distanceIntervalText by remember { mutableStateOf(track.distanceInterval.toString()) }
        
        AlertDialog(
            onDismissRequest = {
                viewModel.updateShowTrackSettingsDialog(false)
                trackViewModel.updateSelectedTrackForSettings(null)
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
                            trackViewModel.updateTrackSettings(
                                track.id,
                                intervalType,
                                timeInterval,
                                distanceInterval
                            )
                        }
                        viewModel.updateShowTrackSettingsDialog(false)
                        trackViewModel.updateSelectedTrackForSettings(null)
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
                        trackViewModel.updateSelectedTrackForSettings(null)
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }

    // 항적 생성 다이얼로그 (메뉴 > 항적 > 항적 생성)
    if (dialogUiState.showTrackCreateDialog) {
        var newTrackName by remember { mutableStateOf("") }
        var newTrackColor by remember { mutableStateOf(Color.Red) }
        var intervalType by remember { mutableStateOf("time") }
        var timeIntervalText by remember { mutableStateOf("5") }
        var distanceIntervalText by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackCreateDialog(false) },
            title = { Text(stringResource(R.string.track_create)) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta)
                            .forEach { color ->
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
                        RadioButton(selected = intervalType == "time", onClick = { intervalType = "time" })
                        Text("시간", modifier = Modifier.padding(start = 4.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = intervalType == "distance", onClick = { intervalType = "distance" })
                        Text("거리", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (intervalType == "time") {
                        TextField(
                            value = timeIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) timeIntervalText = it },
                            label = { Text("시간 간격 (초)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextField(
                            value = distanceIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) distanceIntervalText = it },
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
                            } else 5000L
                            val distanceInterval = if (intervalType == "distance") {
                                distanceIntervalText.toDoubleOrNull() ?: 10.0
                            } else 10.0
                            trackViewModel.addTrack(
                                newTrackName, newTrackColor,
                                intervalType, timeInterval, distanceInterval
                            )
                            viewModel.updateShowTrackCreateDialog(false)
                            updateTrackDisplay()
                        }
                    }
                ) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateShowTrackCreateDialog(false) }) { Text("취소") }
            }
        )
    }

    // 항적 생성 다이얼로그 (메뉴 > 항적 > 항적 생성)
    if (dialogUiState.showTrackCreateDialog) {
        var newTrackName by remember { mutableStateOf("") }
        var newTrackColor by remember { mutableStateOf(Color.Red) }
        var intervalType by remember { mutableStateOf("time") }
        var timeIntervalText by remember { mutableStateOf("5") }
        var distanceIntervalText by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackCreateDialog(false) },
            title = { Text(stringResource(R.string.track_create)) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Color.Red, Color.Blue, Color.Green,
                            Color.Yellow, Color.Cyan, Color.Magenta
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
                            onValueChange = { if (it.all { c -> c.isDigit() }) timeIntervalText = it },
                            label = { Text("시간 간격 (초)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextField(
                            value = distanceIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) distanceIntervalText = it },
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
                            } else 5000L
                            val distanceInterval = if (intervalType == "distance") {
                                distanceIntervalText.toDoubleOrNull() ?: 10.0
                            } else 10.0
                            trackViewModel.addTrack(
                                newTrackName, newTrackColor,
                                intervalType, timeInterval, distanceInterval
                            )
                            updateTrackDisplay()
                            viewModel.updateShowTrackCreateDialog(false)
                        }
                    }
                ) {
                    Text("추가")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateShowTrackCreateDialog(false) }) {
                    Text("취소")
                }
            }
        )
    }

    // 항적 생성 다이얼로그 (메뉴 > 항적 > 항적 생성)
    if (dialogUiState.showTrackCreateDialog) {
        var newTrackName by remember { mutableStateOf("") }
        var newTrackColor by remember { mutableStateOf(Color.Red) }
        var intervalType by remember { mutableStateOf("time") }
        var timeIntervalText by remember { mutableStateOf("5") }
        var distanceIntervalText by remember { mutableStateOf("10") }
        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackCreateDialog(false) },
            title = { Text(stringResource(R.string.track_create)) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Color.Red, Color.Blue, Color.Green,
                            Color.Yellow, Color.Cyan, Color.Magenta
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
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("기록 기준:")
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
                    if (intervalType == "time") {
                        TextField(
                            value = timeIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) timeIntervalText = it },
                            label = { Text("시간 간격 (초)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextField(
                            value = distanceIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) distanceIntervalText = it },
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
                            } else 5000L
                            val distanceInterval = if (intervalType == "distance") {
                                distanceIntervalText.toDoubleOrNull() ?: 10.0
                            } else 10.0
                            trackViewModel.addTrack(
                                newTrackName, newTrackColor,
                                intervalType, timeInterval, distanceInterval
                            )
                            viewModel.updateShowTrackCreateDialog(false)
                            updateTrackDisplay()
                        }
                    }
                ) {
                    Text("추가")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateShowTrackCreateDialog(false) }) {
                    Text("취소")
                }
            }
        )
    }

    // 항적 생성 다이얼로그
    if (dialogUiState.showTrackCreateDialog) {
        var newTrackName by remember { mutableStateOf("") }
        var newTrackColor by remember { mutableStateOf(Color.Red) }
        var intervalType by remember { mutableStateOf("time") }
        var timeIntervalText by remember { mutableStateOf("5") }
        var distanceIntervalText by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { viewModel.updateShowTrackCreateDialog(false) },
            title = { Text(stringResource(R.string.track_create)) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta)
                            .forEach { color ->
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
                        RadioButton(selected = intervalType == "time", onClick = { intervalType = "time" })
                        Text("시간", modifier = Modifier.padding(start = 4.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = intervalType == "distance", onClick = { intervalType = "distance" })
                        Text("거리", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (intervalType == "time") {
                        TextField(
                            value = timeIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) timeIntervalText = it },
                            label = { Text("시간 간격 (초)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextField(
                            value = distanceIntervalText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) distanceIntervalText = it },
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
                            val timeInterval = if (intervalType == "time") (timeIntervalText.toLongOrNull() ?: 5L) * 1000L else 5000L
                            val distanceInterval = if (intervalType == "distance") distanceIntervalText.toDoubleOrNull() ?: 10.0 else 10.0
                            trackViewModel.addTrack(newTrackName, newTrackColor, intervalType, timeInterval, distanceInterval)
                            viewModel.updateShowTrackCreateDialog(false)
                        }
                    }
                ) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.updateShowTrackCreateDialog(false) }) { Text("취소") }
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
                        items(trackViewModel.getTracks()) { track ->
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
                                                        val success = trackViewModel.setTrackVisibility(
                                                            track.id,
                                                            it
                                                        ) {
                                                            viewModel.updateShowTrackLimitDialog(true)
                                                        }
                                                        if (success) {
                                                            updateTrackDisplay()
                                                        }
                                                    }
                                                )
                                            }

                                            // 기록 on/off 스위치 (단일 항적만 기록 가능)
                                            val isRecording = trackViewModel.trackUiState.recordingTracks.containsKey(track.id)
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
                                                        trackViewModel.toggleTrackRecording(track.id)
                                                        updateTrackDisplay()
                                                    }
                                                )
                                            }

                                            // 설정 버튼
                                            TextButton(
                                                onClick = {
                                                    // 항적 설정 다이얼로그 표시
                                                    trackViewModel.updateSelectedTrackForSettings(track)
                                                    viewModel.updateShowTrackSettingsDialog(true)
                                                }
                                            ) {
                                                Text("설정", fontSize = 12.sp)
                                            }

                                            // 삭제 버튼
                                            TextButton(
                                                onClick = {
                                                    if (isRecording) {
                                                        trackViewModel.stopTrackRecording(track.id)
                                                    }
                                                    trackViewModel.deleteTrack(track.id)
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
                                            trackViewModel.getDatesByTrackId(track.id)
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
                                                            trackViewModel.updateHighlightedTrackRecord(null)
                                                        } else {
                                                            // 하이라이트 처리 (날짜별)
                                                            trackViewModel.updateHighlightedTrackRecord(
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
                                                        trackViewModel.deleteTrackPointsByDate(
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
                                trackViewModel.addTrack(
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
                trackViewModel.getDatesByTrackId(trackUiState.selectedTrackForRecords!!.id)
            }
        }
        
        AlertDialog(
            onDismissRequest = {
                viewModel.updateShowTrackRecordListDialog(false)
                trackViewModel.updateSelectedTrackForRecords(null)
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
                                trackViewModel.getTrackPointsByDate(trackUiState.selectedTrackForRecords!!.id, date).size
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (isHighlighted) {
                                        // 이미 하이라이트된 경우: 취소
                                        trackViewModel.updateHighlightedTrackRecord(null)
                                    } else {
                                        // 하이라이트 처리
                                        trackViewModel.updateHighlightedTrackRecord(
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
                            trackViewModel.updateHighlightedTrackRecord(null)
                            updateTrackDisplay()
                        }
                    ) {
                        Text("하이라이트 해제")
                    }
                    TextButton(
                        onClick = {
                            viewModel.updateShowTrackRecordListDialog(false)
                            trackViewModel.updateSelectedTrackForRecords(null)
                        }
                    ) {
                        Text("닫기")
                    }
                }
            }
        )
    }

    // 경로 편집 중: 점 추가 시 지도 업데이트
    val systemSettings = settingsViewModel.systemSettings
    LaunchedEffect(routeUiState.isEditingRoute, routeUiState.editingRoutePoints) {
        if (routeUiState.isEditingRoute && mapLibreMap != null) {
            val editingPoints = routeUiState.editingRoutePoints
            if (editingPoints.isNotEmpty()) {
                val routePoints = editingPoints.map { 
                    org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) 
                }
                val routeId = routeUiState.selectedRoute?.id ?: "editing_route"
                
                // 경로 편집 중에는 항상 표시, 편집 중이 아닐 때는 routeVisible 설정 확인
                val shouldShowRoute = routeUiState.isEditingRoute || systemSettings.routeVisible
                
                if (shouldShowRoute) {
                    // 경로 선 업데이트 (2개 이상일 때만)
                    if (editingPoints.size >= 2) {
                        PMTilesLoader.addRouteLine(mapLibreMap!!, routeId, routePoints)
                    }
                    
                    // 점 마커 표시 (모든 점, 1개일 때도 표시)
                    PMTilesLoader.addRoutePoints(mapLibreMap!!, routeId, routePoints)
                    
                    Log.d("[ChartOnlyScreen]", "경로 점 업데이트: ${editingPoints.size}개")
                } else {
                    // 표시하지 않으면 제거
                    mapLibreMap?.let { map ->
                        PMTilesLoader.removeRouteLine(map, routeId)
                    }
                }
            } else {
                // 점이 없으면 제거
                mapLibreMap?.let { map ->
                    PMTilesLoader.removeRouteLine(map, "editing_route")
                }
            }
        } else if (!routeUiState.isEditingRoute) {
            // 편집 모드가 종료되면 편집 중 경로 제거
            mapLibreMap?.let { map ->
                PMTilesLoader.removeRouteLine(map, "editing_route")
            }
        }
    }
    
    // trackVisible 설정 변경 시 항적 표시 업데이트
    LaunchedEffect(systemSettings.trackVisible) {
        updateTrackDisplay()
    }

    // gridLineEnabled(위경도선) 설정 변경 시 격자선 표시/제거
    LaunchedEffect(systemSettings.gridLineEnabled, mapLibreMap, isMapStyleLoaded) {
        mapLibreMap?.let { map ->
            if (systemSettings.gridLineEnabled && isMapStyleLoaded) {
                PMTilesLoader.addGridLines(map)
            } else {
                PMTilesLoader.removeGridLines(map)
            }
        }
    }

    // destinationVisible(목적지) 설정 변경 시 목적지 마커 표시/제거
    LaunchedEffect(systemSettings.destinationVisible, mapUiState.navigationPoint, mapLibreMap, isMapStyleLoaded) {
        mapLibreMap?.let { map ->
            if (!systemSettings.destinationVisible) {
                PMTilesLoader.removeNavigationMarker(map)
            } else if (isMapStyleLoaded && mapUiState.navigationPoint != null) {
                val np = mapUiState.navigationPoint!!
                PMTilesLoader.addNavigationMarker(map, LatLng(np.latitude, np.longitude), np.name)
            }
        }
    }

    // mapHidden(지도 감춤) 설정 변경 시 지도 배경 레이어 표시/숨김
    LaunchedEffect(systemSettings.mapHidden, mapLibreMap, isMapStyleLoaded) {
        mapLibreMap?.let { map ->
            if (isMapStyleLoaded) {
                if (systemSettings.mapHidden) {
                    PMTilesLoader.addMapHiddenOverlay(map)
                } else {
                    PMTilesLoader.removeMapHiddenOverlay(map)
                }
            }
        }
    }

    // distanceCircleRadius(거리원) 설정 변경 시 거리원 표시/업데이트
    LaunchedEffect(systemSettings.distanceCircleRadius, mapLibreMap, isMapStyleLoaded, viewModel.gpsUiState.lastGpsLocation) {
        mapLibreMap?.let { map ->
            if (isMapStyleLoaded && systemSettings.distanceCircleRadius > 0f && viewModel.gpsUiState.lastGpsLocation != null) {
                val loc = viewModel.gpsUiState.lastGpsLocation!!
                PMTilesLoader.updateDistanceCircle(map, loc, systemSettings.distanceCircleRadius)
            } else {
                PMTilesLoader.removeDistanceCircle(map)
            }
        }
    }

    // courseLineEnabled 설정 변경 시 코스업 선 업데이트
    LaunchedEffect(systemSettings.courseLineEnabled, mapUiState.mapDisplayMode, mapUiState.coursePoint, viewModel.gpsUiState.lastGpsLocation) {
        if (mapUiState.mapDisplayMode == "코스업") {
            updateMapRotation()
        }
    }

    // boat3DEnabled 설정 변경 시 선박 아이콘 2D/3D 갱신
    LaunchedEffect(systemSettings.boat3DEnabled, mapLibreMap, isMapStyleLoaded) {
        if (isMapStyleLoaded) {
            locationManager?.refreshShipIcon(systemSettings.boat3DEnabled)
        }
    }

    // headingLineEnabled(해딩연장) 설정 변경 시 해딩연장선 표시/제거
    LaunchedEffect(systemSettings.headingLineEnabled, systemSettings.extensionLength, mapLibreMap, isMapStyleLoaded, viewModel.gpsUiState.lastGpsLocation, viewModel.gpsUiState.cog) {
        mapLibreMap?.let { map ->
            if (!systemSettings.headingLineEnabled || !isMapStyleLoaded || systemSettings.extensionLength <= 0f || viewModel.gpsUiState.lastGpsLocation == null) {
                PMTilesLoader.removeHeadingLine(map)
            } else {
                val loc = viewModel.gpsUiState.lastGpsLocation!!
                val latLng = LatLng(loc.latitude, loc.longitude)
                PMTilesLoader.addHeadingLine(map, latLng, viewModel.gpsUiState.cog, systemSettings.extensionLength)
            }
        }
    }

    // routeVisible 설정 변경 시 모든 경로 표시 업데이트
    LaunchedEffect(systemSettings.routeVisible, routeUiState.isEditingRoute) {
        if (!routeUiState.isEditingRoute && mapLibreMap != null) {
            val allRoutes = routeViewModel.getAllRoutes()
            if (systemSettings.routeVisible) {
                // 모든 경로 표시
                allRoutes.forEach { route ->
                    val routePoints = route.points.map { 
                        org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) 
                    }
                    if (routePoints.size >= 2) {
                        PMTilesLoader.addRouteLine(mapLibreMap!!, route.id, routePoints)
                    }
                    PMTilesLoader.addRoutePoints(mapLibreMap!!, route.id, routePoints)
                }
            } else {
                // 모든 경로 제거
                allRoutes.forEach { route ->
                    PMTilesLoader.removeRouteLine(mapLibreMap!!, route.id)
                }
            }
        }
    }
    
    // 경로 목록 변경 시 경로 표시 업데이트
    LaunchedEffect(Unit) {
        if (mapLibreMap != null && systemSettings.routeVisible && !routeUiState.isEditingRoute) {
            val allRoutes = routeViewModel.getAllRoutes()
            allRoutes.forEach { route ->
                val routePoints = route.points.map { 
                    org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) 
                }
                if (routePoints.size >= 2) {
                    PMTilesLoader.addRouteLine(mapLibreMap!!, route.id, routePoints)
                }
                PMTilesLoader.addRoutePoints(mapLibreMap!!, route.id, routePoints)
            }
        }
    }
    

    // 경로 생성 설명 다이얼로그
    if (routeUiState.showRouteCreateDialog) {
        AlertDialog(
            onDismissRequest = { routeViewModel.updateShowRouteCreateDialog(false) },
            title = { Text("경로 생성") },
            text = {
                Column {
                    Text("경로를 생성하는 방법:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. '생성' 버튼을 누르면 경로 생성 모드가 시작됩니다", fontSize = 12.sp)
                    Text("2. 지도를 클릭하여 경로에 점을 추가하세요", fontSize = 12.sp)
                    Text("3. 점이 추가되면 파란색 원으로 표시됩니다", fontSize = 12.sp)
                    Text("4. 최소 2개 이상의 점을 추가하세요", fontSize = 12.sp)
                    Text("5. '완료' 버튼을 눌러 경로 이름을 입력하고 저장하세요", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("[ChartOnlyScreen]", "경로 생성 버튼 클릭 - 편집 모드 시작")
                        routeViewModel.updateShowRouteCreateDialog(false)
                        viewModel.updateShowMenu(false)  // 경로 생성 시 메뉴 사이드바 숨김
                        // 경로 편집 모드 시작
                        routeViewModel.setEditingRoute(true)
                        routeViewModel.setEditingRoutePoints(emptyList())
                        routeViewModel.selectRoute(null)
                        Log.d("[ChartOnlyScreen]", "편집 모드 설정 완료: isEditingRoute=${routeViewModel.routeUiState.isEditingRoute}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyColor)
                ) {
                    Text("생성", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { routeViewModel.updateShowRouteCreateDialog(false) }) {
                    Text("취소")
                }
            }
        )
    }

    // 경로 선택 다이얼로그 (항해 시작용)
    if (dialogUiState.showRouteSelectionForNavDialog) {
        val routes = routeViewModel.getAllRoutes()
        AlertDialog(
            onDismissRequest = { viewModel.updateShowRouteSelectionForNavDialog(false) },
            title = { Text(stringResource(R.string.nav_start_from_route)) },
            text = {
                if (routes.isEmpty()) {
                    Text(
                        stringResource(R.string.no_routes_saved),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(routes) { route ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val currentLocation = locationManager?.getCurrentLocationObject()
                                        val currentLatLng = currentLocation?.let {
                                            LatLng(it.latitude, it.longitude)
                                        } ?: mapLibreMap?.cameraPosition?.target
                                        viewModel.setRouteAsNavigation(route, currentLatLng)
                                        mapLibreMap?.let { map ->
                                            val mapUiState = viewModel.mapUiState
                                            val waypoints = mapUiState.waypoints.map {
                                                LatLng(it.latitude, it.longitude)
                                            }
                                            val destination = mapUiState.navigationPoint
                                            if (destination != null) {
                                                val startPoint = currentLatLng ?: map.cameraPosition.target
                                                if (startPoint != null) {
                                                    PMTilesLoader.addNavigationRoute(
                                                        map,
                                                        startPoint,
                                                        waypoints,
                                                        LatLng(destination.latitude, destination.longitude)
                                                    )
                                                }
                                                if (settingsViewModel.systemSettings.destinationVisible) {
                                                    PMTilesLoader.addNavigationMarker(
                                                        map,
                                                        LatLng(destination.latitude, destination.longitude),
                                                        destination.name
                                                    )
                                                }
                                            }
                                        }
                                        viewModel.updateShowRouteSelectionForNavDialog(false)
                                    },
                                colors = CardDefaults.cardColors(containerColor = RouteCardBg)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = route.name,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.points_count, route.points.size),
                                        fontSize = 12.sp,
                                        color = Color.Black.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateShowRouteSelectionForNavDialog(false) }) {
                    Text("닫기")
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

    // 포인트 선택 다이얼로그 (항해 목적지/코스업용)
    if (dialogUiState.showPointSelectionDialog) {
        val isNavMenu = mapUiState.currentMenu == "navigation"
        AlertDialog(
            onDismissRequest = {
                viewModel.updateShowPointSelectionDialog(false)
            },
            title = {
                Text(if (isNavMenu) stringResource(R.string.nav_dest_select) else "코스업 포인트 선택")
            },
            text = {
                Column {
                    Text(if (isNavMenu) stringResource(R.string.nav_dest_select_desc) else "코스업으로 사용할 포인트를 선택하세요:")
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
                                            if (settingsViewModel.systemSettings.destinationVisible) {
                                                mapLibreMap?.let { map ->
                                                    val navigationLatLng = LatLng(point.latitude, point.longitude)
                                                    PMTilesLoader.addNavigationMarker(
                                                        map,
                                                        navigationLatLng,
                                                        point.name
                                                    )
                                                }
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
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                // 메뉴창 또는 설정 화면이 열려있을 때는 플로팅 버튼 숨김
                if (!mapUiState.showMenu && !mapUiState.showSettingsScreen) {
                    // 현재 위치 버튼 (우측 하단)
                    FloatingActionButton(
                        onClick = {
                            locationManager?.startAutoTracking()
                            // 현재 위치로 이동할 때 커서 숨김
                            viewModel.updateShowCursor(false)
                            viewModel.updateCursorLatLng(null)
                            viewModel.updateCursorScreenPosition(null)
                        },
                        modifier = Modifier.size(56.dp),
                        containerColor = Color(0xFF001F3F),
                        contentColor = Color.White
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
            fontSize = settingsViewModel.systemSettings.fontSize,
            isDialogShown = dialogUiState.showDialog ||
                    dialogUiState.showPointManageDialog ||
                    dialogUiState.showEditDialog ||
                    dialogUiState.showPointDeleteList ||
                    dialogUiState.showPointEditSelectionDialog ||
                    dialogUiState.showPointSelectionDialog ||
                    dialogUiState.showRouteSelectionForNavDialog ||
                    dialogUiState.showWaypointDialog ||
                    dialogUiState.showTrackSettingsDialog ||
                    dialogUiState.showTrackCreateDialog ||
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
                
                // mapLibreMap 설정 시마다 isMapStyleLoaded 리셋하여 재초기화 보장
                val isFirstInit = !mapUiState.isMapInitialized
                if (isFirstInit) {
                    mapLibreMap = map
                    viewModel.updateIsMapInitialized(true)
                } else {
                    // 화면 재진입 시 상태 리셋하여 재초기화 트리거
                    isMapStyleLoaded = false
                    mapLibreMap = map
                    Log.d("[ChartOnlyScreen]", "화면 재진입 감지 - isMapStyleLoaded 리셋하여 재초기화 트리거")
                }
                
                locationManager = LocationManager(
                    activity,
                    map,
                        onGpsLocationUpdate = { lat, lng, available ->
                            viewModel.updateGpsLocation(lat, lng, available)
                            
                            // AIS ViewModel에 현재 위치 업데이트
                            aisViewModel.updateLocation(lat, lng)

                            // 항적 기록 점 추가 (MainActivity의 addTrackPointIfNeeded 호출)
                            if (activity is MainActivity) {
                                (activity as MainActivity).addTrackPointIfNeeded(lat, lng, trackViewModel)
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

                            // 항해 중인 경로가 있으면 현재 위치 기준으로 재연결 (선박 이동 시 계속 업데이트)
                            val currentNavigationRoute = mapUiState.currentNavigationRoute
                            if (currentNavigationRoute != null && mapUiState.navigationPoint != null) {
                                val currentLatLng = LatLng(lat, lng)
                                try {
                                    val connectRouteToNavigationUseCase = ConnectRouteToNavigationUseCase(
                                        CalculateDistanceUseCase()
                                    )
                                    val (waypoints, destination) = connectRouteToNavigationUseCase.execute(
                                        currentNavigationRoute,
                                        currentLatLng
                                    )
                                    viewModel.updateWaypoints(waypoints)
                                    viewModel.updateNavigationPoint(destination)
                                    Log.d("[ChartOnlyScreen]", "항해 경로 재연결: 선박 이동에 따라 경로 업데이트")
                                } catch (e: Exception) {
                                    Log.e("[ChartOnlyScreen]", "경로 재연결 실패: ${e.message}")
                                }
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
                    // 스타일 로드는 비동기로 처리하여 UI 블로킹 방지
                    map.getStyle { style ->
                        val wasAlreadyLoaded = isMapStyleLoaded
                        isMapStyleLoaded = true
                        
                        // 지도 조작은 반드시 메인 스레드에서 수행 (MapLibre 요구사항)
                        // 선박 아이콘과 포인트 레이어 추가 (메인 스레드)
                        // 화면 전환 후 재진입 시에도 다시 추가되도록 항상 실행
                        locationManager?.addShipToMap(style)
                        locationManager?.addPointsToMap(style)
                        // AIS 선박은 AISOverlay에서 처리
                        
                        // 화면 재진입 시 항해 경로 재생성 (wasAlreadyLoaded가 true면 재진입)
                        if (wasAlreadyLoaded) {
                            Log.d("[ChartOnlyScreen]", "지도 스타일 재로드 감지 - 모든 레이어 재추가 시작")
                            
                            val mapUiState = viewModel.mapUiState
                            
                            // 항해 경로 재생성 (navigationPoint가 있으면)
                            if (mapUiState.navigationPoint != null && settingsViewModel.systemSettings.destinationVisible) {
                                val navigationLatLng = LatLng(
                                    mapUiState.navigationPoint.latitude,
                                    mapUiState.navigationPoint.longitude
                                )
                                PMTilesLoader.addNavigationMarker(
                                    map,
                                    navigationLatLng,
                                    mapUiState.navigationPoint.name
                                )
                                
                                // 위치 정보가 있으면 항해 경로도 그리기
                                if (locationManager?.getCurrentLocationObject() != null) {
                                    updateNavigationRouteUseCase.execute(
                                        map,
                                        locationManager?.getCurrentLocationObject(),
                                        mapUiState.waypoints,
                                        mapUiState.navigationPoint
                                    )
                                    Log.d("[ChartOnlyScreen]", "항해 경로 및 목적지 마커 재생성 완료")
                                } else {
                                    Log.d("[ChartOnlyScreen]", "목적지 마커 표시 (위치 정보 대기 중, GPS 수신 시 경로 자동 생성)")
                                }
                            }
                            
                            // AIS Overlay 재시작 (coroutineScope 내에서 delay 호출) - aisVisible일 때만
                            if (mapUiState.aisVisible) {
                                aisOverlay.stop()
                                aisOverlayStarted = false // ✅ 시작 플래그 리셋
                                coroutineScope.launch {
                                    delay(100)
                                    // ✅ 초기 선박 데이터를 start()에 전달하여 스타일 로드 후 자동 업데이트
                                    aisOverlay.start(map, aisVessels)
                                    aisOverlayStarted = true // ✅ 시작 플래그 설정
                                }
                            } else {
                                aisOverlay.stop()
                                aisOverlayStarted = false
                            }

                            // 위경도선(gridLineEnabled) 재추가
                            if (settingsViewModel.systemSettings.gridLineEnabled) {
                                PMTilesLoader.addGridLines(map)
                            }
                            
                            Log.d("[ChartOnlyScreen]", "지도 스타일 재로드 - 모든 레이어 재추가 완료")
                        }
                        
                        // 포인트 데이터 로드만 백그라운드에서 처리
                        coroutineScope.launch(Dispatchers.IO) {
                            // 저장된 포인트들을 지도에 표시 (백그라운드에서 로드)
                            val savedPoints = loadPointsFromLocal()
                            
                            // 메인 스레드로 전환하여 지도 업데이트
                            withContext(Dispatchers.Main) {
                                locationManager?.updatePointsOnMap(savedPoints)
                                
                                // 앱 시작 시 항적 표시 (항적 화면 노출이 켜져있으면 최근 기록 표시)
                                updateTrackDisplay()
                            }
                        }
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
                        // 경로 편집 모드인 경우: 위치 이동 / 기존 점 클릭 / 새 점 추가
                        // ✅ ViewModel에서 최신 상태를 직접 가져와서 클로저 캡처 문제 해결
                        val currentRouteUiState = routeViewModel.routeUiState
                        val currentMapUiState = viewModel.mapUiState
                        if (currentRouteUiState.isEditingRoute) {
                            
                            // 1) 위치 이동 모드인 경우: 클릭한 곳으로 점 이동
                            val movingOrder = currentRouteUiState.movingPointOrder
                            if (movingOrder != null) {
                                Log.d("[ChartOnlyScreen]", "점 #${movingOrder + 1} 위치 이동: ${latLng.latitude}, ${latLng.longitude}")
                                routeViewModel.updatePointInEditingRoute(movingOrder, latLng.latitude, latLng.longitude)
                                routeViewModel.setMovingPointOrder(null)
                                showRoutePointEditDialog = false
                                selectedRoutePointForEdit = null
                                return@addOnMapClickListener true
                            }
                            
                            // 2) 기존 경로 점 클릭 감지 (터치 위치 근처에 기존 점이 있는지 확인)
                            val screenPoint = map.projection.toScreenLocation(latLng)
                            val editingPoints = currentRouteUiState.editingRoutePoints.sortedBy { it.order }
                            var clickedExistingPoint: RoutePoint? = null
                            
                            for (point in editingPoints) {
                                val pointLatLng = LatLng(point.latitude, point.longitude)
                                val pointScreen = map.projection.toScreenLocation(pointLatLng)
                                val dx = screenPoint.x - pointScreen.x
                                val dy = screenPoint.y - pointScreen.y
                                val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                                if (distance < 40.0) { // 40px 이내면 클릭으로 간주
                                    clickedExistingPoint = point
                                    break
                                }
                            }
                            
                            if (clickedExistingPoint != null) {
                                // 기존 점 클릭 → 편집 다이얼로그 표시
                                Log.d("[ChartOnlyScreen]", "기존 경로 점 클릭: #${clickedExistingPoint.order}")
                                selectedRoutePointForEdit = clickedExistingPoint
                                showRoutePointEditDialog = true
                            } else {
                                // 3) 빈 곳 클릭 → 새 점 추가
                                Log.d("[ChartOnlyScreen]", "지도 클릭 - 경로 편집 모드: ${latLng.latitude}, ${latLng.longitude}")
                                routeViewModel.addPointToEditingRoute(latLng.latitude, latLng.longitude)
                                Log.d("[ChartOnlyScreen]", "점 추가 완료: 총 ${routeViewModel.routeUiState.editingRoutePoints.size}개")
                            }
                            
                            // 십자가 커서 표시하지 않음
                            return@addOnMapClickListener true
                        }
                        
                        // 경유지 추가 모드인 경우: 커서만 표시
                        val currentDialogUiState = viewModel.dialogUiState
                        if (currentDialogUiState.isAddingWaypoint) {
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

                            // AIS 클릭 감지 (일반/즐겨찾기 삼각형, 원, 라벨 모두 포함)
                            val aisFeatures = map.queryRenderedFeatures(
                                android.graphics.PointF(screenPoint.x, screenPoint.y),
                                "ais-vessels-circle",
                                "ais-vessels-triangle",
                                "ais-vessels-watchlist-triangle",
                                "ais-vessels-label"
                            )
                            if (aisFeatures.isNotEmpty()) {
                                val ais = aisFeatures.first()
                                val mmsi = try { ais.getStringProperty("mmsi") } catch (_: Exception) { null }
                                
                                // MMSI로 선박 찾기
                                if (mmsi != null) {
                                    val vessel = aisVessels.find { it.mmsi == mmsi }
                                    if (vessel != null) {
                                        selectedAISVessel = vessel
                                    }
                                }
                                
                                return@addOnMapClickListener true
                            }

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

                    // 위치 업데이트는 LaunchedEffect에서 처리 (화면 재진입 시 자동 재시작)
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

        // 경로 편집 중 배너 (상단 표시) - 다른 오버레이 위에 표시
        if (routeUiState.isEditingRoute) {
            Log.d("[ChartOnlyScreen]", "경로 편집 배너 표시: isEditingRoute=${routeUiState.isEditingRoute}, points=${routeUiState.editingRoutePoints.size}")
            var showNameDialog by remember { mutableStateOf(false) }
            var routeName by remember { mutableStateOf(routeUiState.selectedRoute?.name ?: "Route ${routeViewModel.getAllRoutes().size + 1}") }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteCardBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (routeUiState.selectedRoute != null)
                                    "경로 수정 중: ${routeUiState.selectedRoute!!.name}"
                                else
                                    "경로 생성 중",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                "지도를 클릭하여 점을 추가하세요 (${routeUiState.editingRoutePoints.size}개)",
                                color = Color.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Row {
                            Button(
                                onClick = {
                                    if (routeUiState.editingRoutePoints.size >= 2) {
                                        showNameDialog = true
                                    }
                                },
                                enabled = routeUiState.editingRoutePoints.size >= 2,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NavyColor
                                )
                            ) {
                                Text("완료", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    routeViewModel.setEditingRoute(false)
                                    routeViewModel.setEditingRoutePoints(emptyList())
                                    routeViewModel.selectRoute(null)
                                    // 편집 중인 경로 선 제거
                                    mapLibreMap?.let { map ->
                                        PMTilesLoader.removeRouteLine(map, "editing_route")
                                        routeUiState.selectedRoute?.id?.let { routeId ->
                                            PMTilesLoader.removeRouteLine(map, routeId)
                                        }
                                    }
                                }
                            ) {
                                Text("취소", color = Color.Black)
                            }
                        }
                    }
                }
            }
            
            // 경로 이름 입력 다이얼로그
            if (showNameDialog) {
                AlertDialog(
                    onDismissRequest = { showNameDialog = false },
                    title = {
                        Text(
                            if (routeUiState.selectedRoute != null) "경로 이름 수정" else "경로 이름 입력"
                        )
                    },
                    text = {
                        TextField(
                            value = routeName,
                            onValueChange = { routeName = it },
                            label = { Text("경로 이름") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (routeName.isNotBlank() && routeUiState.editingRoutePoints.size >= 2) {
                                    if (routeUiState.selectedRoute != null) {
                                        // 기존 경로 업데이트
                                        val updatedRoute = routeUiState.selectedRoute.copy(
                                            name = routeName,
                                            points = routeUiState.editingRoutePoints,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        routeViewModel.updateRoute(updatedRoute)
                                        // 지도에 경로 표시
                                        mapLibreMap?.let { map ->
                                            val routePoints = updatedRoute.points.map { 
                                                org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) 
                                            }
//                                            PMTilesLoader.addRouteLine(map, updatedRoute.id, routePoints)
                                        }
                                    } else {
                                        // 새 경로 생성
                                        val newRoute = routeViewModel.createRoute(routeName, routeUiState.editingRoutePoints)
                                        // 지도에 경로 표시
                                        mapLibreMap?.let { map ->
                                            val routePoints = newRoute.points.map { 
                                                org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) 
                                            }
//                                            PMTilesLoader.addRouteLine(map, newRoute.id, routePoints)
                                        }
                                    }
                                    // 편집 중 표시된 경로 선과 점 마커 제거
                                    mapLibreMap?.let { map ->
                                        val routeId = routeUiState.selectedRoute?.id ?: "editing_route"
                                        PMTilesLoader.removeRouteLine(map, routeId)
                                    }
                                    routeViewModel.setEditingRoute(false)
                                    routeViewModel.setEditingRoutePoints(emptyList())
                                    routeViewModel.selectRoute(null)
                                    showNameDialog = false
                                }
                            },
                            enabled = routeName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyColor)
                        ) {
                            Text(
                                if (routeUiState.selectedRoute != null) "수정" else "생성",
                                color = Color.White
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNameDialog = false }) {
                            Text("취소")
                        }
                    }
                )
            }
            
            // 경로 점 편집 다이얼로그 (위치 이동 모드가 아닐 때만 표시)
            if (showRoutePointEditDialog && selectedRoutePointForEdit != null && routeUiState.movingPointOrder == null) {
                val point = selectedRoutePointForEdit!!
                
                AlertDialog(
                    onDismissRequest = { 
                        showRoutePointEditDialog = false
                        selectedRoutePointForEdit = null
                    },
                    title = { Text("경로 점 #${point.order + 1} 편집") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "위도: ${"%.6f".format(point.latitude)}",
                                fontSize = 14.sp
                            )
                            Text(
                                "경도: ${"%.6f".format(point.longitude)}",
                                fontSize = 14.sp
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // 위치 이동 버튼 (지도 터치) → ViewModel에 movingPointOrder 설정
                            Button(
                                onClick = {
                                    routeViewModel.setMovingPointOrder(point.order)
                                    // 다이얼로그를 닫으면, 다음 클릭이 기존 리스너에서 위치 이동으로 처리됨
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NavyColor
                                )
                            ) {
                                Text("위치 이동 (지도 터치)", color = Color.White)
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text("순서 변경", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            // 순서 변경 버튼
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        routeViewModel.movePointUpInEditingRoute(point.order)
                                        showRoutePointEditDialog = false
                                        selectedRoutePointForEdit = null
                                    },
                                    enabled = point.order > 0,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyColor)
                                ) {
                                    Text("▲ 위로", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        routeViewModel.movePointDownInEditingRoute(point.order)
                                        showRoutePointEditDialog = false
                                        selectedRoutePointForEdit = null
                                    },
                                    enabled = point.order < routeUiState.editingRoutePoints.size - 1,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyColor)
                                ) {
                                    Text("▼ 아래로", color = Color.White)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        // 삭제 버튼
                        Button(
                            onClick = {
                                routeViewModel.removePointFromEditingRoute(point.order)
                                showRoutePointEditDialog = false
                                selectedRoutePointForEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("삭제", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showRoutePointEditDialog = false
                            selectedRoutePointForEdit = null
                        }) {
                            Text("닫기")
                        }
                    }
                )
            }
            
            // 위치 이동 모드 안내 배너 (movingPointOrder가 설정되었을 때)
            if (routeUiState.movingPointOrder != null) {
                val movingOrder = routeUiState.movingPointOrder!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = RouteCardBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "점 #${movingOrder + 1} 위치 이동 중",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "지도를 터치하여 새 위치를 지정하세요",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            TextButton(
                                onClick = {
                                    routeViewModel.setMovingPointOrder(null)
                                }
                            ) {
                                Text("취소", color = Color.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 설정 화면 (지도 위 오버레이)
        if (mapUiState.showSettingsScreen) {
            SettingsScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                onDismiss = { viewModel.updateShowSettingsScreen(false) }
            )
        }

        // 메뉴 패널
        MenuPanel(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            trackViewModel = trackViewModel,
            routeViewModel = routeViewModel,
            mapLibreMap = mapLibreMap,
            locationManager = locationManager,
            loadPointsFromLocal = { loadPointsFromLocal() },
            getNextAvailablePointNumber = { getNextAvailablePointNumber() },
            onDeletePoint = { deletePoint(it) },
            onEditPoint = { point ->
                viewModel.updateSelectedPoint(point)
                viewModel.updateEditPointName(point.name)
                viewModel.updateEditSelectedColor(point.color)
                viewModel.updateShowEditDialog(true)
                viewModel.updateShowMenu(false)
            },
            activity = activity,
            updateMapRotation = { updateMapRotation() },
            updateTrackDisplay = { updateTrackDisplay() }
        )

        // 오버레이 (GPS 정보, 커서 정보) - 설정창 열려 있을 때는 숨김
        if (!mapUiState.showSettingsScreen) {
            MapOverlays(
                viewModel = viewModel,
                fontSize = settingsViewModel.systemSettings.fontSize
            )
        }

        // 지도 컨트롤 버튼들
        MapControls(
            viewModel = viewModel,
            isEditingRoute = routeUiState.isEditingRoute,
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
                    if (mapForMarker != null && settingsViewModel.systemSettings.destinationVisible) {
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
        
        // AIS 선박 정보 다이얼로그 (aisVessels에서 최신 데이터 사용 - 즐겨찾기 토글 시 즉시 반영)
        selectedAISVessel?.let { selected ->
            val displayVessel = aisVessels.find { it.mmsi == selected.mmsi } ?: selected
            AISVesselDialog(
                vessel = displayVessel,
                onDismiss = { selectedAISVessel = null },
                onToggleWatchlist = { aisViewModel.toggleWatchlist(it) }
            )
        }
        }
        
        // 설정 화면은 SystemSetting 앱에서만 제공됩니다.
    }
}

