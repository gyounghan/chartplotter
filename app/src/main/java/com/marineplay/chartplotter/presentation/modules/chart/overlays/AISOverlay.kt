package com.marineplay.chartplotter.presentation.modules.chart.overlays

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import com.marineplay.chartplotter.domain.entities.AISVessel

/**
 * AIS 선박을 지도에 표시하는 오버레이
 * - 줌 0~5: 검은색 점으로 표시
 * - 줌 6 이상: 삼각형으로 표시 (선박 방향 표시)
 */
class AISOverlay {
    private val sourceId = "ais-vessels"
    private val circleLayerId = "ais-vessels-circle"
    private val triangleLayerId = "ais-vessels-triangle"
    private val labelLayerId = "ais-vessels-label"
    private val triangleIconId = "ais-vessel-triangle-icon"
    private val watchlistIconId = "ais-vessel-watchlist-icon"
    private val watchlistLayerId = "ais-vessels-watchlist-triangle"
    private val labelMinZoom = 12.0f
    private val triangleMinZoom = 6.0f // 줌 6 이상에서 삼각형 표시
    private val circleMaxZoom = 5.0f // 줌 0~5에서 점 표시
    
    private var geoJsonSource: GeoJsonSource? = null
    private var circleLayer: CircleLayer? = null
    private var triangleLayer: SymbolLayer? = null
    private var watchlistTriangleLayer: SymbolLayer? = null
    private var labelLayer: SymbolLayer? = null
    private var mapRef: MapLibreMap? = null
    private var styleRef: Style? = null
    
    // ✅ Feature 캐시 (MMSI -> Feature) - 최초 1회 생성 후 재사용
    private val featureCache = mutableMapOf<String, Feature>()
    
    // ✅ 이전 좌표 캐시 (MMSI -> Pair<lat, lon>) - Geometry 재생성 최소화를 위해
    private val previousCoordinates = mutableMapOf<String, Pair<Double, Double>>()
    
    // ✅ 스로틀링: 500ms에 한 번만 업데이트
    private var lastUpdateTime = 0L
    private val updateThrottleMs = 500L
    
    // ✅ Bitmap 캐시
    private var cachedTriangleBitmap: Bitmap? = null
    private var cachedWatchlistBitmap: Bitmap? = null
    
    // ✅ Path 캐시 (재사용을 위해 클래스 멤버로)
    private val trianglePath = Path()
    
    // ✅ 초기화 완료 플래그
    @Volatile
    private var isInitialized = false
    
    // ✅ 최초 FeatureCollection 설정 여부
    private var initialFeaturesSet = false
    
    /**
     * Overlay 시작 (지도 스타일이 로드된 후 호출)
     * @param initialVessels 초기 선박 데이터 (선택적)
     */
    fun start(map: MapLibreMap, initialVessels: List<AISVessel>? = null) {
        Log.d("[AISOverlay]", "🚀 start() 호출됨: initialVessels=${initialVessels?.size ?: 0}개")
        mapRef = map
        isInitialized = false // 초기화 시작
        
        map.getStyle { style ->
            try {
                Log.d("[AISOverlay]", "📋 지도 스타일 로드 완료, 스타일 객체 초기화 시작")
                ensureStyleObjects(style)
                styleRef = style
                
                // ✅ 초기화 완료 플래그 설정 (geoJsonSource가 생성된 후)
                isInitialized = geoJsonSource != null
                Log.d("[AISOverlay]", "✅ 초기화 완료: isInitialized=$isInitialized, geoJsonSource=${geoJsonSource != null}")
                
                // 줌 변화 감지 리스너 추가 (스타일 로드 후)
                map.addOnCameraMoveListener {
                    val currentZoom = map.cameraPosition.zoom.toFloat()
                    Log.d("[AISOverlay]", "줌 변화 감지: $currentZoom")
                    // 줌에 따라 삼각형 크기 업데이트
                    updateTriangleSize(style, currentZoom)
                }
                
                // 초기 줌 설정
                val initialZoom = map.cameraPosition.zoom.toFloat()
                Log.d("[AISOverlay]", "✅ AIS overlay started 성공, 현재 줌: $initialZoom, 초기화 완료: $isInitialized")
                updateTriangleSize(style, initialZoom)
                
                // ✅ 스타일 로드 완료 후 초기 선박 데이터 업데이트
                if (initialVessels != null && isInitialized) {
                    Log.d("[AISOverlay]", "📊 초기 선박 데이터 업데이트 시작: ${initialVessels.size}개")
                    updateVessels(initialVessels)
                } else {
                    Log.d("[AISOverlay]", "⚠️ 초기 선박 데이터 업데이트 스킵: initialVessels=${initialVessels != null}, isInitialized=$isInitialized")
                }
            } catch (e: Exception) {
                Log.e("[AISOverlay]", "❌ start failed: ${e.message}", e)
                isInitialized = false
            }
        }
    }
    
    /**
     * 줌에 따라 삼각형 크기 업데이트
     */
    private fun updateTriangleSize(style: Style, zoom: Float) {
        try {
            triangleLayer?.let { layer ->
                val iconSize = when {
                    zoom < 6f -> 0.0f
                    zoom < 8f -> 0.0f
                    zoom < 10f -> 0.5f
                    zoom < 12f -> 0.6f
                    zoom < 15f -> 0.9f
                    else -> 1.2f
                }
                layer.setProperties(PropertyFactory.iconSize(iconSize))
                Log.d("[AISOverlay]", "삼각형 크기 업데이트: 줌=$zoom, iconSize=$iconSize")
            }
        } catch (e: Exception) {
            Log.e("[AISOverlay]", "삼각형 크기 업데이트 실패: ${e.message}", e)
        }
    }
    
    /**
     * Overlay 중지
     */
    fun stop() {
        mapRef?.getStyle { style ->
            try {
                geoJsonSource?.let { style.removeSource(it) }
                circleLayer?.let { style.removeLayer(it) }
                watchlistTriangleLayer?.let { style.removeLayer(it) }
                triangleLayer?.let { style.removeLayer(it) }
                labelLayer?.let { style.removeLayer(it) }
            } catch (e: Exception) {
                Log.e("[AISOverlay]", "stop failed: ${e.message}", e)
            }
        }
        
        geoJsonSource = null
        circleLayer = null
        triangleLayer = null
        watchlistTriangleLayer = null
        labelLayer = null
        mapRef = null
        styleRef = null
        featureCache.clear()
        previousCoordinates.clear()
        initialFeaturesSet = false
        isInitialized = false // ✅ 초기화 플래그 리셋
    }
    
    /**
     * AIS 선박 데이터 업데이트 (변경된 선박만 감지하여 효율적으로 업데이트)
     */
    fun updateVessels(vessels: List<AISVessel>) {
        try {
            Log.d("[AISOverlay]", "updateVessels 호출: 총 ${vessels.size}개 선박")
            
            // ✅ 초기화가 완료되지 않았으면 스킵
            if (!isInitialized || geoJsonSource == null) {
                Log.d("[AISOverlay]", "AIS 소스가 아직 초기화되지 않았습니다. 초기화 대기 중... (isInitialized=$isInitialized, geoJsonSource=${geoJsonSource != null})")
                return
            }
            
            val now = System.currentTimeMillis()
            
            // ✅ 1. 스로틀링 체크: 500ms에 한 번만 업데이트
            if (now - lastUpdateTime < updateThrottleMs) {
                Log.d("[AISOverlay]", "스로틀링: ${now - lastUpdateTime}ms 경과 (${updateThrottleMs}ms 미만)")
                return
            }
            
            val map = mapRef ?: return
            val zoom = map.cameraPosition.zoom.toFloat()
            
            Log.d("[AISOverlay]", "현재 줌: $zoom, 최소 줌: $triangleMinZoom")
            
            // ✅ 2. 줌 낮으면 스킵 (하지만 로그는 남김)
            if (zoom < triangleMinZoom) {
                Log.d("[AISOverlay]", "줌이 낮아서 스킵: $zoom < $triangleMinZoom")
                return
            }
            
            // ✅ 3. 화면 범위 계산
            val (latSouth, latNorth, lonWest, lonEast) = try {
                val vr = map.projection.visibleRegion
                val corners = listOf(vr.farLeft, vr.farRight, vr.nearLeft, vr.nearRight).filterNotNull()
                if (corners.isEmpty()) {
                    Log.w("[AISOverlay]", "화면 범위 계산 실패: corners가 비어있음")
                    return
                }
                val minLat = corners.minOf { it.latitude }
                val maxLat = corners.maxOf { it.latitude }
                val minLon = corners.minOf { it.longitude }
                val maxLon = corners.maxOf { it.longitude }
                Log.d("[AISOverlay]", "화면 범위: lat[$minLat ~ $maxLat], lon[$minLon ~ $maxLon]")
                Bounds(minLat, maxLat, minLon, maxLon)
            } catch (e: Exception) {
                Log.e("[AISOverlay]", "화면 범위 계산 실패: ${e.message}", e)
                return
            }
            
            // 유효한 좌표를 가진 선박만 필터링
            val validCoordinates = vessels.filter { vessel ->
                vessel.latitude != null && 
                vessel.longitude != null &&
                vessel.latitude!! >= -90.0 && vessel.latitude!! <= 90.0 &&
                vessel.longitude!! >= -180.0 && vessel.longitude!! <= 180.0
            }
            Log.d("[AISOverlay]", "유효한 좌표를 가진 선박: ${validCoordinates.size}개")
            
            // ✅ 4. 화면 범위 안에 있는 선박만 (하지만 일단 모든 선박 표시하도록 주석 처리)
            val validVessels = validCoordinates.filter { vessel ->
                val lat = vessel.latitude!!
                val lon = vessel.longitude!!
                // 화면 범위 체크는 일단 비활성화 (모든 선박 표시)
                // vessel.latitude!! >= latSouth && vessel.latitude!! <= latNorth &&
                // vessel.longitude!! >= lonWest && vessel.longitude!! <= lonEast
                true // 일단 모든 선박 표시
            }
            Log.d("[AISOverlay]", "화면 범위 내 선박: ${validVessels.size}개 (전체: ${validCoordinates.size}개)")
            
            // 현재 선박들의 MMSI 집합
            val currentMmsis = validVessels.map { it.mmsi }.toSet()
            
            // ✅ 변경된 선박만 감지하여 Feature 업데이트 (Geometry 재생성 최소화)
            var hasChanges = false
            var changedCount = 0
            var newCount = 0
            var reusedCount = 0
            
            validVessels.forEach { vessel ->
                val lat = vessel.displayLatitude ?: vessel.latitude
                val lon = vessel.displayLongitude ?: vessel.longitude
                
                if (lat != null && lon != null) {
                    val mmsi = vessel.mmsi
                    val previousCoords = previousCoordinates[mmsi]
                    
                    // ✅ 좌표 변경 확인 (더 정확한 비교로 불필요한 재생성 방지)
                    val coordinatesChanged = if (previousCoords != null) {
                        val (prevLat, prevLon) = previousCoords
                        val latDiff = kotlin.math.abs(prevLat - lat)
                        val lonDiff = kotlin.math.abs(prevLon - lon)
                        latDiff > 0.00001 || lonDiff > 0.00001
                    } else {
                        true // 새 선박
                    }
                    // ✅ 즐겨찾기 변경 확인 (아이콘 색상 업데이트 필요)
                    val cachedFeature = featureCache[mmsi]
                    val cachedWatchlist = cachedFeature?.getNumberProperty("isWatchlisted")?.toInt() ?: 0
                    val newWatchlist = if (vessel.isWatchlisted) 1 else 0
                    val watchlistChanged = cachedWatchlist != newWatchlist
                    
                    if (coordinatesChanged || watchlistChanged) {
                        // ✅ 변경된 선박만 새 Feature 생성 (Geometry 재생성)
                        val geometry = Point.fromLngLat(lon, lat)
                        val newFeature = Feature.fromGeometry(geometry).apply {
                            addStringProperty("id", vessel.id)
                            addStringProperty("name", vessel.name)
                            addStringProperty("mmsi", mmsi) // MMSI를 id로 사용 (Feature-State 대신 property로)
                            addNumberProperty("course", vessel.course.toDouble())
                            addNumberProperty("isWatchlisted", if (vessel.isWatchlisted) 1 else 0)
                        }
                        featureCache[mmsi] = newFeature
                        previousCoordinates[mmsi] = Pair(lat, lon)
                        hasChanges = true
                        if (previousCoords == null) {
                            newCount++
                        } else {
                            changedCount++
                        }
                    } else {
                        // ✅ 변경 없으면 기존 Feature 재사용 (Geometry 재생성 안 함)
                        // course만 업데이트해야 하는 경우도 있지만, Geometry 재생성은 피함
                        reusedCount++
                    }
                }
            }
            
            // ✅ 제거된 선박 처리
            val removedMmsis = featureCache.keys - currentMmsis
            val removedCount = removedMmsis.size
            if (removedMmsis.isNotEmpty()) {
                removedMmsis.forEach { mmsi ->
                    featureCache.remove(mmsi)
                    previousCoordinates.remove(mmsi)
                }
                hasChanges = true
            }
            
            // ✅ 변경사항이 있거나 첫 업데이트인 경우에만 지도 업데이트
            if (hasChanges || !initialFeaturesSet) {
                val finalFeatures = featureCache.values.toList()
                
                Log.d("[AISOverlay]", "지도 업데이트 준비: ${finalFeatures.size}개 Feature (신규: ${newCount}개, 변경: ${changedCount}개, 재사용: ${reusedCount}개, 제거: ${removedCount}개)")
                
                if (finalFeatures.isNotEmpty()) {
                    geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(finalFeatures))
                    lastUpdateTime = now
                    initialFeaturesSet = true
                    Log.d("[AISOverlay]", "✅ AIS 선박 지도 업데이트 완료: 총 ${finalFeatures.size}개 (신규: ${newCount}개, 변경: ${changedCount}개, 재사용: ${reusedCount}개, 제거: ${removedCount}개)")
                } else {
                    geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
                    lastUpdateTime = now
                    initialFeaturesSet = false
                    Log.d("[AISOverlay]", "AIS 선박 모두 제거됨")
                }
            } else {
                // ✅ 변경사항 없음 (로그만 출력, 지도 업데이트 스킵)
                Log.d("[AISOverlay]", "AIS 선박 변경사항 없음 (${validVessels.size}개 유지, 재사용: ${reusedCount}개, 지도 업데이트 스킵)")
            }
        } catch (e: Exception) {
            Log.e("[AISOverlay]", "AIS 선박 업데이트 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 스타일 객체 초기화 (소스, 레이어, 아이콘)
     */
    private fun ensureStyleObjects(style: Style) {
        Log.d("[AISOverlay]", "ensureStyleObjects 시작")
        
        // GeoJsonSource 생성
        if (geoJsonSource == null) {
            geoJsonSource = GeoJsonSource(sourceId).also { 
                style.addSource(it)
                Log.d("[AISOverlay]", "AIS 소스 생성 완료: $sourceId")
            }
        } else {
            Log.d("[AISOverlay]", "AIS 소스 이미 존재함: $sourceId")
        }
        
        // 삼각형 아이콘 생성 및 등록 (캐시 사용)
        if (style.getImage(triangleIconId) == null) {
            if (cachedTriangleBitmap == null) {
                cachedTriangleBitmap = createTriangleIcon(64, Color.rgb(0, 180, 0), Color.rgb(0, 80, 0))
            }
            style.addImage(triangleIconId, cachedTriangleBitmap!!)
            Log.d("[AISOverlay]", "삼각형 아이콘 생성 완료: $triangleIconId (캐시 사용)")
        }
        // 즐겨찾기 선박용 아이콘 (금색/주황색)
        if (style.getImage(watchlistIconId) == null) {
            if (cachedWatchlistBitmap == null) {
                cachedWatchlistBitmap = createTriangleIcon(64, Color.rgb(255, 180, 0), Color.rgb(200, 120, 0))
            }
            style.addImage(watchlistIconId, cachedWatchlistBitmap!!)
            Log.d("[AISOverlay]", "즐겨찾기 아이콘 생성 완료: $watchlistIconId")
        }

        // 줌 6 이상: 일반 선박 삼각형 (초록색)
        if (triangleLayer == null) {
            Log.d("[AISOverlay]", "TriangleLayer 생성 시작")
            triangleLayer = SymbolLayer(triangleLayerId, sourceId).apply {
                setProperties(
                    PropertyFactory.iconImage(triangleIconId),
                    PropertyFactory.iconSize(
                        interpolate(
                            exponential(0.0f),
                            zoom(),
                            stop(6f, 0.0f),
                            stop(8f, 0.0f),
                            stop(10f, 0.5f),
                            stop(12f, 0.6f),
                            stop(15f, 0.9f),
                            stop(18f, 1.2f)
                        )
                    ),
                    PropertyFactory.iconRotate(get("course")),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
                )
                setMinZoom(triangleMinZoom)
                // isWatchlisted가 1이 아닌 선박만 (0이거나 없으면 표시)
                setFilter(neq(coalesce(get("isWatchlisted"), literal(0)), literal(1)))
            }
            style.addLayer(triangleLayer!!)
            Log.d("[AISOverlay]", "TriangleLayer 추가 완료: $triangleLayerId")
        }
        // 즐겨찾기 선박 삼각형 (금색) - 위 레이어 위에 표시
        if (watchlistTriangleLayer == null) {
            watchlistTriangleLayer = SymbolLayer(watchlistLayerId, sourceId).apply {
                setProperties(
                    PropertyFactory.iconImage(watchlistIconId),
                    PropertyFactory.iconSize(
                        interpolate(
                            exponential(0.0f),
                            zoom(),
                            stop(6f, 0.0f),
                            stop(8f, 0.0f),
                            stop(10f, 0.5f),
                            stop(12f, 0.6f),
                            stop(15f, 0.9f),
                            stop(18f, 1.2f)
                        )
                    ),
                    PropertyFactory.iconRotate(get("course")),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
                )
                setMinZoom(triangleMinZoom)
                setFilter(eq(get("isWatchlisted"), literal(1)))
            }
            style.addLayer(watchlistTriangleLayer!!)
            Log.d("[AISOverlay]", "WatchlistTriangleLayer 추가 완료: $watchlistLayerId")
        }

        // 확대 시 MMSI 라벨 표시
        // if (labelLayer == null) {
        //     labelLayer = SymbolLayer(labelLayerId, sourceId).apply {
        //         setProperties(
        //             PropertyFactory.textField(get("mmsi")),
        //             PropertyFactory.textSize(
        //                 interpolate(
        //                     exponential(1.2f),
        //                     zoom(),
        //                     stop(12f, 10f),
        //                     stop(15f, 12f),
        //                     stop(18f, 14f)
        //                 )
        //             ),
        //             PropertyFactory.textColor(Color.WHITE),
        //             PropertyFactory.textHaloColor(Color.BLACK),
        //             PropertyFactory.textHaloWidth(1.5f),
        //             PropertyFactory.textOffset(arrayOf(0f, 1.2f)),
        //             PropertyFactory.textAllowOverlap(true),
        //             PropertyFactory.textIgnorePlacement(true),
        //             PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP)
        //         )
        //         setMinZoom(labelMinZoom)
        //     }
        //     style.addLayer(labelLayer!!)
        //     Log.d("[AISOverlay]", "LabelLayer 추가 완료: $labelLayerId (minZoom=$labelMinZoom)")
        // } else {
        //     Log.d("[AISOverlay]", "LabelLayer 이미 존재함: $labelLayerId")
        // }
        
        Log.d("[AISOverlay]", "AIS 선박 레이어가 추가되었습니다. (Circle: $circleLayerId, Triangle: $triangleLayerId, Label: $labelLayerId)")
    }
    
    // ✅ 화면 범위 데이터 클래스
    private data class Bounds(
        val latSouth: Double,
        val latNorth: Double,
        val lonWest: Double,
        val lonEast: Double
    )
    
    /**
     * 삼각형 아이콘 생성 (북쪽을 향하는 이등변 삼각형)
     * @param fillColor 채움 색상 (기본: 초록색)
     * @param strokeColor 테두리 색상
     */
    private fun createTriangleIcon(
        sizePx: Int,
        fillColor: Int = Color.rgb(0, 180, 0),
        strokeColor: Int = Color.rgb(0, 80, 0)
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.04f
        }
        
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val height = sizePx * 0.5f
        val topY = centerY - height * (2f / 3f)
        val bottomY = centerY + height * (1f / 3f)
        
        val baseWidth = sizePx * 0.35f
        val leftX = centerX - baseWidth / 2f
        val rightX = centerX + baseWidth / 2f
        
        // ✅ Path 재사용
        trianglePath.reset()
        trianglePath.moveTo(centerX, topY) // 위쪽 꼭짓점 (북쪽)
        trianglePath.lineTo(leftX, bottomY) // 왼쪽 아래
        trianglePath.lineTo(rightX, bottomY) // 오른쪽 아래
        trianglePath.close()
        
        // 채움 먼저, 테두리 나중에
        canvas.drawPath(trianglePath, fillPaint)
        canvas.drawPath(trianglePath, strokePaint)
        
        return bitmap
    }
}


