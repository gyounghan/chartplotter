package com.kumhomarine.chartplotter.presentation.components.map

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import com.kumhomarine.chartplotter.PMTilesLoader

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
    onTouchStart: () -> Unit = { }, // 터치 시작 콜백
    fontSize: Float = 14f // 시스템 설정 문자 크기 - 전자해도 텍스트에 반영
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapView는 한 번만 생성
    val mapView = remember {
        val createStartTime = System.currentTimeMillis()
        val view = MapView(context)
        val createElapsed = System.currentTimeMillis() - createStartTime
        Log.d("[ChartPlotterMap]", "⏱️ [MapView 생성] - ${createElapsed}ms")
        view
    }
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
    
    // 문자 크기 변경 시 전자해도 텍스트 레이어 다시 로드 (설정에서 변경 시 즉시 반영)
    LaunchedEffect(fontSize) {
        mapLibreMapInstance?.let { map ->
            PMTilesLoader.loadPMTiles(context, map, fontSize)
        }
    }
    
    // Map이 준비되었을 때 1회만 초기 설정
    val mapConfigured = remember { mutableStateOf(false) }
    
    // ✅ getMapAsync는 LaunchedEffect에서 한 번만 호출
    LaunchedEffect(Unit) {
        if (!mapConfigured.value) {
            val getMapAsyncStartTime = System.currentTimeMillis()
            Log.d("[ChartPlotterMap]", "📞 [시작] getMapAsync 호출 (한 번만)")
            
            mapView.getMapAsync { map ->
                val getMapAsyncElapsed = System.currentTimeMillis() - getMapAsyncStartTime
                Log.d("[ChartPlotterMap]", "⏱️ [getMapAsync 완료] - ${getMapAsyncElapsed}ms")
                
                val onMapReadyStartTime = System.currentTimeMillis()
                Log.d("[ChartPlotterMap]", "🗺️ [시작] onMapReady 콜백")
                
                // MapLibreMap 인스턴스 저장
                mapLibreMapInstance = map
                
                if (!mapConfigured.value) {
                    val cameraStartTime = System.currentTimeMillis()
                    // 기본 위치 설정 (GPS 수신 전에도 맵이 즉시 표시되도록)
                    val centerPoint = LatLng(35.136565, 129.071632)
                    map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(centerPoint)
                        .zoom(11.0)
                        .build()
                    val cameraElapsed = System.currentTimeMillis() - cameraStartTime
                    Log.d("[ChartPlotterMap]", "⏱️ [카메라 설정] 기본 위치: $centerPoint - ${cameraElapsed}ms")

                    val pmtilesStartTime = System.currentTimeMillis()
                    PMTilesLoader.loadPMTiles(context, map, fontSize)
                    val pmtilesElapsed = System.currentTimeMillis() - pmtilesStartTime
                    Log.d("[ChartPlotterMap]", "⏱️ [PMTiles 로드 호출] - ${pmtilesElapsed}ms")

                    mapConfigured.value = true
                    
                    val callbackStartTime = System.currentTimeMillis()
                    onMapReady(map)
                    val callbackElapsed = System.currentTimeMillis() - callbackStartTime
                    Log.d("[ChartPlotterMap]", "⏱️ [onMapReady 콜백 호출] - ${callbackElapsed}ms")
                    
                    val totalElapsed = System.currentTimeMillis() - onMapReadyStartTime
                    Log.d("[ChartPlotterMap]", "✅ [완료] onMapReady (총 ${totalElapsed}ms)")
                } else {
                    val totalElapsed = System.currentTimeMillis() - onMapReadyStartTime
                    Log.d("[ChartPlotterMap]", "⏭️ [스킵] 이미 설정됨 - ${totalElapsed}ms")
                }
            }
        }
    }

    // 터치 리스너 설정 여부 추적 (update 블록 밖에서 remember 사용)
    val touchListenerSet = remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { 
                val factoryStartTime = System.currentTimeMillis()
                Log.d("[ChartPlotterMap]", "🏭 [시작] AndroidView factory")
                val view = mapView
                val factoryElapsed = System.currentTimeMillis() - factoryStartTime
                Log.d("[ChartPlotterMap]", "⏱️ [완료] AndroidView factory - ${factoryElapsed}ms")
                view
            },
            modifier = modifier
        ) { mapViewInstance ->
            // ✅ update 블록에서는 터치 리스너만 설정
            // getMapAsync는 절대 호출하지 않음!
            
            // 터치 리스너는 한 번만 설정
            if (!touchListenerSet.value) {
                val touchListenerStartTime = System.currentTimeMillis()
                Log.d("[ChartPlotterMap]", "👆 [시작] 터치 리스너 설정")
                
                // 터치 이벤트 리스너 추가 (드래그와 단순 터치 구분, 핀치 줌 감지)
                // 클로저로 외부 변수 캡처
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
                
                touchListenerSet.value = true
                val touchListenerElapsed = System.currentTimeMillis() - touchListenerStartTime
                Log.d("[ChartPlotterMap]", "⏱️ [완료] 터치 리스너 설정 - ${touchListenerElapsed}ms")
            }
        }

        // 동적 커서 표시 (터치한 위치에)
        // contentWindowInsets=0으로 지도가 전체 화면을 쓰므로, 터치 좌표는 전체 화면 기준.
        // padding 없이 동일 좌표계를 사용해야 커서가 터치 위치에 정확히 맞춰짐.
        if (showCursor && cursorScreenPosition != null) {
            // 터치한 위치에 + 커서를 지도 위에 오버레이로 표시
            Box(modifier = Modifier.fillMaxSize()) {
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

