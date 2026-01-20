package com.marineplay.chartplotter

import android.Manifest
import android.R.attr
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Switch
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.toArgb
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
import com.marineplay.chartplotter.ui.components.map.ChartPlotterMap
import com.marineplay.chartplotter.ui.components.dialogs.PointRegistrationDialog
import com.marineplay.chartplotter.ui.components.dialogs.PointManageDialog
import com.marineplay.chartplotter.ui.components.dialogs.PointEditDialog
import com.marineplay.chartplotter.ui.components.dialogs.PointDeleteListDialog
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import org.maplibre.android.geometry.LatLngBounds
import android.R.attr.onClick
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.entities.TrackPoint
import com.marineplay.chartplotter.viewmodel.MainViewModel



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
    // 지도 이동 감지용 Handler (클래스 레벨에서 관리)
    private var mapStabilityHandler: android.os.Handler? = null
    private var mapStabilityRunnable: Runnable? = null
    private var mapLibreMap: MapLibreMap? = null

    // 헬퍼들
    private lateinit var pointHelper: PointHelper
    private lateinit var destinationHelper: DestinationHelper
    private lateinit var trackRepository: com.marineplay.chartplotter.domain.repositories.TrackRepository
    private lateinit var sharedPreferences: SharedPreferences

    // Handler 및 Runnable (줌, 항적 타이머 등)
    private var zoomHandler: android.os.Handler? = null
    private var zoomRunnable: Runnable? = null
    // 여러 항적 동시 기록을 위한 타이머 관리
    private val trackTimerHandlers = mutableMapOf<String, android.os.Handler>() // trackId -> Handler
    private val trackTimerRunnables = mutableMapOf<String, Runnable>() // trackId -> Runnable
    
    // ViewModel 참조 (onKeyDown에서 사용하기 위해)
    private var mainViewModel: MainViewModel? = null
    
    // EntryMode 저장 (뒤로가기 처리용)
    private var currentEntryMode: EntryMode = EntryMode.CHART_ONLY

    // 줌 함수들
    private fun startContinuousZoomIn(viewModel: MainViewModel) {
        val mapUiState = viewModel.mapUiState
        if (mapUiState.isZoomInLongPressed) return
        
        viewModel.updateIsZoomInLongPressed(true)
        zoomHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var iteration = 0
        
        zoomRunnable = object : Runnable {
            override fun run() {
                val mapUiState = viewModel.mapUiState
                if (mapUiState.isZoomInLongPressed) {
                    zoomIn(viewModel)
                    // 가속도 효과: 처음에는 느리게(500ms), 점점 빨라져서 최소 50ms까지
                    val delayTime = (100L / (1.0 + iteration * 0.15)).toLong().coerceAtLeast(15L)
                    zoomHandler?.postDelayed(this, delayTime)
                    iteration++
                }
            }
        }
        zoomHandler?.post(zoomRunnable!!)
    }
    
    private fun stopContinuousZoomIn(viewModel: MainViewModel) {
        viewModel.updateIsZoomInLongPressed(false)
        zoomHandler?.removeCallbacks(zoomRunnable!!)
    }
    
    private fun startContinuousZoomOut(viewModel: MainViewModel) {
        val mapUiState = viewModel.mapUiState
        if (mapUiState.isZoomOutLongPressed) return
        
        viewModel.updateIsZoomOutLongPressed(true)
        zoomHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var iteration = 0
        
        zoomRunnable = object : Runnable {
            override fun run() {
                val mapUiState = viewModel.mapUiState
                if (mapUiState.isZoomOutLongPressed) {
                    zoomOut(viewModel)
                    // 가속도 효과: 처음에는 느리게(500ms), 점점 빨라져서 최소 50ms까지
                    val delayTime = (100L / (1.0 + iteration * 0.15)).toLong().coerceAtLeast(10L)
                    zoomHandler?.postDelayed(this, delayTime)
                    iteration++
                }
            }
        }
        zoomHandler?.post(zoomRunnable!!)
    }
    
    private fun stopContinuousZoomOut(viewModel: MainViewModel) {
        viewModel.updateIsZoomOutLongPressed(false)
        zoomHandler?.removeCallbacks(zoomRunnable!!)
    }
    
    private fun zoomIn(viewModel: MainViewModel) {
        // ✅ 줌 로직 단일화: 실제 줌/커서 보정은 ViewModel/UseCase에서 수행
        // (UI 버튼/하드웨어 키/리모컨 등 입력 경로가 달라도 동일 동작 보장)
        viewModel.zoomIn(mapLibreMap)
    }
    
    private fun zoomOut(viewModel: MainViewModel) {
        // ✅ 줌 로직 단일화: 실제 줌/커서 보정은 ViewModel/UseCase에서 수행
        viewModel.zoomOut(mapLibreMap)
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
    
    // 항적 기록 시작 (단일 항적만 기록 가능)
    private fun startTrackRecording(track: Track, viewModel: MainViewModel) {
        // 기존에 기록 중인 항적이 있으면 타이머 정지
        val currentRecordingTracks = viewModel.trackUiState.recordingTracks
        currentRecordingTracks.keys.forEach { existingTrackId ->
            stopTrackRecording(existingTrackId, viewModel)
        }
        
        // ViewModel에서 기록 시작 (기존 항적은 자동으로 중지됨)
        viewModel.startTrackRecording(track)
        
        // 시간 간격 설정인 경우 타이머 시작
        if (track.intervalType == "time") {
            startTrackTimer(track.id, track.timeInterval, viewModel)
        }
        
        Log.d("[MainActivity]", "항적 기록 시작: ${track.name} (간격: ${if (track.intervalType == "time") "${track.timeInterval}ms" else "${track.distanceInterval}m"})")
    }
    
    // 항적 타이머 시작
    private fun startTrackTimer(trackId: String, timeInterval: Long, viewModel: MainViewModel) {
        // 이미 타이머가 실행 중이면 시작하지 않음
        if (trackTimerHandlers.containsKey(trackId) && trackTimerRunnables.containsKey(trackId)) {
            Log.d("[MainActivity]", "타이머가 이미 실행 중입니다: $trackId")
            return
        }
        
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
                override fun run() {
                    val trackUiState = viewModel.trackUiState
                    val gpsUiState = viewModel.gpsUiState
                val recordingState = trackUiState.recordingTracks[trackId]
                
                if (recordingState != null && gpsUiState.lastGpsLocation != null) {
                        // 마지막 GPS 위치를 항적 점으로 추가
                        val lastGpsLocation = gpsUiState.lastGpsLocation!!
                        
                    // ViewModel을 통해 점 추가 (타이머에서 호출되었음을 표시)
                    viewModel.addTrackPointIfNeeded(lastGpsLocation.latitude, lastGpsLocation.longitude, isTimerTriggered = true)
                        
                        // 다음 타이머 예약
                    handler.postDelayed(this, timeInterval)
                } else {
                    Log.w("[MainActivity]", "타이머 실행 중 GPS 위치 또는 기록 상태 없음: $trackId")
                }
            }
        }
        
        trackTimerHandlers[trackId] = handler
        trackTimerRunnables[trackId] = runnable
        
        // 타이머 즉시 시작 (첫 번째 점은 GPS 업데이트 시 추가되므로, 이후부터 주기적으로 추가)
        handler.postDelayed(runnable, timeInterval)
        Log.d("[MainActivity]", "타이머 시작: $trackId (간격: ${timeInterval}ms)")
    }
    
    // 앱 시작 시 자동 기록을 위한 타이머 시작 (외부에서 호출 가능)
    fun startTrackTimerForAutoRecording(track: Track, viewModel: MainViewModel) {
        if (track.intervalType == "time") {
            startTrackTimer(track.id, track.timeInterval, viewModel)
        }
    }
    
    // 항적 기록 중지 (특정 항적)
    private fun stopTrackRecording(trackId: String? = null, viewModel: MainViewModel) {
        val targetTrackId = trackId ?: viewModel.trackUiState.currentRecordingTrack?.id
        if (targetTrackId == null) return
        
        // 타이머 정지
        trackTimerRunnables[targetTrackId]?.let { runnable ->
            trackTimerHandlers[targetTrackId]?.removeCallbacks(runnable)
        }
        trackTimerHandlers.remove(targetTrackId)
        trackTimerRunnables.remove(targetTrackId)
        
        // ViewModel에서 기록 중지 (여러 항적 동시 기록 지원)
        viewModel.stopTrackRecording(targetTrackId)
        
        Log.d("[MainActivity]", "항적 기록 중지: $targetTrackId")
    }
    
    // GPS 위치 업데이트 시 항적 점 추가 (여러 항적 동시 기록 지원)
    internal fun addTrackPointIfNeeded(latitude: Double, longitude: Double, viewModel: MainViewModel) {
        val trackUiState = viewModel.trackUiState
        if (trackUiState.recordingTracks.isEmpty()) return
        
        val currentTime = System.currentTimeMillis()
        
        // 마지막 GPS 위치 업데이트
        viewModel.updateGpsLocation(latitude, longitude, true)
        
        // 각 기록 중인 항적에 대해 처리
        trackUiState.recordingTracks.forEach { (trackId, recordingState) ->
            // ViewModel의 캐시를 사용하여 항적 정보 가져오기 (성능 최적화)
            val track = viewModel.getTrack(trackId) ?: return@forEach
            
            when (track.intervalType) {
            "time" -> {
                    // 시간 간격 기준: 첫 번째 점 추가 후 타이머 시작
                    if (recordingState.lastTrackPointTime == 0L) {
                    // 첫 번째 점 추가
                        viewModel.addTrackPointIfNeeded(latitude, longitude)
                    
                    // 타이머 시작 (설정한 시간 간격마다 점 추가)
                        startTrackTimer(trackId, track.timeInterval, viewModel)
                }
                // 이후 점들은 타이머가 추가함
            }
            "distance" -> {
                // 거리 간격 기준: GPS 업데이트마다 거리 체크
                var shouldAddPoint = false
                
                    if (recordingState.lastTrackPointLocation == null) {
                    // 첫 번째 점
                    shouldAddPoint = true
                } else {
                    val distance = calculateDistance(
                            recordingState.lastTrackPointLocation!!.latitude,
                            recordingState.lastTrackPointLocation!!.longitude,
                        latitude,
                        longitude
                    )
                        if (distance >= track.distanceInterval) {
                        shouldAddPoint = true
                    }
                }
                
                if (shouldAddPoint) {
                        viewModel.addTrackPointIfNeeded(latitude, longitude)
        }
    }
            }
        }
    }
    

    // 지도 회전 제어 함수
    private fun updateMapRotation(viewModel: MainViewModel) {
        mapLibreMap?.let { map ->
            val mapUiState = viewModel.mapUiState
            
            when (mapUiState.mapDisplayMode) {
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
                    mapUiState.coursePoint?.let { point ->
                        val currentLocation = locationManager?.getCurrentLocationObject()
                        if (currentLocation != null) {
                            val bearing = calculateBearing(
                                currentLocation.latitude, currentLocation.longitude,
                                point.latitude, point.longitude
                            )
                            
                            // 선박 위치를 중앙에 오도록 설정
                            val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                            val newPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(currentLatLng) // 선박 위치를 중앙으로
                                .zoom(map.cameraPosition.zoom)
                                .bearing(bearing.toDouble())
                                .build()
                            map.cameraPosition = newPosition
                            
                            // 커서 숨기기
                            viewModel.updateShowCursor(false)
                            viewModel.updateCursorLatLng(null)
                            viewModel.updateCursorScreenPosition(null)

                            // 코스업 선 그리기
//                            val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
//                            val pointLatLng = LatLng(point.latitude, point.longitude)
//                            PMTilesLoader.addCourseLine(map, currentLatLng, pointLatLng)
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
    
    // 위치 권한 요청 (외부에서 호출 가능)
    fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // 이미 권한이 있으면 위치 업데이트 시작
            locationManager?.startLocationUpdates()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 헬퍼들 초기화
        pointHelper = PointHelper(this)
        val trackLocalDataSource = com.marineplay.chartplotter.data.datasources.TrackLocalDataSource(this)
        trackRepository = com.marineplay.chartplotter.data.repositories.TrackRepositoryImpl(trackLocalDataSource)
        val systemSettingsReader = com.marineplay.chartplotter.data.SystemSettingsReader(this)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("chart_plotter_points", Context.MODE_PRIVATE)

        // 저장된 포인트들 로드
        val savedPoints = pointHelper.loadPointsFromLocal()
        android.util.Log.d("[MainActivity]", "저장된 포인트 ${savedPoints.size}개 로드 완료")

        // MapLibre 초기화
        MapLibre.getInstance(this)

        // Intent에서 ENTRY_MODE 읽기
        // Intent Contract:
        // - Action: android.intent.action.MAIN
        // - Component: com.marineplay.chartplotter.MainActivity
        // - Extra Key: ENTRY_MODE
        // - Extra Type: String (CHART_ONLY, BLACKBOX_ONLY, SPLIT)
        val entryModeString = intent.getStringExtra(EntryMode.INTENT_EXTRA_KEY)
        currentEntryMode = EntryMode.fromString(entryModeString)
        
        android.util.Log.d("[MainActivity]", "ENTRY_MODE: $entryModeString -> $currentEntryMode")

        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            ChartPlotterTheme {
                // ViewModel 생성 (Factory 사용)
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.provideFactory(
                        pointHelper = pointHelper,
                        trackRepository = trackRepository,
                        locationManager = locationManager,
                        systemSettingsReader = systemSettingsReader
                    )
                )
                
                // onKeyDown에서 사용하기 위해 ViewModel 참조 저장
                mainViewModel = viewModel
                
                // ChartPlotterApp 호출 (EntryMode에 따라 화면 구성)
                com.marineplay.chartplotter.ui.ChartPlotterApp(
                    entryMode = currentEntryMode,
                    viewModel = viewModel,
                    activity = this@MainActivity,
                    pointHelper = pointHelper,
                    onMapLibreMapChange = { mapLibreMap = it },
                    onLocationManagerChange = { locationManager = it }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // 새로운 Intent에서 ENTRY_MODE 읽기
        val entryModeString = intent?.getStringExtra(EntryMode.INTENT_EXTRA_KEY)
        val newEntryMode = EntryMode.fromString(entryModeString)
        
        if (newEntryMode != currentEntryMode) {
            android.util.Log.d("[MainActivity]", "onNewIntent: ENTRY_MODE 변경 $currentEntryMode -> $newEntryMode")
            currentEntryMode = newEntryMode
            
            // Activity 재생성하여 새로운 EntryMode 반영
            recreate()
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val viewModel = mainViewModel ?: return false
        
        // 하드웨어 키 이벤트는 MainActivity에서 처리하고, 비즈니스 로직은 ViewModel로 위임
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_3 -> {
                // 포인트 등록 다이얼로그 준비
                viewModel.preparePointRegistration(mapLibreMap?.cameraPosition?.target)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_4 -> {
                // 줌 아웃
                viewModel.zoomOut(mapLibreMap)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_5 -> {
                // 줌 인
                viewModel.zoomIn(mapLibreMap)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_6 -> {
                // 화면 위로 이동
                viewModel.moveMap(mapLibreMap, "up")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_7 -> {
                // 화면 아래로 이동
                viewModel.moveMap(mapLibreMap, "down")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_8 -> {
                // 화면 왼쪽으로 이동
                viewModel.moveMap(mapLibreMap, "left")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_9 -> {
                // 화면 오른쪽으로 이동
                viewModel.moveMap(mapLibreMap, "right")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_1 -> {
                // 줌 아웃 버튼 (롱 클릭으로 연속 줌)
                if (event?.isLongPress == true) {
                    startContinuousZoomOut(viewModel)
                } else {
                    viewModel.zoomOut(mapLibreMap)
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_2 -> {
                // 줌 인 버튼 (롱 클릭으로 연속 줌)
                if (event?.isLongPress == true) {
                    startContinuousZoomIn(viewModel)
                } else {
                    viewModel.zoomIn(mapLibreMap)
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_5 -> {
                // 커서 클릭 이벤트 (목적지/포인트 클릭 처리)
                val savedPoints = pointHelper.loadPointsFromLocal()
                viewModel.handleCursorPointSelection(
                    mapLibreMap,
                    savedPoints,
                    ::calculateScreenDistance
                )
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun registerPoint(viewModel: MainViewModel) {
        val pointUiState = viewModel.pointUiState
        pointUiState.currentLatLng?.let { latLng ->
            // 자동 포인트명 생성 (사용 가능한 최소 번호)
            val autoPointName = "Point${getNextAvailablePointNumber()}"
            val finalPointName = if (pointUiState.pointName.isBlank()) autoPointName else pointUiState.pointName
            
            val point = SavedPoint(
                name = finalPointName,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                color = pointUiState.selectedColor,
                iconType = pointUiState.selectedIconType,
                timestamp = System.currentTimeMillis()
            )
            
            savePointToLocal(point)
            val allPoints = loadPointsFromLocal()
            viewModel.updatePointCount(allPoints.size)
            
            // 새로 등록된 포인트를 지도에 즉시 표시
            locationManager?.updatePointsOnMap(allPoints)
            
            android.util.Log.d("[MainActivity]", "포인트 등록 완료: $finalPointName, 좌표: $latLng, 색상: ${pointUiState.selectedColor}, 아이콘: ${pointUiState.selectedIconType}")
            viewModel.updateShowDialog(false)
            // 포인트 등록 후 커서 숨김
            viewModel.updateShowCursor(false)
            viewModel.updateCursorLatLng(null)
            viewModel.updateCursorScreenPosition(null)
        }
    }
    
    // 빠른 포인트 생성 (다이얼로그 없이 바로 생성)
    private fun createQuickPoint(viewModel: MainViewModel) {
        val mapUiState = viewModel.mapUiState
        val pointUiState = viewModel.pointUiState
        mapUiState.cursorLatLng?.let { latLng ->
            val autoPointName = "Point${getNextAvailablePointNumber()}"
            
            val point = SavedPoint(
                name = autoPointName,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                color = pointUiState.selectedColor,
                iconType = pointUiState.selectedIconType,
                timestamp = System.currentTimeMillis()
            )
            
            savePointToLocal(point)
            val allPoints = loadPointsFromLocal()
            viewModel.updatePointCount(allPoints.size)
            
            // 새로 등록된 포인트를 지도에 즉시 표시
            locationManager?.updatePointsOnMap(allPoints)
            
            android.util.Log.d("[MainActivity]", "빠른 포인트 생성 완료: $autoPointName, 좌표: $latLng, 색상: ${pointUiState.selectedColor}, 아이콘: ${pointUiState.selectedIconType}")
            
            // 포인트 생성 후 커서 숨김
            viewModel.updateShowCursor(false)
            viewModel.updateCursorLatLng(null)
            viewModel.updateCursorScreenPosition(null)
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
        return pointHelper.loadPointsFromLocal().map { pointHelperPoint ->
            SavedPoint(
                name = pointHelperPoint.name,
                latitude = pointHelperPoint.latitude,
                longitude = pointHelperPoint.longitude,
                color = Color(pointHelperPoint.color.toArgb()),
                iconType = pointHelperPoint.iconType,
                timestamp = pointHelperPoint.timestamp
            )
        }
    }
    
    /** 포인트 삭제 */
    private fun deletePoint(point: SavedPoint, viewModel: MainViewModel) {
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
            viewModel.updatePointCount(existingPoints.size)
            
            android.util.Log.d("[MainActivity]", "포인트 삭제 완료: ${point.name}")
            viewModel.updateShowPointManageDialog(false)
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 삭제 실패: ${e.message}")
        }
    }
    
    /** 포인트 업데이트 */
    private fun updatePoint(originalPoint: SavedPoint, newName: String, newColor: Color, viewModel: MainViewModel) {
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
            
            viewModel.updateShowEditDialog(false)
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 업데이트 실패: ${e.message}")
        }
    }

    override fun onBackPressed() {
        // CHART_ONLY 모드가 아닌 경우 (카메라, AIS, 계기판 등) 뒤로가기 시 앱 종료
        if (currentEntryMode != EntryMode.CHART_ONLY) {
            android.util.Log.d("[MainActivity]", "뒤로가기: $currentEntryMode 모드에서 앱 종료")
            // finishAffinity()를 사용하여 Task 전체를 종료
            finishAffinity()
        } else {
            // CHART_ONLY 모드에서는 기본 동작 (차트 화면 유지)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.stopLocationUpdates()
        locationManager?.unregisterSensors()
    }
}
