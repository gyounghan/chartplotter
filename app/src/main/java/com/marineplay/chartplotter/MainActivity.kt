package com.marineplay.chartplotter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.marineplay.chartplotter.ui.theme.ChartPlotterTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // MapLibre 초기화
        MapLibre.getInstance(this)

        setContent {
            ChartPlotterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChartPlotterMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
@Composable
fun ChartPlotterMap(modifier: Modifier = Modifier) {
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