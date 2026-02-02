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
    private val labelMinZoom = 12.0f
    private val triangleMinZoom = 6.0f // 줌 6 이상에서 삼각형 표시
    private val circleMaxZoom = 5.0f // 줌 0~5에서 점 표시
    
    private var geoJsonSource: GeoJsonSource? = null
    private var circleLayer: CircleLayer? = null
    private var triangleLayer: SymbolLayer? = null
    private var labelLayer: SymbolLayer? = null
    private var mapRef: MapLibreMap? = null
    private var styleRef: Style? = null
    
    // 이전 Feature 캐시 (MMSI -> Feature)
    private val previousFeatures = mutableMapOf<String, Feature>()
    
    // ✅ 스로틀링: 500ms에 한 번만 업데이트
    private var lastUpdateTime = 0L
    private val updateThrottleMs = 500L
    
    // ✅ Bitmap 캐시
    private var cachedTriangleBitmap: Bitmap? = null
    
    // ✅ 초기화 완료 플래그
    @Volatile
    private var isInitialized = false
    
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
                triangleLayer?.let { style.removeLayer(it) }
                labelLayer?.let { style.removeLayer(it) }
            } catch (e: Exception) {
                Log.e("[AISOverlay]", "stop failed: ${e.message}", e)
            }
        }
        
        geoJsonSource = null
        circleLayer = null
        triangleLayer = null
        labelLayer = null
        mapRef = null
        styleRef = null
        previousFeatures.clear()
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
            
            // 변경된 선박만 감지
            val updatedFeatures = mutableMapOf<String, Feature>()
            var hasChanges = false
            var changedCount = 0
            
            validVessels.forEach { vessel ->
                val lat = vessel.displayLatitude ?: vessel.latitude
                val lon = vessel.displayLongitude ?: vessel.longitude
                
                if (lat != null && lon != null) {
                    val previousFeature = previousFeatures[vessel.mmsi]
                    
                    // 좌표가 변경되었는지 확인
                    val coordinatesChanged = if (previousFeature != null) {
                        val prevPoint = previousFeature.geometry() as? Point
                        if (prevPoint != null) {
                            val coordinates = prevPoint.coordinates()
                            // Point.coordinates()는 [longitude, latitude] 리스트 반환
                            if (coordinates.size >= 2) {
                                val prevLon = coordinates[0]
                                val prevLat = coordinates[1]
                                // 소수점 6자리로 반올림하여 비교 (미세한 변화 무시)
                                val lonDiff = kotlin.math.abs(prevLon - lon)
                                val latDiff = kotlin.math.abs(prevLat - lat)
                                lonDiff > 0.000001 || latDiff > 0.000001
                            } else {
                                true
                            }
                        } else {
                            true
                        }
                    } else {
                        true // 새 선박
                    }
                    
                    if (coordinatesChanged) {
                        // 변경된 선박만 새 Feature 생성
                        val geometry = Point.fromLngLat(lon, lat)
                        val newFeature = Feature.fromGeometry(geometry).apply {
                            addStringProperty("id", vessel.id)
                            addStringProperty("name", vessel.name)
                            addStringProperty("mmsi", vessel.mmsi)
                            // 삼각형 회전을 위한 course 추가
                            addNumberProperty("course", vessel.course.toDouble())
                        }
                        updatedFeatures[vessel.mmsi] = newFeature
                        hasChanges = true
                        changedCount++
                    } else {
                        // 변경 없으면 이전 Feature 재사용
                        previousFeature?.let { updatedFeatures[vessel.mmsi] = it }
                    }
                }
            }
            
            // 제거된 선박 처리
            val removedMmsis = previousFeatures.keys - currentMmsis
            val removedCount = removedMmsis.size
            if (removedMmsis.isNotEmpty()) {
                removedMmsis.forEach { mmsi ->
                    previousFeatures.remove(mmsi)
                }
                hasChanges = true
            }
            
            // 변경사항이 있거나 첫 업데이트인 경우에만 지도 업데이트
            if (hasChanges || previousFeatures.isEmpty()) {
                val finalFeatures = updatedFeatures.values.toList()
                
                Log.d("[AISOverlay]", "지도 업데이트 준비: ${finalFeatures.size}개 Feature (변경: ${changedCount}개, 제거: ${removedCount}개, hasChanges: $hasChanges)")
                
                if (finalFeatures.isNotEmpty()) {
                    geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(finalFeatures))
                    lastUpdateTime = now // ✅ 업데이트 시간 기록
                    Log.d("[AISOverlay]", "✅ AIS 선박 지도 업데이트 완료: 총 ${finalFeatures.size}개 (변경: ${changedCount}개, 제거: ${removedCount}개)")
                } else {
                    geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
                    lastUpdateTime = now
                    Log.d("[AISOverlay]", "AIS 선박 모두 제거됨")
                }
                
                // 캐시 업데이트
                previousFeatures.clear()
                previousFeatures.putAll(updatedFeatures)
            } else {
                // 변경사항 없음 (로그만 출력, 지도 업데이트 스킵)
                Log.d("[AISOverlay]", "AIS 선박 변경사항 없음 (${validVessels.size}개 유지, 지도 업데이트 스킵)")
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
                cachedTriangleBitmap = createTriangleIcon(64)
            }
            style.addImage(triangleIconId, cachedTriangleBitmap!!)
            Log.d("[AISOverlay]", "삼각형 아이콘 생성 완료: $triangleIconId (캐시 사용)")
        }
        

        
        // 줌 6 이상: 삼각형으로 표시
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
                    ).also {
                        Log.d("[AISOverlay]", "iconSize Expression 설정 완료: 줌 6=0.6, 8=0.8, 10=1.0, 12=1.3, 15=1.8, 18=2.5")
                    },
                    PropertyFactory.iconRotate(get("course")), // 선박 방향으로 회전
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
                )
                setMinZoom(triangleMinZoom) // 줌 6 이상에서만 표시
            }
            style.addLayer(triangleLayer!!)
            Log.d("[AISOverlay]", "TriangleLayer 추가 완료: $triangleLayerId (minZoom=$triangleMinZoom)")
        } else {
            Log.d("[AISOverlay]", "TriangleLayer 이미 존재함: $triangleLayerId")
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
     * 삼각형 아이콘 생성 (북쪽을 향하는 이등변 삼각형, 밑변이 짧음)
     * 속은 비어있고 갈색 테두리만 표시
     */
    private fun createTriangleIcon(sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 갈색 테두리만 사용 (속은 비어있음)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(157, 108, 72) // 갈색 (Brown)
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.05f // 테두리 두께
        }
        
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val height = sizePx * 0.5f // 삼각형 높이 (줄임)
        // 이등변 삼각형의 무게중심이 비트맵 중심과 일치하도록 조정
        // 무게중심은 높이의 1/3 지점(위에서부터)이므로, centerY가 무게중심이 되도록 설정
        val topY = centerY - height * (2f / 3f) // 위쪽 꼭짓점 Y 좌표
        val bottomY = centerY + height * (1f / 3f) // 아래쪽 밑변 Y 좌표
        
        // 밑변이 짧은 이등변 삼각형 (밑변 너비를 좁게 설정)
        val baseWidth = sizePx * 0.35f // 밑변 너비 (더 줄임)
        val leftX = centerX - baseWidth / 2f
        val rightX = centerX + baseWidth / 2f
        
        // 북쪽을 향하는 삼각형 경로 생성
        val path = Path().apply {
            moveTo(centerX, topY) // 위쪽 꼭짓점 (북쪽)
            lineTo(leftX, bottomY) // 왼쪽 아래
            lineTo(rightX, bottomY) // 오른쪽 아래
            close()
        }
        
        // 테두리만 그리기 (속은 비어있음)
        canvas.drawPath(path, strokePaint)
        
        return bitmap
    }
}


