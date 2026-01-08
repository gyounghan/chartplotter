package com.marineplay.chartplotter.domain.usecases

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * 줌 UseCase
 */
class ZoomUseCase {
    /**
     * 줌 인을 수행합니다.
     * @param map MapLibreMap 인스턴스
     * @param cursorLatLng 커서 위치 (있으면 커서를 중앙으로 맞춤)
     * @return 새로운 줌 레벨
     */
    fun zoomIn(
        map: MapLibreMap,
        cursorLatLng: LatLng?
    ): Double {
        val currentZoom = map.cameraPosition.zoom
        val newZoom = (currentZoom + 0.1).coerceAtMost(22.0)
        val currentPosition = map.cameraPosition
        
        val newPosition = if (cursorLatLng != null) {
            // 커서 위치를 중앙으로 맞추고 줌 인
            CameraPosition.Builder()
                .target(cursorLatLng)
                .zoom(newZoom)
                .bearing(currentPosition.bearing)
                .build()
        } else {
            // 현재 위치에서 줌 인
            CameraPosition.Builder()
                .target(currentPosition.target)
                .zoom(newZoom)
                .bearing(currentPosition.bearing)
                .build()
        }
        
        map.cameraPosition = newPosition
        return newZoom
    }
    
    /**
     * 줌 아웃을 수행합니다.
     * @param map MapLibreMap 인스턴스
     * @param cursorLatLng 커서 위치 (있으면 커서를 중앙으로 맞춤)
     * @return 새로운 줌 레벨
     */
    fun zoomOut(
        map: MapLibreMap,
        cursorLatLng: LatLng?
    ): Double {
        val currentZoom = map.cameraPosition.zoom
        val newZoom = (currentZoom - 0.1).coerceAtLeast(6.0)
        val currentPosition = map.cameraPosition
        
        val newPosition = if (cursorLatLng != null) {
            // 커서 위치를 중앙으로 맞추고 줌 아웃
            CameraPosition.Builder()
                .target(cursorLatLng)
                .zoom(newZoom)
                .bearing(currentPosition.bearing)
                .build()
        } else {
            // 현재 위치에서 줌 아웃
            CameraPosition.Builder()
                .target(currentPosition.target)
                .zoom(newZoom)
                .bearing(currentPosition.bearing)
                .build()
        }
        
        map.cameraPosition = newPosition
        return newZoom
    }
    
    /**
     * 커서의 화면 위치를 업데이트합니다 (줌 후 커서 위치 유지용).
     * @param map MapLibreMap 인스턴스
     * @param cursorLatLng 커서 위치
     * @return 업데이트된 화면 위치
     */
    fun updateCursorScreenPosition(
        map: MapLibreMap,
        cursorLatLng: LatLng
    ): android.graphics.PointF {
        return map.projection.toScreenLocation(cursorLatLng)
    }
}
