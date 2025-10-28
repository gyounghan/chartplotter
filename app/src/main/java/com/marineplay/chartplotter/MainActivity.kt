package com.marineplay.chartplotter

import android.Manifest
import android.R.attr
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.marineplay.chartplotter.ui.theme.ChartPlotterTheme
import com.marineplay.chartplotter.utils.DistanceCalculator
import com.marineplay.chartplotter.helpers.PointHelper
import com.marineplay.chartplotter.helpers.DestinationHelper
import com.marineplay.chartplotter.ui.components.PointDialog
import com.marineplay.chartplotter.ui.components.DestinationDialog
import com.marineplay.chartplotter.ui.components.MenuPanel
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.geometry.LatLngBounds
import android.R.attr.onClick



data class SavedPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val color: Color,
    val iconType: String, // "circle", "triangle", "square"
    val timestamp: Long
)

data class Destination(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {

    private var locationManager: LocationManager? = null
    private var mapLibreMap: MapLibreMap? = null
    private var showDialog by mutableStateOf(false)

    // 헬퍼들
    private lateinit var pointHelper: PointHelper
    private lateinit var destinationHelper: DestinationHelper
    private var isMapInitialized by mutableStateOf(false)
    private var centerCoordinates by mutableStateOf("")
    private var pointName by mutableStateOf("")
    private var selectedColor by mutableStateOf(Color.Red)
    private var currentLatLng: LatLng? = null
    private lateinit var sharedPreferences: SharedPreferences

    // 포인트 관리 다이얼로그 관련
    private var showPointManageDialog by mutableStateOf(false)
    private var selectedPoint: SavedPoint? = null
    private var showEditDialog by mutableStateOf(false)
    private var showPointSelectionDialog by mutableStateOf(false) // 포인트 선택 다이얼로그
    private var editPointName by mutableStateOf("")
    private var editSelectedColor by mutableStateOf(Color.Red)
    private var showMenu by mutableStateOf(false)
    private var currentMenu by mutableStateOf("main") // "main", "point", "ais"
    private var showPointDeleteList by mutableStateOf(false)

    // GPS 좌표 표시 관련
    private var currentGpsLatitude by mutableStateOf(0.0)
    private var currentGpsLongitude by mutableStateOf(0.0)
    private var isGpsAvailable by mutableStateOf(false)
    private var currentShipCog by mutableStateOf(0.0f) // 선박 COG (방향)

    // 동적 커서 관련
    private var showCursor by mutableStateOf(false)
    private var cursorLatLng by mutableStateOf<LatLng?>(null)
    private var cursorScreenPosition by mutableStateOf<android.graphics.PointF?>(null)

    // 포인트 아이콘 관련
    private var selectedIconType by mutableStateOf("circle") // "circle", "triangle", "square"
    private var pointCount by mutableStateOf(0) // 현재 포인트 수

    // 지도 표시 모드 관련
    private var mapDisplayMode by mutableStateOf("노스업") // 노스업, 헤딩업, 코스업
    private var coursePoint by mutableStateOf<SavedPoint?>(null) // 코스업용 포인트
    
    // 줌 롱 클릭 관련
    private var isZoomInLongPressed by mutableStateOf(false)
    private var isZoomOutLongPressed by mutableStateOf(false)
    private var zoomHandler: android.os.Handler? = null
    private var zoomRunnable: Runnable? = null
    private var popupPosition by mutableStateOf<android.graphics.PointF?>(null)

    // 줌 함수들
    private fun startContinuousZoomIn() {
        if (isZoomInLongPressed) return
        
        isZoomInLongPressed = true
        zoomHandler = android.os.Handler(android.os.Looper.getMainLooper())
        
        zoomRunnable = object : Runnable {
            override fun run() {
                if (isZoomInLongPressed) {
                    zoomIn()
                    zoomHandler?.postDelayed(this, 200) // 200ms마다 줌 인
                }
            }
        }
        zoomHandler?.post(zoomRunnable!!)
    }
    
    private fun stopContinuousZoomIn() {
        isZoomInLongPressed = false
        zoomHandler?.removeCallbacks(zoomRunnable!!)
    }
    
    private fun startContinuousZoomOut() {
        if (isZoomOutLongPressed) return
        
        isZoomOutLongPressed = true
        zoomHandler = android.os.Handler(android.os.Looper.getMainLooper())
        
        zoomRunnable = object : Runnable {
            override fun run() {
                if (isZoomOutLongPressed) {
                    zoomOut()
                    zoomHandler?.postDelayed(this, 200) // 200ms마다 줌 아웃
                }
            }
        }
        zoomHandler?.post(zoomRunnable!!)
    }
    
    private fun stopContinuousZoomOut() {
        isZoomOutLongPressed = false
        zoomHandler?.removeCallbacks(zoomRunnable!!)
    }
    
    private fun zoomIn() {
        mapLibreMap?.let { map ->
            val currentZoom = map.cameraPosition.zoom
            val newZoom = (currentZoom + 0.5).coerceAtMost(22.0)
            
            val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                .target(map.cameraPosition.target)
                .zoom(newZoom)
                .bearing(map.cameraPosition.bearing)
                .build()
            map.cameraPosition = newPosition
        }
    }
    
    private fun zoomOut() {
        mapLibreMap?.let { map ->
            val currentZoom = map.cameraPosition.zoom
            val newZoom = (currentZoom - 0.5).coerceAtLeast(6.0)
            
            val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                .target(map.cameraPosition.target)
                .zoom(newZoom)
                .bearing(map.cameraPosition.bearing)
                .build()
            map.cameraPosition = newPosition
        }
    }

    // 사용 가능한 최소 포인트 번호 찾기
    private fun getNextAvailablePointNumber(): Int {
        val existingPoints = loadPointsFromLocal()
        val usedNumbers = existingPoints.mapNotNull { point ->
            // "Point123" 형태에서 숫자 부분만 추출
            val matchResult = Regex("Point(\\d+)").find(point.name)
            matchResult?.groupValues?.get(1)?.toIntOrNull()
        }.toSet()

        // 1부터 시작해서 사용되지 않은 첫 번째 번호 찾기
        var nextNumber = 1
        while (usedNumbers.contains(nextNumber)) {
            nextNumber++
        }
        return nextNumber
    }







    // 두 지점 간의 거리 계산 (미터) - 유틸리티 클래스 사용
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return DistanceCalculator.calculateGeographicDistance(lat1, lon1, lat2, lon2)
    }

    // 화면 거리 계산 (픽셀) - 유틸리티 클래스 사용
    private fun calculateScreenDistance(
        clickLatLng: LatLng,
        targetLatLng: LatLng,
        map: MapLibreMap
    ): Double {
        return DistanceCalculator.calculateScreenDistance(clickLatLng, targetLatLng, map)
    }

    // 지도 회전 제어 함수
    private fun updateMapRotation() {
        mapLibreMap?.let { map ->
            when (mapDisplayMode) {
                "노스업" -> {
                    // 북쪽이 위쪽 (0도)
                    val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(map.cameraPosition.target)
                        .zoom(map.cameraPosition.zoom)
                        .bearing(0.0)
                        .build()
                    map.cameraPosition = newPosition

                    // 코스업 선 제거
                    PMTilesLoader.removeCourseLine(map)
                }
                "헤딩업" -> {
                    // 보트의 진행방향이 위쪽 (현재 bearing의 반대)
                    val heading = locationManager?.getCurrentBearing() ?: 0f
//                    Log.d("[MainActivity]", "헤딩업 지도 회전: 보트 방향 ${heading}도 -> 지도 bearing ${-heading}도")
                    val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(map.cameraPosition.target)
                        .zoom(map.cameraPosition.zoom)
                        .bearing(heading.toDouble()) // bearing의 반대 방향으로 회전
                        .build()
                    map.cameraPosition = newPosition

                    // 코스업 선 제거
                    PMTilesLoader.removeCourseLine(map)
                }
                "코스업" -> {
                    // 포인트 방향이 위쪽
                    coursePoint?.let { point ->
                        val currentLocation = locationManager?.getCurrentLocationObject()
                        if (currentLocation != null) {
                            val bearing = calculateBearing(
                                currentLocation.latitude, currentLocation.longitude,
                                point.latitude, point.longitude
                            )
                            val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(map.cameraPosition.target)
                                .zoom(map.cameraPosition.zoom)
                                .bearing(bearing.toDouble())
                                .build()
                            map.cameraPosition = newPosition

                            // 코스업 선 그리기
                            val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                            val pointLatLng = LatLng(point.latitude, point.longitude)
                            PMTilesLoader.addCourseLine(map, currentLatLng, pointLatLng)
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

                    // 코스업 선 제거
                    PMTilesLoader.removeCourseLine(map)
                }
            }
        }
    }

    // 두 지점 간의 bearing 계산
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val y = Math.sin(deltaLonRad) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)

        val bearingRad = Math.atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)

        return (((bearingDeg % 360) + 360) % 360).toFloat()
    }

    // 위치 권한 요청 런처
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // 정확한 위치 권한이 허용됨
                locationManager?.startLocationUpdates()
                Log.d("[MainActivity]", "정확한 위치 권한 허용 - 첫 번째 위치에서 자동 이동")
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // 대략적인 위치 권한이 허용됨
                locationManager?.startLocationUpdates()
                Log.d("[MainActivity]", "대략적인 위치 권한 허용 - 첫 번째 위치에서 자동 이동")
            }
            else -> {
                // 위치 권한이 거부됨
                android.util.Log.w("[MainActivity]", "위치 권한이 거부되었습니다.")
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 헬퍼들 초기화
        pointHelper = PointHelper(this)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("chart_plotter_points", Context.MODE_PRIVATE)

        // 저장된 포인트들 로드
        val savedPoints = pointHelper.loadPointsFromLocal()
        pointCount = savedPoints.size
        android.util.Log.d("[MainActivity]", "저장된 포인트 ${savedPoints.size}개 로드 완료")

        // MapLibre 초기화
        MapLibre.getInstance(this)

        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            ChartPlotterTheme {
                // UI 줌 버튼 상태 관리
                var isZoomInPressed by remember { mutableStateOf(false) }
                var isZoomOutPressed by remember { mutableStateOf(false) }

                // 🚀 UI 줌 인 버튼 롱클릭 반복 확대
                LaunchedEffect(isZoomInPressed) {
                    if (isZoomInPressed) {
                        while (isZoomInPressed) {
                            mapLibreMap?.let { map ->
                                val currentZoom = map.cameraPosition.zoom
                                val newZoom = (currentZoom + 0.5).coerceAtMost(20.0)
                                
                                // 커서가 있으면 3단계 처리
                                if (showCursor && cursorLatLng != null) {
                                    // 1단계: 커서를 맵 중앙에 위치 (화면 중앙으로 이동)
                                    val centerLatLng = map.cameraPosition.target
                                    if (centerLatLng != null) {
                                        val centerScreenPoint = map.projection.toScreenLocation(centerLatLng)
                                        cursorScreenPosition = centerScreenPoint
                                        Log.d("[MainActivity]", "줌 인 - 1단계: 커서를 맵 중앙에 위치")
                                    }
                                    
                                    // 2단계: 이동하기 전 커서 위치로 지도 중앙 맞춤
                                    val originalCursorLatLng = cursorLatLng!!
                                    val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                        org.maplibre.android.camera.CameraPosition.Builder()
                                            .target(originalCursorLatLng)
                                            .zoom(newZoom)
                                            .build()
                                    )
                                    map.animateCamera(cameraUpdate, 300)
                                    
                                    Log.d("[MainActivity]", "줌 인 - 2단계: 원래 커서 위치로 지도 중앙 맞춤 + 3단계: 줌 인 처리")
                                } else {
                                    // 커서가 없으면 일반 줌 인
                                    val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                                    map.animateCamera(cameraUpdate, 300)
                                }
                                Log.d("[MainActivity]", "줌 인: $currentZoom -> $newZoom")
                            }
                            delay(200L) // 반복 속도 조절 (200ms = 0.2초)
                        }
                    }
                }

                // 🚀 UI 줌 아웃 버튼 롱클릭 반복 축소
                LaunchedEffect(isZoomOutPressed) {
                    if (isZoomOutPressed) {
                        while (isZoomOutPressed) {
                            mapLibreMap?.let { map ->
                                val currentZoom = map.cameraPosition.zoom
                                val newZoom = (currentZoom - 0.5).coerceAtLeast(0.0)
                                
                                // 커서가 있으면 3단계 처리
                                if (showCursor && cursorLatLng != null) {
                                    // 1단계: 커서를 맵 중앙에 위치 (화면 중앙으로 이동)
                                    val centerLatLng = map.cameraPosition.target
                                    if (centerLatLng != null) {
                                        val centerScreenPoint = map.projection.toScreenLocation(centerLatLng)
                                        cursorScreenPosition = centerScreenPoint
                                        Log.d("[MainActivity]", "줌 아웃 - 1단계: 커서를 맵 중앙에 위치")
                                    }
                                    
                                    // 2단계: 이동하기 전 커서 위치로 지도 중앙 맞춤
                                    val originalCursorLatLng = cursorLatLng!!
                                    val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                        org.maplibre.android.camera.CameraPosition.Builder()
                                            .target(originalCursorLatLng)
                                            .zoom(newZoom)
                                            .build()
                                    )
                                    map.animateCamera(cameraUpdate, 300)
                                    
                                    Log.d("[MainActivity]", "줌 아웃 - 2단계: 원래 커서 위치로 지도 중앙 맞춤 + 3단계: 줌 아웃 처리")
                                } else {
                                    // 커서가 없으면 일반 줌 아웃
                                    val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                                    map.animateCamera(cameraUpdate, 300)
                                }
                                Log.d("[MainActivity]", "줌 아웃: $currentZoom -> $newZoom")
                            }
                            delay(200L) // 반복 속도 조절 (200ms = 0.2초)
                        }
                    }
                }

                // 지도 표시 모드 변경 시 회전 업데이트
                LaunchedEffect(mapDisplayMode) {
                    updateMapRotation()
                }

                // 코스업 모드에서 포인트 변경 시 회전 업데이트
                LaunchedEffect(coursePoint) {
                    if (mapDisplayMode == "코스업") {
                        updateMapRotation()
                    }
                }
                // 포인트 등록 다이얼로그 표시
                PointDialog(
                    showDialog = showDialog,
                    pointName = pointName,
                    selectedColor = selectedColor,
                    onNameChange = { pointName = it },
                    onColorChange = { selectedColor = it },
                    onConfirm = { registerPoint() },
                    onDismiss = { showDialog = false }
                )

                // 포인트 관리 다이얼로그 표시
                if (showPointManageDialog && selectedPoint != null) {
                    PointManageDialog(
                        point = selectedPoint!!,
                        onDelete = { deletePoint(selectedPoint!!) },
                        onEdit = {
                            showPointManageDialog = false
                            showEditDialog = true
                        },
                        onDismiss = { showPointManageDialog = false }
                    )
                }

                // 포인트 편집 다이얼로그 표시
                if (showEditDialog && selectedPoint != null) {
                    PointEditDialog(
                        point = selectedPoint!!,
                        pointName = editPointName,
                        onPointNameChange = { editPointName = it },
                        selectedColor = editSelectedColor,
                        onColorChange = { editSelectedColor = it },
                        onSave = { updatePoint(selectedPoint!!, editPointName, editSelectedColor) },
                        onDismiss = { showEditDialog = false }
                    )
                }

                // 포인트 삭제 목록 다이얼로그 표시
                if (showPointDeleteList) {
                    PointDeleteListDialog(
                        points = loadPointsFromLocal(),
                        onDeletePoint = { point -> deletePoint(point) },
                        onDismiss = { showPointDeleteList = false }
                    )
                }

                // 포인트 선택 다이얼로그 (코스업용)
                if (showPointSelectionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPointSelectionDialog = false },
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
                                                    coursePoint = point
                                                    showPointSelectionDialog = false
                                                    updateMapRotation()
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (coursePoint == point) Color.Yellow else Color.White
                                            )
                                        ) {
                                            Text(
                                                text = "${point.name} (${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)})",
                                                modifier = Modifier.padding(8.dp),
                                                color = if (coursePoint == point) Color.Black else Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showPointSelectionDialog = false }
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
                        if (!showMenu) {
                            // 현재 위치 버튼 (우측 하단)
                            FloatingActionButton(
                            onClick = {
                                locationManager?.startAutoTracking()
                                // 현재 위치로 이동할 때 커서 숨김
                                showCursor = false
                                cursorLatLng = null
                                cursorScreenPosition = null
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

                            // 목적지 마커 추가 (지도 스타일 로드 완료 후)
                            map.getStyle { style ->
                                // 약간의 지연을 두고 마커 추가 (스타일 완전 로드 대기)
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    // 목적지 마커는 더 이상 사용하지 않음
                                }, 500) // 0.5초 지연
                            }

                            /* ✅ 카메라 타겟 범위 제한: 한·중·일 대략 커버 */
                            val regionBounds = LatLngBounds.Builder()
                                // NE, SW 2점만으로 범위 구성
                                .include(LatLng(42.0, 150.0))  // 북동 (대략 일본 북부~쿠릴 열도 부근까지)
                                .include(LatLng(24.0, 120.0))   // 남서 (중국 남부~베트남 북부 위도까지)
                                .build()

                            map.setLatLngBoundsForCameraTarget(regionBounds)
                            if (!isMapInitialized) {
                                mapLibreMap = map
                                locationManager = LocationManager(
                                    this@MainActivity,
                                    map,
                                    onGpsLocationUpdate = { lat, lng, available ->
                                        currentGpsLatitude = lat
                                        currentGpsLongitude = lng
                                        isGpsAvailable = available

                                        // 코스업 모드에서 위치 변경 시 선 업데이트
                                        if (mapDisplayMode == "코스업" && coursePoint != null) {
                                            val currentLocation = locationManager?.getCurrentLocationObject()
                                            if (currentLocation != null) {
                                                val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                                                val pointLatLng = LatLng(coursePoint!!.latitude, coursePoint!!.longitude)
                                                PMTilesLoader.addCourseLine(map, currentLatLng, pointLatLng)
                                            }
                                        }
                                    },
                                    onBearingUpdate = { bearing ->
                                        // COG 정보 업데이트
                                        currentShipCog = bearing
                                        // 헤딩업 모드일 때만 지도 회전 업데이트
                                        if (mapDisplayMode == "헤딩업") {
//                                            Log.d("[MainActivity]", "헤딩업 모드: 보트 방향 ${bearing}도로 지도 회전")
                                            updateMapRotation()
                                        } else {
//                                            Log.v("[MainActivity]", "보트 방향 ${bearing}도 감지됨 (현재 모드: ${mapDisplayMode})")
                                        }
                                    }
                                )

                                // 센서 초기화
                                locationManager?.initializeSensors()

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

                                // 지도 클릭 이벤트 처리 (포인트 마커 클릭 감지 + 터치 위치에 커서 표시)
                                map.addOnMapClickListener { latLng ->
                                    // 클릭된 위치에서 포인트 레이어의 피처들을 쿼리
                                    val screenPoint = map.projection.toScreenLocation(latLng)
                                    val features = map.queryRenderedFeatures(
                                        android.graphics.PointF(screenPoint.x, screenPoint.y),
                                        "points-symbol"
                                    )

                                    // 항상 터치한 위치에 커서 표시
                                    cursorLatLng = latLng
                                    cursorScreenPosition = screenPoint
                                    showCursor = true

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
                                            selectedPoint = point
                                            editPointName = point.name
                                            editSelectedColor = point.color
                                            showPointManageDialog = true
                                        }

                                        Log.d("[MainActivity]", "포인트 클릭 + 커서 표시: ${latLng.latitude}, ${latLng.longitude}")

                                        true // 기본 지도 클릭 이벤트 방지
                                    } else {
                                        Log.d("[MainActivity]", "터치 위치에 커서 표시: ${latLng.latitude}, ${latLng.longitude}")

                                        false // 기본 지도 클릭 이벤트 허용
                                    }
                                }


                                // 위치 권한 확인 및 요청
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    locationManager?.startLocationUpdates()
                                    // 첫 번째 위치 정보를 받으면 자동으로 그 위치로 이동 (onLocationChanged에서 처리)
                                    Log.d("[MainActivity]", "위치 추적 시작 - 첫 번째 위치에서 자동 이동")
                                } else {
                                    locationPermissionRequest.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }

                                isMapInitialized = true
                            }
                        },
                        isDialogShown = showDialog, // ⬅ 전달
                        showCursor = showCursor,
                        cursorLatLng = cursorLatLng,
                        cursorScreenPosition = cursorScreenPosition,
                        onTouchEnd = { latLng, screenPoint ->
                            Log.d("[MainActivity]", "터치 이벤트 발생: ${latLng.latitude}, ${latLng.longitude}")
                            // 터치 종료 시 커서 표시
                            cursorLatLng = latLng
                            cursorScreenPosition = screenPoint
                            showCursor = true
                        },
                        onTouchStart = {
                            // 터치 시작 시 커서 숨김
                            showCursor = false
                        }
                    )

                    // 우측 상단 메뉴 버튼
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 24.dp, end = 16.dp, start = 16.dp, bottom = 16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 포인트 생성 버튼 (커서가 표시될 때만 보임)
                            if (showCursor) {
                                FloatingActionButton(
                                    onClick = { createQuickPoint() },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = Color(0xC6E0E0E0),
                                    contentColor = Color.Black,
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp,
                                        focusedElevation = 0.dp,
                                        hoveredElevation = 0.dp
                                    ),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(
                                            width = 1.dp,
                                            color = Color.White ,
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = "포인트 추가",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            // 메뉴 버튼
                            FloatingActionButton(
                                onClick = { showMenu = !showMenu },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = Color(0xC6E2E2E2),
                                contentColor = Color.Black,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 0.dp,                   // ✅ 내부가 밝아 보이는 효과 최소화
                                    pressedElevation = 0.dp,
                                    focusedElevation = 0.dp,
                                    hoveredElevation = 0.dp
                                ),
                                modifier = Modifier
                                    .size(48.dp)
//                                    .clip(CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = Color.White,
                                        shape =  RoundedCornerShape(16.dp)
                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "메뉴"
                                )
                            }
                        }
                    }
                    
                    // 아이콘 선택 UI (커서가 표시될 때만 보임, 지도 좌측 상단)
                    if (showCursor) {
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
                                        selectedIconType = when (selectedIconType) {
                                            "circle" -> "square"
                                            "triangle" -> "circle"
                                            "square" -> "triangle"
                                            else -> "circle"
                                        }
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
                                    when (selectedIconType) {
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
                                        selectedIconType = when (selectedIconType) {
                                            "circle" -> "triangle"
                                            "triangle" -> "square"
                                            "square" -> "circle"
                                            else -> "circle"
                                        }
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
                    
                    // 메뉴바 (우측에 고정, 지도 조작 방해하지 않음)
                    if (showMenu) {
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
                                            text = when (currentMenu) {
                                                "main" -> "메뉴"
                                                "point" -> "포인트"
                                                "ais" -> "AIS"
                                                else -> "메뉴"
                                            },
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        IconButton(
                                            onClick = { 
                                                if (currentMenu == "main") {
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                } else {
                                                    currentMenu = "main"
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (currentMenu == "main") Icons.Default.Close else Icons.Default.ArrowBack,
                                                contentDescription = if (currentMenu == "main") "메뉴 닫기" else "뒤로가기",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // 메인 메뉴
                                    if (currentMenu == "main") {
                                        Text(
                                            "포인트", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    currentMenu = "point"
                                                },
                                            color = Color.White
                                        )
                                        
                                        Text(
                                            "AIS", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    currentMenu = "ais"
                                                },
                                            color = Color.White
                                        )
                                        
                                        Text(
                                            "화면표시 방법설정", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    currentMenu = "display"
                                                },
                                            color = Color.White
                                        )
                                        
                                        
                                        Text(
                                            "설정", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                    // TODO: 설정 화면 구현
                                                },
                                            color = Color.White
                                        )
                                        
                                        Text(
                                            "정보", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                    // TODO: 정보 화면 구현
                                                },
                                            color = Color.White
                                        )
                                    }
                                    
                                    // 포인트 메뉴
                                    if (currentMenu == "point") {
                                        Text(
                                            "포인트 생성", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    // 커서 위치가 있으면 커서 위치 사용, 없으면 화면 중앙 좌표 사용
                                                    val targetLatLng = if (showCursor && cursorLatLng != null) {
                                                        cursorLatLng
                                                    } else {
                                                        mapLibreMap?.cameraPosition?.target
                                                    }
                                                    
                                                    targetLatLng?.let { latLng ->
                                                        currentLatLng = latLng
                                                        centerCoordinates = "위도: ${String.format("%.6f", latLng.latitude)}\n경도: ${String.format("%.6f", latLng.longitude)}"
                                                        pointName = "Point${getNextAvailablePointNumber()}" // 자동 포인트명 생성
                                                        selectedColor = Color.Red // 색상 초기화
                                                    } ?: run {
                                                        centerCoordinates = "좌표를 가져올 수 없습니다."
                                                        currentLatLng = null
                                                    }
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                    showDialog = true
                                                },
                                            color = Color.White
                                        )
                                        
                                        Text(
                                            "포인트 삭제", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                    showPointDeleteList = true
                                                },
                                            color = Color.White
                                        )
                                        
                                        Text(
                                            "포인트 변경", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                    // TODO: 포인트 변경 화면 구현
                                                },
                                            color = Color.White
                                        )
                                    }
                                    
                                    // AIS 메뉴
                                    if (currentMenu == "ais") {
                                        Text(
                                            "AIS ON/OFF", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
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
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                    // TODO: AIS 설정 화면 구현
                                                },
                                            color = Color.White
                                        )
                                    }
                                    
                                    // 화면표시 방법설정 메뉴
                                    if (currentMenu == "display") {
                                        Text(
                                            "노스업", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    Log.d("[MainActivity]", "지도 표시 모드 변경: ${mapDisplayMode} -> 노스업")
                                                    mapDisplayMode = "노스업"
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                },
                                            color = if (mapDisplayMode == "노스업") Color.Yellow else Color.White
                                        )
                                        
                                        Text(
                                            "헤딩업", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    Log.d("[MainActivity]", "지도 표시 모드 변경: ${mapDisplayMode} -> 헤딩업")
                                                    mapDisplayMode = "헤딩업"
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                },
                                            color = if (mapDisplayMode == "헤딩업") Color.Yellow else Color.White
                                        )
                                        
                                        Text(
                                            "코스업", 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable { 
                                                    Log.d("[MainActivity]", "지도 표시 모드 변경: ${mapDisplayMode} -> 코스업")
                                                    mapDisplayMode = "코스업"
                                                    // 포인트 목록에서 선택하도록 변경
                                                    val savedPoints = loadPointsFromLocal()
                                                    if (savedPoints.isNotEmpty()) {
                                                        showPointSelectionDialog = true
                                                    } else {
                                                        // 포인트가 없으면 포인트 생성 안내
                                                        android.util.Log.d("[MainActivity]", "코스업을 위해 포인트를 먼저 생성하세요")
                                                    }
                                                    showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                },
                                            color = if (mapDisplayMode == "코스업") Color.Yellow else Color.White
                                        )
                                        
                                        if (mapDisplayMode == "코스업") {
                                            Text(
                                                "포인트 변경", 
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                    .clickable { 
                                                        showMenu = false
                                                    currentMenu = "main" // 메뉴 닫을 때 초기화
                                                        val savedPoints = loadPointsFromLocal()
                                                        if (savedPoints.isNotEmpty()) {
                                                            showPointSelectionDialog = true
                                                        }
                                                    },
                                                color = Color.White
                                            )
                                        }
                                    }
                                    
                                }
                            }
                        }
                    }
                    
                    // 줌 인/아웃 버튼 (가운데 하단)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 줌 아웃 버튼
                            FloatingActionButton(
                                onClick = {

                                },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = Color(0xC6E2E2E2),
                                contentColor = Color.Black,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 0.dp,                   // ✅ 내부가 밝아 보이는 효과 최소화
                                    pressedElevation = 0.dp,
                                    focusedElevation = 0.dp,
                                    hoveredElevation = 0.dp
                                ),
                                modifier = Modifier
                                    .size(56.dp)
//                                    .clip(CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = Color.White,
                                        shape =  RoundedCornerShape(16.dp)
                                    )

                                    ,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
//                                        .combinedClickable(
//                                            onClick = {
//                                                      },
//                                            onLongClick = {
//                                                isZoomInPressed = true
//                                            }
//                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    // 손가락이 눌리는 순간
                                                    isZoomOutPressed = true
                                                    tryAwaitRelease() // 손을 뗄 때까지 대기
                                                    // 손을 떼면 여기로 돌아옴
                                                    isZoomOutPressed = false
                                                },
                                                onTap = {
                                                    // 짧게 눌렀을 때 동작 (선택사항)
                                                    mapLibreMap?.let { map ->
                                                        val currentZoom = map.cameraPosition.zoom
                                                        val newZoom = (currentZoom - 0.5).coerceAtLeast(0.0)

                                                        // 커서가 있으면 3단계 처리


                                                        if (showCursor && cursorLatLng != null) {
                                                            // 1단계: 커서를 맵 중앙에 위치 (화면 중앙으로 이동)
                                                            val centerLatLng = map.cameraPosition.target
                                                            if (centerLatLng != null) {
                                                                val centerScreenPoint = map.projection.toScreenLocation(centerLatLng)
                                                                cursorScreenPosition = centerScreenPoint
                                                                Log.d("[MainActivity]", "줌 아웃 - 1단계: 커서를 맵 중앙에 위치")
                                                            }

                                                            // 2단계: 이동하기 전 커서 위치로 지도 중앙 맞춤
                                                            val originalCursorLatLng = cursorLatLng!!
                                                            val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                                org.maplibre.android.camera.CameraPosition.Builder()
                                                                    .target(originalCursorLatLng)
                                                                    .zoom(newZoom)
                                                                    .build()
                                                            )
                                                            map.animateCamera(cameraUpdate, 300)

                                                            Log.d("[MainActivity]", "줌 아웃 - 2단계: 원래 커서 위치로 지도 중앙 맞춤 + 3단계: 줌 아웃 처리")
                                                        } else {
                                                            // 커서가 없으면 일반 줌 아웃
                                                            val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                                                            map.animateCamera(cameraUpdate, 300)
                                                        }
                                                        Log.d("[MainActivity]", "줌 아웃: $currentZoom -> $newZoom")
                                                    }
                                                    Log.d("[MainActivity]", "짧게 클릭")
                                                }
                                            )
                                        },

                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "-",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // 줌 인 버튼
                            FloatingActionButton(
                                onClick = {

                                },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = Color(0xC6E2E2E2),
                                contentColor = Color.Black,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 0.dp,                   // ✅ 내부가 밝아 보이는 효과 최소화
                                    pressedElevation = 0.dp,
                                    focusedElevation = 0.dp,
                                    hoveredElevation = 0.dp
                                ),
                                modifier = Modifier
                                    .size(56.dp)
//                                    .clip(CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = Color.White,
                                        shape =  RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
//                                        .combinedClickable(
//                                            onClick = {
//                                                      },
//                                            onLongClick = {
//                                                isZoomInPressed = true
//                                            }
//                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    // 손가락이 눌리는 순간
                                                    isZoomInPressed = true
                                                    tryAwaitRelease() // 손을 뗄 때까지 대기
                                                    // 손을 떼면 여기로 돌아옴
                                                    isZoomInPressed = false
                                                },
                                                onTap = {
                                                    // 짧게 눌렀을 때 동작 (선택사항)
                                                    mapLibreMap?.let { map ->
                                                        val currentZoom = map.cameraPosition.zoom
                                                        val newZoom = (currentZoom + 0.5).coerceAtLeast(0.0)

                                                        // 커서가 있으면 3단계 처리


                                                        if (showCursor && cursorLatLng != null) {
                                                            // 1단계: 커서를 맵 중앙에 위치 (화면 중앙으로 이동)
                                                            val centerLatLng = map.cameraPosition.target
                                                            if (centerLatLng != null) {
                                                                val centerScreenPoint = map.projection.toScreenLocation(centerLatLng)
                                                                cursorScreenPosition = centerScreenPoint
                                                                Log.d("[MainActivity]", "줌 아웃 - 1단계: 커서를 맵 중앙에 위치")
                                                            }

                                                            // 2단계: 이동하기 전 커서 위치로 지도 중앙 맞춤
                                                            val originalCursorLatLng = cursorLatLng!!
                                                            val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                                org.maplibre.android.camera.CameraPosition.Builder()
                                                                    .target(originalCursorLatLng)
                                                                    .zoom(newZoom)
                                                                    .build()
                                                            )
                                                            map.animateCamera(cameraUpdate, 300)

                                                            Log.d("[MainActivity]", "줌 아웃 - 2단계: 원래 커서 위치로 지도 중앙 맞춤 + 3단계: 줌 아웃 처리")
                                                        } else {
                                                            // 커서가 없으면 일반 줌 아웃
                                                            val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                                                            map.animateCamera(cameraUpdate, 300)
                                                        }
                                                        Log.d("[MainActivity]", "줌 아웃: $currentZoom -> $newZoom")
                                                    }
                                                    Log.d("[MainActivity]", "짧게 클릭")
                                                }
                                            )
                                        },

                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // GPS 좌표와 지도 모드 통합 표시 (좌측하단)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.DarkGray.copy(alpha = 0.7f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                // GPS 좌표
                                Text(
                                    text = "GPS 좌표",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isGpsAvailable) {
                                    Text(
                                        text = "위도: ${String.format("%.6f", currentGpsLatitude)}",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "경도: ${String.format("%.6f", currentGpsLongitude)}",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "COG: ${String.format("%.1f", currentShipCog)}°",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Text(
                                        text = "GPS 신호 없음",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 지도 표시 모드
                                Text(
                                    text = "지도 모드",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mapDisplayMode,
                                    color = Color.Yellow,
                                    fontSize = 11.sp
                                )
                                if (mapDisplayMode == "코스업" && coursePoint != null) {
                                    Text(
                                        text = "포인트 설정됨: ${coursePoint!!.name}",
                                        color = Color.Green,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_3 -> {
                // 현재 화면 중앙 좌표 가져오기
                mapLibreMap?.cameraPosition?.target?.let { latLng ->
                    currentLatLng = latLng
                    centerCoordinates = "위도: ${String.format("%.6f", latLng.latitude)}\n경도: ${String.format("%.6f", latLng.longitude)}"
                    pointName = "" // 포인트명 초기화
                    selectedColor = Color.Red // 색상 초기화
                } ?: run {
                    centerCoordinates = "좌표를 가져올 수 없습니다."
                    currentLatLng = null
                }
                showDialog = true
                return true
            }
            KeyEvent.KEYCODE_BUTTON_4 -> {
                // 줌 아웃
                mapLibreMap?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom - 0.5).coerceAtMost(20.0)

                    // 커서가 있으면 3단계 처리
                    if (showCursor && cursorLatLng != null) {
                        // 1단계: 커서를 맵 중앙에 위치 (화면 중앙으로 이동)
                        val centerLatLng = map.cameraPosition.target
                        if (centerLatLng != null) {
                            val centerScreenPoint = map.projection.toScreenLocation(centerLatLng)
                            cursorScreenPosition = centerScreenPoint
                            Log.d("[MainActivity]", "줌 인 - 1단계: 커서를 맵 중앙에 위치")
                        }

                        // 2단계: 이동하기 전 커서 위치로 지도 중앙 맞춤
                        val originalCursorLatLng = cursorLatLng!!
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                            org.maplibre.android.camera.CameraPosition.Builder()
                                .target(originalCursorLatLng)
                                .zoom(newZoom)
                                .build()
                        )
                        map.animateCamera(cameraUpdate, 300)

                        Log.d("[MainActivity]", "줌 인 - 2단계: 원래 커서 위치로 지도 중앙 맞춤 + 3단계: 줌 인 처리")
                    } else {
                        // 커서가 없으면 일반 줌 인
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                        map.animateCamera(cameraUpdate, 300)
                    }
                    Log.d("[MainActivity]", "줌 인: $currentZoom -> $newZoom")
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_5 -> {
                // 줌 인
                mapLibreMap?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom + 0.5).coerceAtMost(20.0)

                    // 커서가 있으면 3단계 처리
                    if (showCursor && cursorLatLng != null) {
                        // 1단계: 커서를 맵 중앙에 위치 (화면 중앙으로 이동)
                        val centerLatLng = map.cameraPosition.target
                        if (centerLatLng != null) {
                            val centerScreenPoint = map.projection.toScreenLocation(centerLatLng)
                            cursorScreenPosition = centerScreenPoint
                            Log.d("[MainActivity]", "줌 인 - 1단계: 커서를 맵 중앙에 위치")
                        }

                        // 2단계: 이동하기 전 커서 위치로 지도 중앙 맞춤
                        val originalCursorLatLng = cursorLatLng!!
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                            org.maplibre.android.camera.CameraPosition.Builder()
                                .target(originalCursorLatLng)
                                .zoom(newZoom)
                                .build()
                        )
                        map.animateCamera(cameraUpdate, 300)

                        Log.d("[MainActivity]", "줌 인 - 2단계: 원래 커서 위치로 지도 중앙 맞춤 + 3단계: 줌 인 처리")
                    } else {
                        // 커서가 없으면 일반 줌 인
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                        map.animateCamera(cameraUpdate, 300)
                    }
                    Log.d("[MainActivity]", "줌 인: $currentZoom -> $newZoom")
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_6 -> {
                // 화면 위로 이동
                mapLibreMap?.let { map ->
                    val currentPosition = map.cameraPosition
                    currentPosition.target?.let { target ->
                        val currentLat = target.latitude
                        val currentLng = target.longitude
                        val zoom = currentPosition.zoom
                        
                        // 위도 증가 (북쪽으로 이동)
                        val newLat = currentLat + (0.01 / Math.pow(2.0, zoom - 8.0))
                        val newPosition = org.maplibre.android.geometry.LatLng(newLat, currentLng)
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newLatLng(newPosition)
                        map.animateCamera(cameraUpdate, 300)
                        android.util.Log.d("[MainActivity]", "화면 위로 이동: $currentLat -> $newLat")
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_7 -> {
                // 화면 아래로 이동
                mapLibreMap?.let { map ->
                    val currentPosition = map.cameraPosition
                    currentPosition.target?.let { target ->
                        val currentLat = target.latitude
                        val currentLng = target.longitude
                        val zoom = currentPosition.zoom
                        
                        // 위도 감소 (남쪽으로 이동)
                        val newLat = currentLat - (0.01 / Math.pow(2.0, zoom - 8.0))
                        val newPosition = org.maplibre.android.geometry.LatLng(newLat, currentLng)
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newLatLng(newPosition)
                        map.animateCamera(cameraUpdate, 300)
                        android.util.Log.d("[MainActivity]", "화면 아래로 이동: $currentLat -> $newLat")
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_8 -> {
                // 화면 왼쪽으로 이동
                mapLibreMap?.let { map ->
                    val currentPosition = map.cameraPosition
                    currentPosition.target?.let { target ->
                        val currentLat = target.latitude
                        val currentLng = target.longitude
                        val zoom = currentPosition.zoom
                        
                        // 경도 감소 (서쪽으로 이동)
                        val newLng = currentLng - (0.01 / Math.pow(2.0, zoom - 8.0))
                        val newPosition = org.maplibre.android.geometry.LatLng(currentLat, newLng)
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newLatLng(newPosition)
                        map.animateCamera(cameraUpdate, 300)
                        android.util.Log.d("[MainActivity]", "화면 왼쪽으로 이동: $currentLng -> $newLng")
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_9 -> {
                // 화면 오른쪽으로 이동
                mapLibreMap?.let { map ->
                    val currentPosition = map.cameraPosition
                    currentPosition.target?.let { target ->
                        val currentLat = target.latitude
                        val currentLng = target.longitude
                        val zoom = currentPosition.zoom
                        
                        // 경도 증가 (동쪽으로 이동)
                        val newLng = currentLng + (0.01 / Math.pow(2.0, zoom - 8.0))
                        val newPosition = org.maplibre.android.geometry.LatLng(currentLat, newLng)
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newLatLng(newPosition)
                        map.animateCamera(cameraUpdate, 300)
                        android.util.Log.d("[MainActivity]", "화면 오른쪽으로 이동: $currentLng -> $newLng")
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_1 -> {
                // 줌 아웃 버튼 (롱 클릭으로 연속 줌)
                if (event?.isLongPress == true) {
                    // 롱 클릭: 연속 줌 아웃 시작
                    startContinuousZoomOut()
                    android.util.Log.d("[MainActivity]", "줌 아웃 롱 클릭 시작")
                } else {
                    // 일반 클릭: 한 번 줌 아웃
                    zoomOut()
                    android.util.Log.d("[MainActivity]", "줌 아웃 일반 클릭")
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_2 -> {
                // 줌 인 버튼 (롱 클릭으로 연속 줌)
                if (event?.isLongPress == true) {
                    // 롱 클릭: 연속 줌 인 시작
                    startContinuousZoomIn()
                    android.util.Log.d("[MainActivity]", "줌 인 롱 클릭 시작")
                } else {
                    // 일반 클릭: 한 번 줌 인
                    zoomIn()
                    android.util.Log.d("[MainActivity]", "줌 인 일반 클릭")
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_5 -> {
                // 커서 클릭 이벤트 (목적지/포인트 클릭 처리)
                if (showCursor && cursorLatLng != null && cursorScreenPosition != null) {
                    Log.d("[MainActivity]", "커서 클릭 이벤트 발생: ${cursorLatLng!!.latitude}, ${cursorLatLng!!.longitude}")
                    
                    // 포인트 클릭 확인 (화면 거리 기준)
                    mapLibreMap?.let { map ->
                        val savedPoints = loadPointsFromLocal()
                        val closestPoint = savedPoints.minByOrNull { point ->
                            val pointLatLng = LatLng(point.latitude, point.longitude)
                            val screenDistance = calculateScreenDistance(cursorLatLng!!, pointLatLng, map)
                            screenDistance
                        }
                        
                        if (closestPoint != null) {
                            val pointLatLng = LatLng(closestPoint.latitude, closestPoint.longitude)
                            val screenDistance = calculateScreenDistance(cursorLatLng!!, pointLatLng, map)
                            
                            if (screenDistance <= 40) { // 100픽셀 이내
                                Log.d("[MainActivity]", "포인트 클릭: ${closestPoint.name} (화면 거리: ${screenDistance}픽셀)")
                                // 포인트 편집/삭제 다이얼로그 표시
                                selectedPoint = closestPoint
                                editPointName = closestPoint.name
                                editSelectedColor = closestPoint.color
                                showEditDialog = true
                            } else {
                                Log.d("[MainActivity]", "포인트 화면 거리가 너무 멀어서 클릭으로 인식하지 않음 (${screenDistance}픽셀)")
                            }
                        }
                    }
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun registerPoint() {
        currentLatLng?.let { latLng ->
            // 자동 포인트명 생성 (사용 가능한 최소 번호)
            val autoPointName = "Point${getNextAvailablePointNumber()}"
            val finalPointName = if (pointName.isBlank()) autoPointName else pointName
            
            val point = SavedPoint(
                name = finalPointName,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                color = selectedColor,
                iconType = selectedIconType,
                timestamp = System.currentTimeMillis()
            )
            
            savePointToLocal(point)
            pointCount++
            
            // 새로 등록된 포인트를 지도에 즉시 표시
            val allPoints = loadPointsFromLocal()
            locationManager?.updatePointsOnMap(allPoints)
            
            android.util.Log.d("[MainActivity]", "포인트 등록 완료: $finalPointName, 좌표: $latLng, 색상: $selectedColor, 아이콘: $selectedIconType")
            showDialog = false
            // 포인트 등록 후 커서 숨김
            showCursor = false
            cursorLatLng = null
            cursorScreenPosition = null
        }
    }
    
    // 빠른 포인트 생성 (다이얼로그 없이 바로 생성)
    private fun createQuickPoint() {
        cursorLatLng?.let { latLng ->
            val autoPointName = "Point${getNextAvailablePointNumber()}"
            
            val point = SavedPoint(
                name = autoPointName,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                color = selectedColor,
                iconType = selectedIconType,
                timestamp = System.currentTimeMillis()
            )
            
            savePointToLocal(point)
            pointCount++
            
            // 새로 등록된 포인트를 지도에 즉시 표시
            val allPoints = loadPointsFromLocal()
            locationManager?.updatePointsOnMap(allPoints)
            
            android.util.Log.d("[MainActivity]", "빠른 포인트 생성 완료: $autoPointName, 좌표: $latLng, 색상: $selectedColor, 아이콘: $selectedIconType")
            
            // 포인트 생성 후 커서 숨김
            showCursor = false
            cursorLatLng = null
            cursorScreenPosition = null
        }
    }
    
    private fun savePointToLocal(point: SavedPoint) {
        try {
            val existingPoints = loadPointsFromLocal().toMutableList()
            existingPoints.add(point)
            
            val jsonArray = JSONArray()
            existingPoints.forEach { p ->
                val jsonObject = JSONObject().apply {
                    put("name", p.name)
                    put("latitude", p.latitude)
                    put("longitude", p.longitude)
                    put("color", AndroidColor.argb(
                        (p.color.alpha * 255).toInt(),
                        (p.color.red * 255).toInt(),
                        (p.color.green * 255).toInt(),
                        (p.color.blue * 255).toInt()
                    ))
                    put("iconType", p.iconType)
                    put("timestamp", p.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPreferences.edit()
                .putString("saved_points", jsonArray.toString())
                .apply()
                
            android.util.Log.d("[MainActivity]", "포인트 저장 완료: ${existingPoints.size}개")
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 저장 실패: ${e.message}")
        }
    }
    
    private fun loadPointsFromLocal(): List<SavedPoint> {
        return try {
            val jsonString = sharedPreferences.getString("saved_points", null)
            if (jsonString != null) {
                val jsonArray = JSONArray(jsonString)
                val points = mutableListOf<SavedPoint>()
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val point = SavedPoint(
                        name = jsonObject.getString("name"),
                        latitude = jsonObject.getDouble("latitude"),
                        longitude = jsonObject.getDouble("longitude"),
                        color = Color(jsonObject.getInt("color")),
                        iconType = jsonObject.optString("iconType", "circle"), // 기존 포인트는 기본값 "circle"
                        timestamp = jsonObject.getLong("timestamp")
                    )
                    points.add(point)
                }
                points
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 로드 실패: ${e.message}")
            emptyList()
        }
    }
    
    /** 포인트 삭제 */
    private fun deletePoint(point: SavedPoint) {
        try {
            val existingPoints = loadPointsFromLocal().toMutableList()
            existingPoints.removeAll { it.timestamp == point.timestamp }
            
            val jsonArray = JSONArray()
            existingPoints.forEach { p ->
                val jsonObject = JSONObject().apply {
                    put("name", p.name)
                    put("latitude", p.latitude)
                    put("longitude", p.longitude)
                    put("color", AndroidColor.argb(
                        (p.color.alpha * 255).toInt(),
                        (p.color.red * 255).toInt(),
                        (p.color.green * 255).toInt(),
                        (p.color.blue * 255).toInt()
                    ))
                    put("timestamp", p.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPreferences.edit()
                .putString("saved_points", jsonArray.toString())
                .apply()
            
            // 지도에서 포인트 제거
            locationManager?.updatePointsOnMap(existingPoints)
            
            // pointCount 업데이트
            pointCount = existingPoints.size
            
            android.util.Log.d("[MainActivity]", "포인트 삭제 완료: ${point.name}")
            showPointManageDialog = false
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 삭제 실패: ${e.message}")
        }
    }
    
    /** 포인트 업데이트 */
    private fun updatePoint(originalPoint: SavedPoint, newName: String, newColor: Color) {
        try {
            val existingPoints = loadPointsFromLocal().toMutableList()
            val pointIndex = existingPoints.indexOfFirst { it.timestamp == originalPoint.timestamp }
            
            if (pointIndex != -1) {
                val updatedPoint = originalPoint.copy(
                    name = newName,
                    color = newColor
                )
                existingPoints[pointIndex] = updatedPoint
                
                val jsonArray = JSONArray()
                existingPoints.forEach { p ->
                    val jsonObject = JSONObject().apply {
                        put("name", p.name)
                        put("latitude", p.latitude)
                        put("longitude", p.longitude)
                        put("color", AndroidColor.argb(
                            (p.color.alpha * 255).toInt(),
                            (p.color.red * 255).toInt(),
                            (p.color.green * 255).toInt(),
                            (p.color.blue * 255).toInt()
                        ))
                        put("timestamp", p.timestamp)
                    }
                    jsonArray.put(jsonObject)
                }
                
                sharedPreferences.edit()
                    .putString("saved_points", jsonArray.toString())
                    .apply()
                
                // 지도에서 포인트 업데이트
                locationManager?.updatePointsOnMap(existingPoints)
                
                android.util.Log.d("[MainActivity]", "포인트 업데이트 완료: $newName")
            }
            
            showEditDialog = false
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 업데이트 실패: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.stopLocationUpdates()
        locationManager?.unregisterSensors()
    }
}

@Composable
fun ChartPlotterMap(
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit = {},
    showCenterMarker: Boolean = true,
    isDialogShown: Boolean = false, // ⬅ 추가
    showCursor: Boolean = false,
    cursorLatLng: LatLng? = null,
    cursorScreenPosition: android.graphics.PointF? = null,
    onTouchEnd: (LatLng, android.graphics.PointF) -> Unit = { _, _ -> }, // 터치 종료 콜백
    onTouchStart: () -> Unit = { } // 터치 시작 콜백
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapView는 한 번만 생성
    val mapView = remember { MapView(context) }
    var mapLibreMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    // MapView 생명주기 연결
    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(null) // 중요: 최초 1회
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // 다이얼로그가 떠 있을 땐 렌더 일시 정지 → 닫히면 재개
    LaunchedEffect(isDialogShown) {
        if (isDialogShown) mapView.onPause() else mapView.onResume()
    }
    

    // Map이 준비되었을 때 1회만 초기 설정
    val mapConfigured = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = modifier
        ) { mapViewInstance ->
            // 터치 이벤트 리스너 추가 (드래그와 단순 터치 구분, 핀치 줌 감지)
            var isDragging = false
            var touchStartTime = 0L
            var touchStartX = 0f
            var touchStartY = 0f
            var isPinchZoom = false  // 핀치 줌 감지용
            
            mapViewInstance.setOnTouchListener { _, event ->
                // 터치 포인트 개수로 핀치 줌 감지 (더 확실한 방법)
                val pointerCount = event.pointerCount
                if (pointerCount > 1) {
                    isPinchZoom = true
                    Log.d("[MainActivity]", "핀치 줌 감지 (포인트 ${pointerCount}개) - 모든 커서 처리 차단")
                    return@setOnTouchListener false
                }
                
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        touchStartTime = System.currentTimeMillis()
                        touchStartX = event.x
                        touchStartY = event.y
                        isDragging = false
                        isPinchZoom = false
                        Log.d("[MainActivity]", "터치 시작")
                    }
                    android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                        // 두 번째 손가락이 터치되면 핀치 줌으로 판단
                        isPinchZoom = true
                        Log.d("[MainActivity]", "ACTION_POINTER_DOWN - 핀치 줌 감지")
                        return@setOnTouchListener false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        // 핀치 줌 중이면 모든 커서 관련 처리 완전히 차단
                        if (isPinchZoom) {
                            return@setOnTouchListener false
                        }
                        
                        // 움직임이 있으면 드래그로 판단
                        val deltaX = Math.abs(event.x - touchStartX)
                        val deltaY = Math.abs(event.y - touchStartY)
                        if (deltaX > 10 || deltaY > 10) { // 10픽셀 이상 움직이면 드래그
                            isDragging = true
                            onTouchStart() // 드래그 중에는 커서 숨김
                        }
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        val touchDuration = System.currentTimeMillis() - touchStartTime
                        
                        // 핀치 줌이었다면 커서 위치 그대로 유지
                        if (isPinchZoom) {
                            Log.d("[MainActivity]", "핀치 줌 종료 - 커서 위치 고정 유지")
                            return@setOnTouchListener false
                        }
                        
                        // MapLibreMap이 준비된 경우에만 처리
                        mapLibreMapInstance?.let { map ->
                            val x = event.x
                            val y = event.y
                            
                            // 화면 좌표를 지리 좌표로 변환
                            val latLng = map.projection.fromScreenLocation(android.graphics.PointF(x, y))
                            val screenPoint = android.graphics.PointF(x, y)
                            

                            if (isDragging) {
                                // 드래그 종료 시 커서 표시
                                onTouchEnd(latLng, screenPoint)
                                Log.d("[MainActivity]", "드래그 종료 위치에 커서 표시: ${latLng.latitude}, ${latLng.longitude}")
                            } else if (touchDuration < 500) { // 500ms 이내의 짧은 터치는 단순 클릭
                                // 단순 터치 시 커서 표시
                                onTouchEnd(latLng, screenPoint)
                                Log.d("[MainActivity]", "단순 터치 위치에 커서 표시: ${latLng.latitude}, ${latLng.longitude}")
                            } else {
                                // 긴 터치는 무시
                                Log.d("[MainActivity]", "긴 터치 무시")
                            }
                        }
                    }
                }
                false // 기본 터치 이벤트 허용 (지도 이동 가능)
            }
            mapViewInstance.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(map: MapLibreMap) {
                    // MapLibreMap 인스턴스 저장
                    mapLibreMapInstance = map
                    
                    if (!mapConfigured.value) {
                        val centerPoint = LatLng(35.0, 128.0)
                        map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                            .target(centerPoint)
                            .zoom(8.0)
                            .build()

                        PMTilesLoader.loadPMTilesFromAssets(context, map)

                        mapConfigured.value = true       // ⬅ 재초기화 방지
                        onMapReady(map)
                    }
                }
            })
        }

        // 동적 커서 표시 (터치한 위치에)
        if (showCursor && cursorScreenPosition != null) {
            // 터치한 위치에 + 커서를 지도 위에 오버레이로 표시
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 터치한 화면 좌표에 커서 표시 (크기 증가)
                Box(
                    modifier = Modifier
                        .size(48.dp)  // 32dp -> 48dp로 크기 증가
                  
                        .offset(
                            x = with(LocalDensity.current) { 
                                val density = LocalDensity.current.density
                                val cursorSizePx = 48 * density  // 32 -> 48로 변경
                                val offsetPx = cursorSizePx / 2
                                val xDp = (cursorScreenPosition!!.x - offsetPx) / density
                                Log.d("[MainActivity]", "Compose X: ${cursorScreenPosition!!.x}px -> ${xDp}dp (오프셋: ${offsetPx}px)")
                                xDp.dp
                            },
                            y = with(LocalDensity.current) { 
                                val density = LocalDensity.current.density
                                val cursorSizePx = 48 * density  // 32 -> 48로 변경
                                val offsetPx = cursorSizePx / 2
                                val yDp = (cursorScreenPosition!!.y - offsetPx) / density
                                Log.d("[MainActivity]", "Compose Y: ${cursorScreenPosition!!.y}px -> ${yDp}dp (오프셋: ${offsetPx}px)")
                                yDp.dp
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 28.sp,  // 20sp -> 28sp로 폰트 크기 증가
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
        }
    }
}




@Composable
fun PointRegistrationDialog(
    centerCoordinates: String,
    pointName: String,
    onPointNameChange: (String) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    selectedIconType: String,
    onIconTypeChange: (String) -> Unit,
    getNextAvailablePointNumber: () -> Int,
    onRegister: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Red to "빨간색",
        Color.Blue to "파란색", 
        Color.Green to "초록색",
        Color.Yellow to "노란색",
        Color.Magenta to "자홍색",
        Color.Cyan to "청록색"
    )
    
    var showColorMenu by remember { mutableStateOf(false) }
    var focusState by remember { mutableStateOf("name") } // "name", "color", "register", "cancel"
    val focusRequester = remember { FocusRequester() }
    var isButtonPressed by remember { mutableStateOf(false) } // 버튼이 눌렸는지 추적
    var selectedColorIndex by remember { mutableStateOf(0) } // 색상 메뉴에서 선택된 색상 인덱스
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 등록") },
        text = { 
            Column {
                // 좌표 표시
                Text("현재 화면 중앙 좌표:", fontSize = 14.sp)
                Text(
                    text = centerCoordinates,
                    modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 8.dp),
                    fontSize = 12.sp
                )
                
                // 포인트명 입력 (자동 생성 + 편집 가능)
                val autoPointName = "Point${getNextAvailablePointNumber()}"
                val displayPointName = if (pointName.isBlank()) autoPointName else pointName
                
                TextField(
                    value = displayPointName,
                    onValueChange = onPointNameChange,
                    label = { Text("포인트명 (자동: $autoPointName)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(
                            if (focusState == "name") Color.Yellow.copy(alpha = 0.3f) else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                        .focusRequester(focusRequester)
                )
                
                // 색상 선택 (포커스 표시)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(
                            if (focusState == "color") Color.Yellow.copy(alpha = 0.3f) else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                ) {
                    Text("색상:", modifier = Modifier.padding(end = 8.dp))
                    Box(
                        modifier = Modifier
                            .background(selectedColor, CircleShape)
                            .clickable { showColorMenu = true }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .background(selectedColor, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = colors.find { it.first == selectedColor }?.second ?: "빨간색",
                        fontSize = 12.sp
                    )
                }
                
                // 색상 드롭다운 메뉴
                DropdownMenu(
                    expanded = showColorMenu,
                    onDismissRequest = { 
                        // 버튼이 눌렸거나 포커스가 color에 있을 때는 메뉴를 닫지 않음
                        if (!isButtonPressed && focusState != "color") {
                            showColorMenu = false
                        }
                        isButtonPressed = false // 버튼 상태 리셋
                    },
                    modifier = Modifier
                        .focusable()
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.nativeKeyEvent.keyCode) {
                                193 /* BUTTON_6  */ -> {
                                    // 색상 메뉴에서 이전 색상으로 이동
                                    selectedColorIndex = if (selectedColorIndex > 0) selectedColorIndex - 1 else colors.size - 1
                                    Log.d("[Dialog]", "색상 메뉴에서 이전 색상: ${colors[selectedColorIndex].second}")
                                    true
                                }
                                194 /* BUTTON_7  */ -> {
                                    // 색상 메뉴에서 다음 색상으로 이동
                                    selectedColorIndex = (selectedColorIndex + 1) % colors.size
                                    Log.d("[Dialog]", "색상 메뉴에서 다음 색상: ${colors[selectedColorIndex].second}")
                                    true
                                }
                                197 /* BUTTON_10 */ -> {
                                    // 현재 선택된 색상을 적용하고 메뉴 닫기
                                    onColorChange(colors[selectedColorIndex].first)
                                    showColorMenu = false
                                    Log.d("[Dialog]", "색상 선택됨: ${colors[selectedColorIndex].second}")
                                    true
                                }
                                198 /* BUTTON_11 */ -> {
                                    // 색상 메뉴 닫기
                                    showColorMenu = false
                                    Log.d("[Dialog]", "색상 메뉴 닫기")
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    colors.forEachIndexed { index, (color, name) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(color, CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .background(color, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = name,
                                        color = if (index == selectedColorIndex) Color.Blue else Color.Unspecified
                                    )
                                }
                            },
                            onClick = {
                                onColorChange(color)
                                showColorMenu = false
                            }
                        )
                    }
                }
                
                // 아이콘 선택
                Text("아이콘:", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // 원 아이콘
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedIconType == "circle") Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onIconTypeChange("circle") }
                            .border(
                                width = 2.dp,
                                color = if (selectedIconType == "circle") Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color.Red, CircleShape)
                        )
                    }
                    
                    // 삼각형 아이콘
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedIconType == "triangle") Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onIconTypeChange("triangle") }
                            .border(
                                width = 2.dp,
                                color = if (selectedIconType == "triangle") Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▲",
                            fontSize = 20.sp,
                            color = Color.Red
                        )
                    }
                    
                    // 사각형 아이콘
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedIconType == "square") Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onIconTypeChange("square") }
                            .border(
                                width = 2.dp,
                                color = if (selectedIconType == "square") Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Red, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRegister,
                enabled = pointName.isNotBlank(),
                modifier = Modifier.background(
                    if (focusState == "register") Color.Blue.copy(alpha = 0.3f) else Color.Transparent,
                    androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                )
            ) {
                Text("등록")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.background(
                    if (focusState == "cancel") Color.Red.copy(alpha = 0.3f) else Color.Transparent,
                    androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                )
            ) {
                Text("취소")
            }
        },
        // ⬇️ 여기서 193/194/195/196을 포커스 이동으로만 매핑
        modifier = Modifier
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.nativeKeyEvent.keyCode) {
                    193 /* BUTTON_6  */ -> {
                        // 색상 메뉴가 열려있으면 색상 선택, 아니면 포커스 위로 이동
                        if (showColorMenu) {
                            // 색상 메뉴에서 이전 색상으로 이동
                            selectedColorIndex = if (selectedColorIndex > 0) selectedColorIndex - 1 else colors.size - 1
                            Log.d("[Dialog]", "색상 메뉴에서 이전 색상: ${colors[selectedColorIndex].second}")
                        } else {
                            // 포커스 위로 이동
                            focusState = when (focusState) {
                                "name" -> "name"
                                "color" -> "name"
                                "register" -> "color"
                                "cancel" -> "register"
                                else -> "name"
                            }
                            Log.d("[Dialog]", "위로 이동: $focusState")
                        }
                        true
                    }
                    194 /* BUTTON_7  */ -> {
                        // 색상 메뉴가 열려있으면 색상 선택, 아니면 포커스 아래로 이동
                        Log.d("[Dialog]", "194 입력: ${showColorMenu}")
                        if (showColorMenu) {
                            // 색상 메뉴에서 다음 색상으로 이동
                            selectedColorIndex = (selectedColorIndex + 1) % colors.size
                            Log.d("[Dialog]", "색상 메뉴에서 다음 색상: ${colors[selectedColorIndex].second}")
                        } else {
                            // 포커스 아래로 이동
                            focusState = when (focusState) {
                                "name" -> "color"
                                "color" -> "register"
                                "register" -> "cancel"
                                "cancel" -> "cancel"
                                else -> "name"
                            }
                            Log.d("[Dialog]", "아래로 이동: $focusState")
                        }
                        true
                    }
                    195 /* BUTTON_8  */ -> {
                        // 포커스 왼쪽으로 이동
                        if (focusState == "register") {
                            focusState = "cancel"
                        } else if (focusState == "cancel") {
                            focusState = "register"
                        }
                        Log.d("[Dialog]", "좌로 이동: $focusState")
                        true
                    }
                    196 /* BUTTON_9  */ -> {
                        // 포커스 오른쪽으로 이동
                        if (focusState == "register") {
                            focusState = "cancel"
                        } else if (focusState == "cancel") {
                            focusState = "register"
                        }
                        Log.d("[Dialog]", "우로 이동: $focusState")
                        true
                    }
                    197 /* BUTTON_10 */ -> {
                        // 현재 포커스된 요소 선택/액션
                        isButtonPressed = true // 버튼이 눌렸음을 표시
                        if (showColorMenu) {
                            // 색상 메뉴가 열려있으면 현재 선택된 색상을 적용
                            onColorChange(colors[selectedColorIndex].first)
                            showColorMenu = false
                            Log.d("[Dialog]", "색상 선택됨: ${colors[selectedColorIndex].second}")
                        } else {
                            when (focusState) {
                                "name" -> {
                                    focusRequester.requestFocus()
                                    Log.d("[Dialog]", "포인트명 입력 필드 선택됨")
                                }
                                "color" -> {
                                    showColorMenu = true
                                    selectedColorIndex = colors.indexOfFirst { it.first == selectedColor }.takeIf { it >= 0 } ?: 0
                                    Log.d("[Dialog]", "색상 메뉴 열림: $showColorMenu")
                                }
                                "register" -> {
                                    if (pointName.isNotBlank()) {
                                        onRegister()
                                    }
                                    Log.d("[Dialog]", "등록 버튼 클릭됨")
                                }
                                "cancel" -> {
                                    onDismiss()
                                    Log.d("[Dialog]", "취소 버튼 클릭됨")
                                }
                            }
                        }
                        true
                    }
                    198 /* BUTTON_11 */ -> {
                        // 취소
                        if (showColorMenu) {
                            showColorMenu = false
                        } else {
                            onDismiss()
                        }
                        Log.d("[Dialog]", "취소")
                        true
                    }
                    else -> false
                }
            }
    )
}

@Composable
fun PointManageDialog(
    point: SavedPoint,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 관리") },
        text = { 
            Column {
                Text("포인트명: ${point.name}", fontSize = 16.sp)
                Text("위도: ${String.format("%.6f", point.latitude)}", fontSize = 14.sp)
                Text("경도: ${String.format("%.6f", point.longitude)}", fontSize = 14.sp)
                Text("등록일: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(point.timestamp))}", fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onEdit,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Blue
                )
            ) {
                Text("변경")
            }
        },
        dismissButton = {
            Row {
                Button(
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Red
                    )
                ) {
                    Text("삭제")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

@Composable
fun PointEditDialog(
    point: SavedPoint,
    pointName: String,
    onPointNameChange: (String) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Red to "빨간색",
        Color.Blue to "파란색", 
        Color.Green to "초록색",
        Color.Yellow to "노란색",
        Color.Magenta to "자홍색",
        Color.Cyan to "청록색"
    )
    
    var showColorMenu by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 편집") },
        text = { 
            Column {
                // 좌표 표시
                Text("좌표:", fontSize = 14.sp)
                Text(
                    text = "위도: ${String.format("%.6f", point.latitude)}\n경도: ${String.format("%.6f", point.longitude)}",
                    modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 8.dp),
                    fontSize = 12.sp
                )
                
                // 포인트명 입력
                TextField(
                    value = pointName,
                    onValueChange = onPointNameChange,
                    label = { Text("포인트명") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                // 색상 선택
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("색상:", modifier = Modifier.padding(end = 8.dp))
                    Box(
                        modifier = Modifier
                            .background(selectedColor, CircleShape)
                            .clickable { showColorMenu = true }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .background(selectedColor, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = colors.find { it.first == selectedColor }?.second ?: "빨간색",
                        fontSize = 12.sp
                    )
                }
                
                // 색상 드롭다운 메뉴
                DropdownMenu(
                    expanded = showColorMenu,
                    onDismissRequest = { showColorMenu = false }
                ) {
                    colors.forEach { (color, name) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(color, CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .background(color, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            },
                            onClick = {
                                onColorChange(color)
                                showColorMenu = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = pointName.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun PointDeleteListDialog(
    points: List<SavedPoint>,
    onDeletePoint: (SavedPoint) -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<SavedPoint?>(null) }
    
    // 삭제 확인 다이얼로그
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("포인트 삭제") },
            text = { 
                Text("'${showDeleteConfirm!!.name}' 포인트를 삭제하시겠습니까?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePoint(showDeleteConfirm!!)
                        showDeleteConfirm = null
                        onDismiss()
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 포인트 목록 다이얼로그
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 삭제") },
        text = {
            if (points.isEmpty()) {
                Text("삭제할 포인트가 없습니다.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(points) { point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(point.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = point.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "위도: ${String.format("%.6f", point.latitude)}\n경도: ${String.format("%.6f", point.longitude)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Button(
                                onClick = { showDeleteConfirm = point },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("삭제", color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ChartPlotterMapPreview() {
    ChartPlotterTheme {
        Text("Chart Plotter Map Preview")
    }
}