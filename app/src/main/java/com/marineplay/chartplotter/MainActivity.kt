package com.marineplay.chartplotter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.io.File

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

                // PMTiles 파일을 로드
                loadPMTilesFromAssets(context, map)
            }
        })
    }
}

private fun copyPmtilesFromAssets(context: android.content.Context, assetPath: String, outName: String): File {
    val outDir = File(context.filesDir, "pmtiles").apply { mkdirs() }
    val out = File(outDir, outName)
    context.assets.open(assetPath).use { input ->
        out.outputStream().use { output -> input.copyTo(output) }
    }
    return out
}

private fun loadPMTilesFromAssets(context: android.content.Context, map: MapLibreMap) {
    val styleJson = """
{
  "version": 8,
  "sources": {},
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": {
        "background-color": "#FFFFFF"
      }
    }
  ]
}
""".trimIndent()
    try {
        // 두 개의 PMTiles 파일을 모두 복사
        val lineTilesFile = copyPmtilesFromAssets(context, "pmtiles/lineTiles.pmtiles", "lineTiles.pmtiles")
        val areaTilesFile = copyPmtilesFromAssets(context, "pmtiles/areaTiles.pmtiles", "areaTiles.pmtiles")
        val depthTilesFile =  copyPmtilesFromAssets(context, "pmtiles/p_1_129_soundg.pmtiles", "depthTiles.pmtiles")
        Log.d("[PMTiles]", "lineTiles 파일 복사 완료: ${lineTilesFile.absolutePath}")
        Log.d("[PMTiles]", "areaTiles 파일 복사 완료: ${areaTilesFile.absolutePath}")

        // 세 PMTiles 파일이 모두 존재하는지 확인
        if (lineTilesFile.exists() && areaTilesFile.exists() && depthTilesFile.exists()) {
            try {
                Log.d("[PMTiles]", "lineTiles 파일 크기: ${lineTilesFile.length()} bytes")
                Log.d("[PMTiles]", "areaTiles 파일 크기: ${areaTilesFile.length()} bytes")
                Log.d("[PMTiles]", "depthTiles 파일 크기: ${depthTilesFile.length()} bytes")

                // PMTiles URL 생성
                val lineTilesUrl = "pmtiles://file://${lineTilesFile.absolutePath}"
                val areaTilesUrl = "pmtiles://file://${areaTilesFile.absolutePath}"
                val depthTilesUrl = "pmtiles://file://${depthTilesFile.absolutePath}"

                Log.d("[PMTiles]", "lineTiles URL: $lineTilesUrl")
                Log.d("[PMTiles]", "areaTiles URL: $areaTilesUrl")
                Log.d("[PMTiles]", "depthTiles URL: $depthTilesUrl")

                try {
                    // 기본 스타일을 로드하고 두 개의 PMTiles를 데이터 소스로 추가
                    map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
                        // ↓ 여기서부터 소스/레이어 추가 그대로
                        // lineTiles PMTiles를 벡터 소스로 추가
                        val lineTilesTileset = TileSet("pmtiles", lineTilesUrl)
                        val lineTilesSource = VectorSource("lineTiles-source", lineTilesUrl)
                        style.addSource(lineTilesSource)

                        // areaTiles PMTiles를 벡터 소스로 추가
                        val areaTilesTileset = TileSet("pmtiles", areaTilesUrl)
                        val areaTilesSource = VectorSource("areaTiles-source", areaTilesUrl)
                        style.addSource(areaTilesSource)

                        // depthTiles PMTiles를 벡터 소스로 추가
                        val depthTilesTileset = TileSet("pmtiles", depthTilesUrl)
                        val depthTilesSource = VectorSource("depthTiles-source", depthTilesUrl)
                        style.addSource(depthTilesSource)
                        Log.d("[PMTiles]", "depthTiles 벡터 소스 추가 완료")

                        // PMTiles 메타데이터 확인을 위한 로그
                        try {
                            Log.d("[PMTiles]", "depthTiles 소스 정보 확인 중...")
                            // 소스가 로드된 후 메타데이터 확인
                            val source = style.getSource("depthTiles-source")
                            if (source != null) {
                                Log.d("[PMTiles]", "depthTiles 소스 존재 확인됨")
                            } else {
                                Log.e("[PMTiles]", "depthTiles 소스를 찾을 수 없음")
                            }
                        } catch (e: Exception) {
                            Log.e("[PMTiles]", "depthTiles 소스 확인 중 오류: ${e.message}")
                        }

                        // lineTiles 데이터를 표시할 레이어들 추가
                        val lineLayer = LineLayer("lineTiles-lines", "lineTiles-source").apply {
                            setSourceLayer("line_map") // PMTiles의 실제 레이어명에 맞게 조정
                            setMinZoom(0f)
                            setMaxZoom(24f)
                            val colorExpr = match(
                                toNumber(coalesce(get("COLOR"), get("BFR_COLOR"), get("LAYER"))),

                                // 여기에 네 코드 맵핑(예시)
                                literal(98), color(Color.parseColor("#E53935")),   // LAYER/COLOR=98 → 빨강
                                literal(96), color(Color.parseColor("#1E88E5")),   // 96 → 파랑
                                literal(12), color(Color.parseColor("#FB8C00")),   // 12 → 주황

                                // default
                                color(Color.parseColor("#666666"))
                            )
                            val widthExpr = coalesce(
                                toNumber(get("WIDTH")),
                                toNumber(get("BFR_WIDTH")),
                                literal(1.0f)
                            )

                            setProperties(lineColor(colorExpr), // 파란색
                                lineWidth(widthExpr)
                            )
                        }
                        style.addLayer(lineLayer)
                        // areaTiles 데이터를 표시할 레이어들 추가 (COLOR 속성 사용)
                        val areaLayer = org.maplibre.android.style.layers.FillLayer("areaTiles-areas", "areaTiles-source").apply {
                            setSourceLayer("area_map") // PMTiles의 실제 레이어명에 맞게 조정
                            setMinZoom(0f)
                            setMaxZoom(24f)
                            val colorExpr = match(
                                toNumber(coalesce(get("COLOR"), get("BFR_COLOR"), get("LAYER"))),

                                // 여기에 네 코드 맵핑(예시)
                                literal(160), color(Color.parseColor("#C3FFFD")),   // LAYER/COLOR=98 → 빨강
                                literal(11), color(Color.parseColor("#00FFFA")),   // 96 → 파랑
                                literal(62), color(Color.parseColor("#079CFF")),   // 12 → 주황
                                literal(68), color(Color.parseColor("#07E0FF")),   // 12 → 주황
                                literal(74), color(Color.parseColor("#079CFF80")),   // 12 → 주황
                                literal(131), color(Color.parseColor("#07FDFF91")),   // 12 → 주황

                                // default
                                color(Color.parseColor("#FFFFF8CA"))

                            )

                            setProperties(
                                org.maplibre.android.style.layers.PropertyFactory.fillColor(colorExpr
                                ),
                                org.maplibre.android.style.layers.PropertyFactory.fillOpacity(0.6f)
                            )

                        }
                        style.addLayer(areaLayer)

                        // areaTiles의 경계선도 표시 (COLOR 속성 사용)
                        val areaLineLayer = LineLayer("areaTiles-lines", "areaTiles-source").apply {
                            setSourceLayer("area_map")
                            setProperties(
                                lineColor(org.maplibre.android.style.expressions.Expression.get("COLOR")),
                                lineWidth(1.0f)
                            )
                        }
                        style.addLayer(areaLineLayer)

                        // depthTilesSource를 style.addSource 한 "이후"에 추가
                        val depthValueLabels = org.maplibre.android.style.layers.SymbolLayer(
                            "depth-value-labels", "depthTiles-source"
                        ).apply {
                            setSourceLayer("P_1_129_SOUNDG")
                            minZoom = 0f            // 필요 시 조정 (라벨 과밀 방지)
                            maxZoom = 24f

                            setProperties(
                                // VALUE 숫자(소수 0~1자리) + " m" 붙이기
                                PropertyFactory.textField(
                                    concat(
                                        // 소수 1자리로 표기하려면: toString(div(round(mul(toNumber(get("VALUE")), 10)), 10))
                                        toString(round(toNumber(get("VALUE")))),  // 정수로 반올림
                                        literal(" m")
                                    )
                                ),
                                PropertyFactory.textSize(
                                    interpolate(
                                        exponential(1.2f), zoom(),
                                        stop(12, 10f), stop(16, 14f), stop(20, 18f)
                                    )
                                ),
                                PropertyFactory.textColor(Color.BLACK),
                                PropertyFactory.textHaloColor(Color.WHITE),
                                PropertyFactory.textHaloWidth(1.5f),
                                PropertyFactory.textAllowOverlap(false),   // 충돌 회피 켜기(기본값)
                                PropertyFactory.textIgnorePlacement(false)
                                // 원형 마커와 같이 쓸 때 위치를 위로 조금 올리고 싶으면:
                                // PropertyFactory.textOffset(arrayOf(0f, -0.8f))
                            )

                            // 0 또는 값 없음은 숨기기(선택)
                            setFilter(all(has("VALUE"), gt(toNumber(get("VALUE")), literal(0))))
                        }

                        style.addLayer(depthValueLabels)
                        Log.d("[PMTiles]", "테스트 원형 마커 레이어 추가 완료")

                        Log.d("[PMTiles]", "PMTiles 데이터 소스 및 레이어 추가 성공!")
                        Log.d("[PMTiles]", "PMTiles 지도가 성공적으로 표시되었습니다.")
                        Log.d("[PMTiles]", "수심 데이터 레이어가 추가되었습니다.")
                    }
                } catch (e: Exception) {
                    Log.e("[PMTiles]", "PMTiles 스타일 로드 실패: ${e.message}")
                    e.printStackTrace()

                    // PMTiles 로드 실패 시 기본 스타일 사용
                    map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) {
                        Log.d("[PMTiles]", "기본 스타일 로드 완료")
                        Log.d("[PMTiles]", "PMTiles 로드에 실패하여 기본 지도를 표시합니다.")
                    }
                }

            } catch (e: Exception) {
                Log.e("[PMTiles]", "PMTiles 로드 중 에러: ${e.message}")
                e.printStackTrace()

                // 에러 발생 시 기본 스타일 사용
                map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json"))
            }
        } else {
            Log.e("[PMTiles]", "PMTiles 파일이 존재하지 않음")
            // 파일이 없으면 기본 스타일 사용
            map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json"))
        }

    } catch (e: Exception) {
        Log.e("[PMTiles]", "PMTiles 파일 복사 중 에러: ${e.message}")
        e.printStackTrace()

        // 에러 발생 시 기본 스타일 사용
        map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json"))
    }
}


@Preview(showBackground = true)
@Composable
fun ChartPlotterMapPreview() {
    ChartPlotterTheme {
        Text("Chart Plotter Map Preview")
    }
}