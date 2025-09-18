package com.marineplay.chartplotter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.marineplay.chartplotter.ui.theme.ChartPlotterTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    
    private var locationManager: LocationManager? = null
    private var mapLibreMap: MapLibreMap? = null
    private var showDialog by mutableStateOf(false)
    private var isMapInitialized by mutableStateOf(false)

    // 위치 권한 요청 런처
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // 정확한 위치 권한이 허용됨
                locationManager?.startLocationUpdates()
                locationManager?.startAutoTracking()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // 대략적인 위치 권한이 허용됨
                locationManager?.startLocationUpdates()
                locationManager?.startAutoTracking()
            }
            else -> {
                // 위치 권한이 거부됨
                android.util.Log.w("[MainActivity]", "위치 권한이 거부되었습니다.")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // MapLibre 초기화
        MapLibre.getInstance(this)

        setContent {
            ChartPlotterTheme {
                // 다이얼로그 표시
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Button 3") },
                        text = { Text("Button 3이 눌렸습니다.") },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("확인")
                            }
                        }
                    )
                }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                locationManager?.startAutoTracking()
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ship),
                                contentDescription = "내 위치로 이동",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    ChartPlotterMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onMapReady = { map ->
                            if (!isMapInitialized) {
                                mapLibreMap = map
                                locationManager = LocationManager(this@MainActivity, map)

                                // PMTiles 로드 후 선박 아이콘 추가를 위해 스타일 로드 완료를 기다림
                                map.getStyle { style ->
                                    locationManager?.addShipToMap(style)
                                }

                                // 지도 터치/드래그 감지하여 자동 추적 중지
                                map.addOnCameraMoveListener {
                                    locationManager?.stopAutoTracking()
                                }

                                // 위치 권한 확인 및 요청
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    locationManager?.startLocationUpdates()
                                    // 처음 시작 시 자동 추적 활성화
                                    locationManager?.startAutoTracking()
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
                        isDialogShown = showDialog // ⬅ 전달
                    )
                }
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_3 -> {
                showDialog = true
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.stopLocationUpdates()
    }
}

@Composable
fun ChartPlotterMap(
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit = {},
    showCenterMarker: Boolean = true,
    isDialogShown: Boolean = false, // ⬅ 추가
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapView는 한 번만 생성
    val mapView = remember { MapView(context) }

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
            mapViewInstance.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(map: MapLibreMap) {
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

        if (showCenterMarker) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "+")
            }
        }
    }
}




@Preview(showBackground = true)
@Composable
fun ChartPlotterMapPreview() {
    ChartPlotterTheme {
        Text("Chart Plotter Map Preview")
    }
}