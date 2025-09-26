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
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import org.maplibre.android.style.layers.CircleLayer
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
) : LocationListener, SensorEventListener {

    // 시스템 LocationManager
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as SysLocationManager
    
    // 센서 관련
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var orientationSensor: Sensor? = null
    private var currentBearing: Float = 0f // 현재 방위각 (도)

    private var currentLocation: Location? = null
    private var shipSource: GeoJsonSource? = null
    private var shipLayer: SymbolLayer? = null
    private var isAutoTracking = false // 자동 추적 상태
    private var savedZoomLevel: Double? = null // 사용자가 설정한 줌 레벨 저장
    private var rotationVector: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

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
            // 북쪽(위쪽)을 향하는 삼각형 비트맵을 1번만 등록
            val shipBitmap = createShipTriangleIcon(0f)
            style.addImage("ship-icon", shipBitmap)

            shipSource = GeoJsonSource(SHIP_SOURCE_ID).also { style.addSource(it) }

            shipLayer = SymbolLayer(SHIP_LAYER_ID, SHIP_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.iconImage("ship-icon"),
                    PropertyFactory.iconSize(1.0f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    PropertyFactory.iconRotate(get("bearing")),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)
                )
            }
            style.addLayer(shipLayer!!)
            Log.d("[LocationManager]", "삼각형 선박 아이콘 레이어가 추가되었습니다.")
        } catch (e: Exception) {
            Log.e("[LocationManager]", "선박 아이콘 추가 실패: ${e.message}")
        }
    }

    /** 선박 위치 업데이트 (방향 포함) */
    private fun updateShipLocation(location: Location) {
        try {
            val latLng = LatLng(location.latitude, location.longitude)

            // 1) 이동 중이면 GPS bearing 우선
            val speedMps = location.speed     // m/s
            val gpsBearing = if (location.hasBearing() && speedMps > 0.8f) location.bearing else null

            // 2) 정지/저속이면 센서 방위 사용
            val bearing = gpsBearing ?: currentBearing

            // 3) 피처에 bearing 속성 포함
            val feature = Feature.fromGeometry(
                Point.fromLngLat(latLng.longitude, latLng.latitude)
            ).apply {
                addNumberProperty("bearing", ((bearing % 360) + 360) % 360) // 0~360 정규화
            }

//            // 회전된 삼각형 아이콘 생성
//            val shipIcon = createShipTriangleIcon(bearing)
//
//            // 스타일에 아이콘 추가
//            map.style?.addImage("ship-icon", shipIcon)
            
            shipSource?.setGeoJson(feature)
            
            Log.d("[LocationManager]", "선박 아이콘 업데이트: 방위각 ${bearing}도")
        } catch (e: Exception) {
            Log.e("[LocationManager]", "선박 아이콘 업데이트 실패: ${e.message}")
        }
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
            // 1) 포인트 소스
            pointsSource = GeoJsonSource(POINTS_SOURCE_ID).also { style.addSource(it) }

            // 2) 포인트 심볼 레이어 (클릭 이벤트 지원)
            pointsLayer = SymbolLayer(POINTS_LAYER_ID, POINTS_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.iconImage(get("iconId")), // 동적 아이콘 ID
                    PropertyFactory.iconSize(1.0f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(false),
                    PropertyFactory.visibility(Property.VISIBLE)
                )
                setMinZoom(8.0f)
            }
            style.addLayer(pointsLayer!!)

            // 3) 라벨 전용 심볼 레이어(텍스트만)
            val labelLayer = SymbolLayer("${POINTS_LAYER_ID}-label", POINTS_SOURCE_ID).apply {
                setProperties(
                    PropertyFactory.textField(get("name")),
                    PropertyFactory.textSize(12f),
                    PropertyFactory.textColor(Color.WHITE),
                    PropertyFactory.textHaloColor(Color.BLACK),
                    PropertyFactory.textHaloWidth(2f),
                    PropertyFactory.textOffset(arrayOf(0f, -1.8f)),
                    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.visibility(Property.VISIBLE)
                )
                setMinZoom(8.0f)
            }
            style.addLayer(labelLayer)

            Log.d("[LocationManager]", "포인트(심볼+라벨) 레이어가 추가되었습니다.")
        } catch (e: Exception) {
            Log.e("[LocationManager]", "포인트 레이어 추가 실패: ${e.message}")
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

    private fun composeColorToHex(c: ComposeColor): String {
        val a = (c.alpha * 255).toInt().coerceIn(0, 255)
        val r = (c.red   * 255).toInt().coerceIn(0, 255)
        val g = (c.green * 255).toInt().coerceIn(0, 255)
        val b = (c.blue  * 255).toInt().coerceIn(0, 255)
        // 불투명만 쓸 거면 "#RRGGBB", 투명도까지 쓰려면 "#AARRGGBB"
        return if (a == 255) String.format("#%02X%02X%02X", r, g, b)
        else String.format("#%02X%02X%02X%02X", a, r, g, b)
    }

    /** 저장된 포인트들을 지도에 표시 */
    fun updatePointsOnMap(points: List<SavedPoint>) {
        try {
            // 각 포인트마다 고유한 아이콘 생성 및 스타일에 추가
            points.forEachIndexed { index, point ->
                val iconId = "point-icon-$index"
                val bitmap = createPointIconBitmap(point.color)
                map.style?.addImage(iconId, bitmap)
            }
            
            val features = points.mapIndexed { index, point ->
                val geometry = Point.fromLngLat(point.longitude, point.latitude)
                val colorHex = composeColorToHex(point.color)
                Feature.fromGeometry(geometry).apply {
                    addStringProperty("name", point.name)
                    addStringProperty("colorHex", colorHex)
                    addStringProperty("id", "${point.latitude}_${point.longitude}_${point.timestamp}")
                    addStringProperty("iconId", "point-icon-$index") // 고유 아이콘 ID
                }
            }
            pointsSource?.setGeoJson(FeatureCollection.fromFeatures(features))
            Log.d("[LocationManager]", "포인트 ${points.size}개 업데이트 (심볼 레이어).")
        } catch (e: Exception) {
            Log.e("[LocationManager]", "포인트 업데이트 실패: ${e.message}")
        }
    }
    
    /** 센서 초기화 */
    fun initializeSensors() {
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        if (orientationSensor != null) {
            sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI)
            Log.d("[LocationManager]", "방향 센서 등록 완료")
        } else {
            Log.w("[LocationManager]", "방향 센서를 사용할 수 없습니다")
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("[LocationManager]", "Rotation Vector 센서 등록 완료")
        } ?: Log.w("[LocationManager]", "Rotation Vector 센서를 사용할 수 없습니다")
    }
    
    /** 센서 해제 */
    fun unregisterSensors() {
        sensorManager.unregisterListener(this)
        Log.d("[LocationManager]", "센서 해제 완료")
    }
    
    /** SensorEventListener 구현 */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rot = FloatArray(9)
        val outRot = FloatArray(9)
        val ori = FloatArray(3)

        // 1) 회전행렬
        SensorManager.getRotationMatrixFromVector(rot, event.values)

        // 2) 화면 회전에 따른 좌표계 remap (기기 방향 보정)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val rotation = wm.defaultDisplay.rotation
        val axisX = when (rotation) {
            android.view.Surface.ROTATION_90  -> SensorManager.AXIS_Y
            android.view.Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y
            else -> SensorManager.AXIS_X
        }
        val axisY = when (rotation) {
            android.view.Surface.ROTATION_90  -> SensorManager.AXIS_MINUS_X
            android.view.Surface.ROTATION_270 -> SensorManager.AXIS_X
            else -> SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(rot, axisX, axisY, outRot)

        // 3) 오일러 각 (azimuth, pitch, roll)
        SensorManager.getOrientation(outRot, ori)
        val azimuthRad = ori[0]
        val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()

        // 4) 0~360 정규화
        currentBearing = ((azimuthDeg % 360f) + 360f) % 360f

        // 위치가 있다면 즉시 갱신(정지 상태에서도 머리방향이 바뀌도록)
        currentLocation?.let { updateShipLocation(it) }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 센서 정확도 변경 시 처리 (필요시)
    }
    
    /** 삼각형 선박 아이콘 생성 */
    private fun createShipTriangleIcon(bearing: Float): Bitmap {
        val size = 72
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 6f
        
        // 삼각형 경로 생성 (북쪽을 향하는 삼각형)
        val path = Path()
        val angle = Math.toRadians(bearing.toDouble())
        
        // 삼각형의 세 점 계산 (회전 적용)
        val frontX = centerX + (radius * 0.8f * Math.sin(angle)).toFloat()
        val frontY = centerY - (radius * 0.8f * Math.cos(angle)).toFloat()
        
        val backLeftX = centerX + (radius * 0.4f * Math.sin(angle + Math.PI * 2/3)).toFloat()
        val backLeftY = centerY - (radius * 0.4f * Math.cos(angle + Math.PI * 2/3)).toFloat()
        
        val backRightX = centerX + (radius * 0.4f * Math.sin(angle - Math.PI * 2/3)).toFloat()
        val backRightY = centerY - (radius * 0.4f * Math.cos(angle - Math.PI * 2/3)).toFloat()
        
        path.moveTo(frontX, frontY)
        path.lineTo(backLeftX, backLeftY)
        path.lineTo(backRightX, backRightY)
        path.close()
        
        // 삼각형 그리기
        canvas.drawPath(path, paint)
        canvas.drawPath(path, strokePaint)
        
        return bitmap
    }
    
    
}