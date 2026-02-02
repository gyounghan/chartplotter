package com.marineplay.chartplotter.presentation.modules.chart.overlays

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.util.Calendar

/**
 * assets의 1분 간격 조류 CSV를 읽어서 지도에 "화살표(방향)" + "유속(텍스트)"로 표시하는 오버레이.
 *
 * - direction_deg: 흐르는 방향(to) 기준 각도 (0=북, 90=동)
 * - speed_knots: knots
 */
class TidalCurrentOverlay(
    private val context: Context,
    private val scope: CoroutineScope,
    private val assetCsvPath: String,
    private val defaultCenter: LatLng
) {
    private val sourceId = "tidal_current_source"
    private val arrowLayerId = "tidal_current_arrow_layer"
    private val labelLayerId = "tidal_current_label_layer"
    private val imageId = "tidal-current-arrow"

    private var geoJsonSource: GeoJsonSource? = null
    private var job: Job? = null
    private var cameraListenerRegistered = false
    private var mapRef: MapLibreMap? = null

    // ✅ 생성자에서 로딩하지 않고 lazy로 변경 (백그라운드 로딩)
    private val pointsByMinute: List<List<PointRow>> by lazy {
        loadCsvPoints(assetCsvPath)
    }
    private var csvLoaded = false
    private val speedFormat = DecimalFormat("0.0")

    data class PointRow(
        val lat: Double,
        val lon: Double,
        val speedKnots: Double,
        val directionDeg: Double,
        val minZoom: Double,
        val maxZoom: Double
    )

    fun start(map: MapLibreMap) {
        // 기존 작업이 있으면 취소 (재시작 보장)
        job?.cancel()
        job = null
        mapRef = map

        map.getStyle { style ->
            try {
                // ✅ 파일 존재 여부 먼저 체크
                if (!checkCsvExists(assetCsvPath)) {
                    Log.w("[TidalCurrentOverlay]", "CSV 파일이 없습니다: $assetCsvPath - Overlay 시작 안 함")
                    return@getStyle
                }
                
                ensureStyleObjects(style)
                ensureCameraListener(map)
                
                // ✅ CSV 로딩을 백그라운드로
                if (!csvLoaded) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            // 백그라운드에서 로딩
                            pointsByMinute.size // lazy 초기화
                            csvLoaded = true
                            
                            withContext(Dispatchers.Main) {
                                updateForView(map)
                                // 이후 1분마다 갱신
                                job = scope.launch {
                                    while (isActive) {
                                        delay(60_000)
                                        mapRef?.let { updateForView(it) }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("[TidalCurrentOverlay]", "CSV 로딩 실패: ${e.message}", e)
                        }
                    }
                } else {
                    updateForView(map)
                    job = scope.launch {
                        while (isActive) {
                            delay(60_000)
                            mapRef?.let { updateForView(it) }
                        }
                    }
                }
                
                Log.d("[TidalCurrentOverlay]", "started (asset=$assetCsvPath)")
            } catch (e: Exception) {
                Log.e("[TidalCurrentOverlay]", "start failed: ${e.message}", e)
            }
        }
    }
    
    // ✅ 파일 존재 여부 체크
    private fun checkCsvExists(path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        mapRef?.removeOnCameraIdleListener(cameraIdleListener)
        cameraListenerRegistered = false
        mapRef = null
    }

    private val cameraIdleListener = MapLibreMap.OnCameraIdleListener {
        mapRef?.let { updateForView(it) }
    }

    private fun ensureCameraListener(map: MapLibreMap) {
        if (cameraListenerRegistered) return
        map.addOnCameraIdleListener(cameraIdleListener)
        cameraListenerRegistered = true
    }

    private fun ensureStyleObjects(style: Style) {
        // 이미지 1회 등록
        if (style.getImage(imageId) == null) {
            style.addImage(imageId, createArrowBitmap(sizePx = 64))
        }

        // 소스 1회 등록
        val existing = style.getSource(sourceId)
        geoJsonSource = if (existing is GeoJsonSource) {
            existing
        } else {
            GeoJsonSource(sourceId).also { style.addSource(it) }
        }

        // 레이어 1회 등록 (화살표)
        if (style.getLayer(arrowLayerId) == null) {
            val arrowLayer = SymbolLayer(arrowLayerId, sourceId).apply {
                setProperties(
                    PropertyFactory.iconImage(imageId),
                    PropertyFactory.iconSize(0.8f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    PropertyFactory.iconRotate(org.maplibre.android.style.expressions.Expression.get("direction")),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)
                )
            }
            style.addLayer(arrowLayer)
        }

        // 레이어 1회 등록 (유속 텍스트)
        if (style.getLayer(labelLayerId) == null) {
            val labelLayer = SymbolLayer(labelLayerId, sourceId).apply {
                setProperties(
                    PropertyFactory.textField(org.maplibre.android.style.expressions.Expression.get("speedLabel")),
                    PropertyFactory.textSize(14f),
                    PropertyFactory.textColor(android.graphics.Color.WHITE),
                    PropertyFactory.textHaloColor(android.graphics.Color.BLACK),
                    PropertyFactory.textHaloWidth(2f),
                    PropertyFactory.textOffset(arrayOf(0f, 1.6f)),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.textIgnorePlacement(true)
                )
            }
            // ✅ 숫자 텍스트는 줌이 충분히 올라왔을 때만 보이게
            labelLayer.setMinZoom(12.0f)
            style.addLayer(labelLayer)
        }
    }

    /**
     * ✅ 현재 카메라의 visible bounds 안에서만 조류를 생성/표시.
     * ✅ 줌 레벨에 따라 샘플링 간격을 조절해서 "줌 낮으면 대표 몇 개, 줌 높으면 촘촘히" 구현.
     */
    private fun updateForView(map: MapLibreMap) {
        // ✅ CSV 로딩 안 됐으면 스킵
        if (!csvLoaded) return
        
        val minute = nowMinuteOfDay()
        val points = pointsByMinute.getOrNull(minute) ?: return

        val zoom = map.cameraPosition.zoom.toFloat()
        // ✅ LatLngBounds 필드명 차이(MapLibre/Mapbox 버전 차이)로 인한 컴파일 이슈 방지:
        // visibleRegion의 4 코너로 직접 min/max bounds를 계산한다.
        val (latSouth, latNorth, lonWest, lonEast) = try {
            val vr = map.projection.visibleRegion
            val corners = listOf(vr.farLeft, vr.farRight, vr.nearLeft, vr.nearRight).filterNotNull()
            if (corners.isEmpty()) {
                Bounds(
                    defaultCenter.latitude - 0.05,
                    defaultCenter.latitude + 0.05,
                    defaultCenter.longitude - 0.05,
                    defaultCenter.longitude + 0.05
                )
            } else {
                val minLat = corners.minOf { it.latitude }
                val maxLat = corners.maxOf { it.latitude }
                val minLon = corners.minOf { it.longitude }
                val maxLon = corners.maxOf { it.longitude }
                Bounds(minLat, maxLat, minLon, maxLon)
            }
        } catch (e: Exception) {
            Bounds(
                defaultCenter.latitude - 0.05,
                defaultCenter.latitude + 0.05,
                defaultCenter.longitude - 0.05,
                defaultCenter.longitude + 0.05
            )
        }

        val maxFeatures = 3000
        val features = ArrayList<Feature>(minOf(maxFeatures, points.size))

        for (p in points) {
            // ✅ 각 데이터마다 low/high 줌(min/max) 범위를 적용
            if (zoom.toDouble() < p.minZoom || zoom.toDouble() >= p.maxZoom) continue
            // ✅ 현재 화면 bounds 안만 표시
            if (p.lat < latSouth || p.lat > latNorth) continue
            if (p.lon < lonWest || p.lon > lonEast) continue

            val f = Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).also {
                it.addNumberProperty("direction", p.directionDeg)
                it.addStringProperty("speedLabel", "${speedFormat.format(p.speedKnots)}kt")
            }
            features.add(f)
            if (features.size >= maxFeatures) break
        }

        geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(features))
        Log.d(
            "[TidalCurrentOverlay]",
            "update zoom=$zoom minute=$minute features=${features.size}"
        )
    }

    private data class Bounds(
        val latSouth: Double,
        val latNorth: Double,
        val lonWest: Double,
        val lonEast: Double
    )

    private fun nowMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return h * 60 + m
    }

    private fun loadCsvPoints(path: String): List<List<PointRow>> {
        val byMinute = Array(24 * 60) { mutableListOf<PointRow>() }
        try {
            context.assets.open(path).use { input ->
                BufferedReader(InputStreamReader(input)).use { br ->
                    // header skip
                    val header = br.readLine()
                    if (header == null) return emptyList()

                    while (true) {
                        val line = br.readLine() ?: break
                        val parts = line.split(',')
                        if (parts.size < 5) continue

                        // timestamp format: 2026-01-20T00:00:00
                        val ts = parts[0]
                        val hh = ts.substring(11, 13).toIntOrNull() ?: continue
                        val mm = ts.substring(14, 16).toIntOrNull() ?: continue
                        val minuteOfDay = hh * 60 + mm

                        val lat = parts.getOrNull(1)?.toDoubleOrNull() ?: continue
                        val lon = parts.getOrNull(2)?.toDoubleOrNull() ?: continue
                        val speed = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                        val dir = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                        val minZoom = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                        val maxZoom = parts.getOrNull(6)?.toDoubleOrNull() ?: 24.0

                        if (minuteOfDay in 0..1439) {
                            byMinute[minuteOfDay].add(
                                PointRow(
                                    lat = lat,
                                    lon = lon,
                                    speedKnots = speed,
                                    directionDeg = dir,
                                    minZoom = minZoom,
                                    maxZoom = maxZoom
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("[TidalCurrentOverlay]", "CSV load failed: $path (${e.message})", e)
        }

        return byMinute.map { it.toList() }
    }

    /**
     * 위(북쪽)를 향하는 단순 화살표(삼각형+꼬리) 비트맵 생성.
     * 아이콘 회전은 MapLibre SymbolLayer의 iconRotate로 처리.
     */
    private fun createArrowBitmap(sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(230, 0, 140, 255) // 살짝 투명한 파랑
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = (sizePx * 0.06f)
        }

        val cx = sizePx / 2f
        val top = sizePx * 0.12f
        val bottom = sizePx * 0.88f

        // 화살촉(삼각형)
        val headW = sizePx * 0.42f
        val headH = sizePx * 0.38f
        val head = Path().apply {
            moveTo(cx, top)
            lineTo(cx - headW / 2f, top + headH)
            lineTo(cx + headW / 2f, top + headH)
            close()
        }

        // 몸통(짧은 사각 꼬리)
        val tailW = sizePx * 0.16f
        val tailTop = top + headH
        val tail = RectF(cx - tailW / 2f, tailTop, cx + tailW / 2f, bottom)

        canvas.drawPath(head, paint)
        canvas.drawRect(tail, paint)
        canvas.drawPath(head, stroke)
        canvas.drawRect(tail, stroke)

        return bmp
    }
}

