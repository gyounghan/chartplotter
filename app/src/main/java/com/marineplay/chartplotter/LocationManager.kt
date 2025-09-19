package com.marineplay.chartplotter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationListener
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.MapLibreMap.OnMapClickListener
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import androidx.compose.ui.graphics.Color as ComposeColor

// ⚠️ 안드로이드 시스템 LocationManager를 별칭으로 import (현재 클래스명과 충돌 방지)
import android.location.LocationManager as SysLocationManager

/**
 * GPS/네트워크 위치 추적 및 선박 아이콘 관리
 */
class LocationManager(
    private val context: Context,
    private val map: MapLibreMap
) : LocationListener {

    // 시스템 LocationManager
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as SysLocationManager

    private var currentLocation: Location? = null
    private var shipSource: GeoJsonSource? = null
    private var shipLayer: SymbolLayer? = null
    private var isAutoTracking = false // 자동 추적 상태
    private var savedZoomLevel: Double? = null // 사용자가 설정한 줌 레벨 저장
    
    // 포인트 마커 관련
    private var pointsSource: GeoJsonSource? = null
    private var pointsLayer: SymbolLayer? = null
    private var onPointClickListener: ((SavedPoint) -> Unit)? = null

    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10 m
        private const val MIN_TIME_BETWEEN_UPDATES = 1000L      // 1 s
        private const val SHIP_SOURCE_ID = "ship-location"
        private const val SHIP_LAYER_ID = "ship-symbol"
        private const val POINTS_SOURCE_ID = "saved-points"
        private const val POINTS_LAYER_ID = "points-symbol"
    }

    /** 위치 권한 확인: FINE 또는 COARSE 둘 중 하나만 있어도 OK */
    fun hasLocationPermission(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /** 사용 가능한(존재 + 활성) 프로바이더 선택 (GPS → NETWORK → PASSIVE 우선순위) */
    private fun pickProvider(): String? {
        val enabled = try { lm.getProviders(true) } catch (_: Exception) { emptyList<String>() }
        return when {
            enabled.contains(SysLocationManager.GPS_PROVIDER) -> SysLocationManager.GPS_PROVIDER
            enabled.contains(SysLocationManager.NETWORK_PROVIDER) -> SysLocationManager.NETWORK_PROVIDER
            enabled.contains(SysLocationManager.PASSIVE_PROVIDER) -> SysLocationManager.PASSIVE_PROVIDER
            else -> null
        }
    }

    /** 마지막 알려진 위치 중 가장 그럴듯한 것 하나 고르기 */
    private fun bestLastKnownLocation(): Location? {
        val candidates = listOf(
            SysLocationManager.GPS_PROVIDER,
            SysLocationManager.NETWORK_PROVIDER,
            SysLocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (p in candidates) {
            val loc = try { lm.getLastKnownLocation(p) } catch (_: Exception) { null }
            if (loc != null && (best == null || loc.time > best!!.time)) best = loc
        }
        return best
    }

    /** 위치 추적 시작 */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w("[LocationManager]", "위치 권한이 없습니다. 권한 요청 필요.")
            return
        }

        val provider = pickProvider()
        if (provider == null) {
            Log.e("[LocationManager]", "사용 가능한 위치 프로바이더가 없습니다. 설정에서 위치를 켜주세요.")
            try {
                context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) { }
            return
        }

        try {
            // 크래시 방지: 존재/활성 확인된 provider만 요청 + 메인 루퍼 지정
            lm.requestLocationUpdates(
                provider,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                this,
                Looper.getMainLooper()
            )
            // UX: 시작 즉시 마지막 위치로 한번 업데이트/카메라 이동
            bestLastKnownLocation()?.let { onLocationChanged(it) }
            Log.d("[LocationManager]", "위치 추적 시작 (provider=$provider)")
        } catch (e: IllegalArgumentException) {
            Log.e("[LocationManager]", "프로바이더 오류: $provider", e)
        } catch (e: SecurityException) {
            Log.e("[LocationManager]", "권한 오류: ${e.message}")
        }
    }

    /** 위치 추적 중지 */
    fun stopLocationUpdates() {
        try { lm.removeUpdates(this) } catch (_: Exception) {}
        Log.d("[LocationManager]", "위치 추적이 중지되었습니다.")
    }

    /** 위치 변경 콜백 */
    override fun onLocationChanged(location: Location) {
        currentLocation = location
        Log.d("[LocationManager]", "위치 업데이트: ${location.latitude}, ${location.longitude}")
        updateShipLocation(location)
        
        // 자동 추적이 활성화된 경우에만 카메라를 따라가게 함
        if (isAutoTracking) {
            centerMapOnCurrentLocation()
        }
    }

    /** 현재 위치 LatLng */
    fun getCurrentLocation(): LatLng? = currentLocation?.let { LatLng(it.latitude, it.longitude) }

    /** 선박 아이콘을 지도에 추가 */
    fun addShipToMap(style: Style) {
        try {
            val shipBitmap = createShipIconBitmap()
            style.addImage("ship-icon", shipBitmap)

            shipSource = GeoJsonSource(SHIP_SOURCE_ID).also { style.addSource(it) }

            shipLayer = SymbolLayer(SHIP_LAYER_ID, SHIP_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.iconImage("ship-icon"),
                    PropertyFactory.iconSize(1.0f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
                )
            }
            style.addLayer(shipLayer!!)
            Log.d("[LocationManager]", "선박 아이콘 레이어가 추가되었습니다.")
        } catch (e: Exception) {
            Log.e("[LocationManager]", "선박 아이콘 추가 실패: ${e.message}")
        }
    }

    /** 선박 위치 업데이트 */
    private fun updateShipLocation(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        shipSource?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(point)))
    }

    /** 지도 중앙을 현재 위치로 이동 (줌 레벨 유지) */
    fun centerMapOnCurrentLocation() {
        currentLocation?.let { loc ->
            val latLng = LatLng(loc.latitude, loc.longitude)
            val zoom = savedZoomLevel ?: 15.0 // 저장된 줌 레벨이 있으면 사용, 없으면 기본값 15.0
            val camPos = CameraPosition.Builder().target(latLng).zoom(zoom).build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(camPos), 1000)
            Log.d("[LocationManager]", "지도가 현재 위치로 이동되었습니다: $latLng (줌: $zoom)")
        } ?: Log.w("[LocationManager]", "현재 위치를 찾을 수 없습니다.")
    }

    /** 자동 추적 시작 (버튼 클릭 시 호출) */
    fun startAutoTracking() {
        isAutoTracking = true
        centerMapOnCurrentLocation()
        Log.d("[LocationManager]", "자동 추적이 시작되었습니다.")
    }

    /** 자동 추적 중지 (사용자가 지도를 터치/드래그할 때 호출) */
    fun stopAutoTracking() {
        isAutoTracking = false
        // 현재 줌 레벨을 저장 (사용자가 설정한 줌 레벨 유지)
        savedZoomLevel = map.cameraPosition.zoom
        Log.d("[LocationManager]", "자동 추적이 중지되었습니다. 줌 레벨 저장: $savedZoomLevel")
    }

    /** 자동 추적 상태 확인 */
    fun isAutoTrackingEnabled(): Boolean = isAutoTracking

    /** 현재 위치 표시용 원형 마커 비트맵 생성 */
    fun createShipIconBitmap(): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 흰색 테두리
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        
        // 빨간색 채우기
        val fillPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 6f // 테두리를 위한 여백
        
        // 빨간색 원 그리기
        canvas.drawCircle(centerX, centerY, radius, fillPaint)
        // 흰색 테두리 그리기
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
        
        return bitmap
    }
    
    /** 포인트 마커를 지도에 추가 */
    fun addPointsToMap(style: Style) {
        try {
            // 포인트 마커 아이콘 추가
            val pointBitmap = createPointIconBitmap(ComposeColor.Red)
            style.addImage("point-icon", pointBitmap)
            
            // 포인트 소스와 레이어 추가
            pointsSource = GeoJsonSource(POINTS_SOURCE_ID).also { style.addSource(it) }
            
            pointsLayer = SymbolLayer(POINTS_LAYER_ID, POINTS_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.iconImage("point-icon"),
                    PropertyFactory.iconSize(1.0f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    PropertyFactory.textField(get("name")),
                    PropertyFactory.textSize(12f),
                    PropertyFactory.textColor(Color.WHITE),
                    PropertyFactory.textHaloColor(Color.BLACK),
                    PropertyFactory.textHaloWidth(2f),
                    PropertyFactory.textOffset(arrayOf(0f, -2f)),
                    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.visibility(Property.VISIBLE)
                )
                setMinZoom(16.0f) // 줌 레벨 8 이상에서만 표시
            }
            style.addLayer(pointsLayer!!)
            Log.d("[LocationManager]", "포인트 마커 레이어가 추가되었습니다.")
        } catch (e: Exception) {
            Log.e("[LocationManager]", "포인트 마커 추가 실패: ${e.message}")
        }
    }
    
    /** 포인트 마커 비트맵 생성 */
    private fun createPointIconBitmap(color: ComposeColor): Bitmap {
        val size = 32
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 흰색 테두리
        val borderPaint = Paint().apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        
        // 색상 채우기
        val fillPaint = Paint().apply {
            this.color = Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 4f
        
        // 색상 원 그리기
        canvas.drawCircle(centerX, centerY, radius, fillPaint)
        // 흰색 테두리 그리기
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
        
        return bitmap
    }
    
    /** 저장된 포인트들을 지도에 표시 */
    fun updatePointsOnMap(points: List<SavedPoint>) {
        try {
            val features = points.map { point ->
                val geometry = Point.fromLngLat(point.longitude, point.latitude)
                Feature.fromGeometry(geometry).apply {
                    addStringProperty("name", point.name)
                    addNumberProperty("color", Color.argb(
                        (point.color.alpha * 255).toInt(),
                        (point.color.red * 255).toInt(),
                        (point.color.green * 255).toInt(),
                        (point.color.blue * 255).toInt()
                    ))
                    addStringProperty("id", "${point.latitude}_${point.longitude}_${point.timestamp}")
                }
            }
            
            pointsSource?.setGeoJson(FeatureCollection.fromFeatures(features))
            Log.d("[LocationManager]", "포인트 ${points.size}개가 지도에 표시되었습니다.")
        } catch (e: Exception) {
            Log.e("[LocationManager]", "포인트 업데이트 실패: ${e.message}")
        }
    }
    
    /** 포인트 클릭 리스너 설정 */
    fun setOnPointClickListener(listener: (SavedPoint) -> Unit) {
        onPointClickListener = listener
    }
    
    /** 포인트 클릭 이벤트 처리 */
    fun handlePointClick(latLng: LatLng, points: List<SavedPoint>): Boolean {
        // 클릭된 위치와 가장 가까운 포인트 찾기 (10미터 이내)
        val clickedPoint = points.find { point ->
            val distance = calculateDistance(
                latLng.latitude, latLng.longitude,
                point.latitude, point.longitude
            )
            distance <= 10.0 // 10미터 이내
        }
        
        clickedPoint?.let { point ->
            onPointClickListener?.invoke(point)
            return true
        }
        return false
    }
    
    /** 두 좌표 간의 거리 계산 (미터) */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}