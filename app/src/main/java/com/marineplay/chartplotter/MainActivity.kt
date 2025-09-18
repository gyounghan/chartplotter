package com.marineplay.chartplotter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

class MainActivity : ComponentActivity() {
    
    private var locationManager: LocationManager? = null
    private var mapLibreMap: MapLibreMap? = null
    
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
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationManager?.stopLocationUpdates()
    }
}
@Composable
fun ChartPlotterMap(
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { mapViewInstance ->
        mapViewInstance.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(map: MapLibreMap) {
                // 기본 카메라 위치 설정 (한국 근해)
                val centerPoint = LatLng(35.0, 128.0)
                map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                    .target(centerPoint)
                    .zoom(8.0)
                    .build()

                // PMTiles 파일을 자동으로 로드
                PMTilesLoader.loadPMTilesFromAssets(context, map)
                
                // 지도가 준비되면 콜백 호출
                onMapReady(map)
            }
        })
    }
}



@Preview(showBackground = true)
@Composable
fun ChartPlotterMapPreview() {
    ChartPlotterTheme {
        Text("Chart Plotter Map Preview")
    }
}