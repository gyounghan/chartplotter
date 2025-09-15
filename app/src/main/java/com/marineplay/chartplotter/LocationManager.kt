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
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

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

    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10 m
        private const val MIN_TIME_BETWEEN_UPDATES = 1000L      // 1 s
        private const val SHIP_SOURCE_ID = "ship-location"
        private const val SHIP_LAYER_ID = "ship-symbol"
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

    /** 지도 중앙을 현재 위치로 이동 */
    fun centerMapOnCurrentLocation() {
        currentLocation?.let { loc ->
            val latLng = LatLng(loc.latitude, loc.longitude)
            val camPos = CameraPosition.Builder().target(latLng).zoom(15.0).build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(camPos), 1000)
            Log.d("[LocationManager]", "지도가 현재 위치로 이동되었습니다: $latLng")
        } ?: Log.w("[LocationManager]", "현재 위치를 찾을 수 없습니다.")
    }

    /** 간단한 선박 아이콘 비트맵 생성 */
    fun createShipIconBitmap(): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val path = android.graphics.Path().apply {
            moveTo(size / 2f, 0f)
            lineTo(0f, size.toFloat())
            lineTo(size.toFloat(), size.toFloat())
            close()
        }
        canvas.drawPath(path, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 4f, paint)
        return bitmap
    }
}