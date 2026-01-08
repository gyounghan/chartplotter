package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.LocationManager
import com.marineplay.chartplotter.PMTilesLoader
import com.marineplay.chartplotter.SavedPoint
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * 지도 회전 UseCase
 */
class MapRotationUseCase(
    private val locationManager: LocationManager?,
    private val calculateBearingUseCase: CalculateBearingUseCase
) {
    /**
     * 지도 회전을 업데이트합니다.
     * @param map MapLibreMap 인스턴스
     * @param displayMode 지도 표시 모드 ("노스업", "헤딩업", "코스업")
     * @param coursePoint 코스업 모드에서 사용할 포인트
     * @param currentLocation 현재 위치 (코스업 모드에서 사용)
     * @return 새로운 카메라 위치
     */
    fun execute(
        map: MapLibreMap,
        displayMode: String,
        coursePoint: SavedPoint?,
        currentLocation: android.location.Location?
    ): CameraPosition {
        val currentPosition = map.cameraPosition
        
        return when (displayMode) {
            "노스업" -> {
                // 북쪽이 위쪽 (0도)
                val newPosition = CameraPosition.Builder()
                    .target(currentPosition.target)
                    .zoom(currentPosition.zoom)
                    .bearing(0.0)
                    .build()
                map.cameraPosition = newPosition
                PMTilesLoader.removeCourseLine(map)
                newPosition
            }
            "헤딩업" -> {
                // 보트의 진행방향이 위쪽
                val heading = locationManager?.getCurrentBearing() ?: 0f
                val newPosition = CameraPosition.Builder()
                    .target(currentPosition.target)
                    .zoom(currentPosition.zoom)
                    .bearing(heading.toDouble())
                    .build()
                map.cameraPosition = newPosition
                PMTilesLoader.removeCourseLine(map)
                newPosition
            }
            "코스업" -> {
                // 포인트 방향이 위쪽
                coursePoint?.let { point ->
                    currentLocation?.let { location ->
                        val bearing = calculateBearingUseCase.execute(
                            location.latitude,
                            location.longitude,
                            point.latitude,
                            point.longitude
                        )
                        
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        val newPosition = CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(currentPosition.zoom)
                            .bearing(bearing.toDouble())
                            .build()
                        map.cameraPosition = newPosition
                        newPosition
                    } ?: currentPosition
                } ?: run {
                    // 코스 포인트가 없으면 노스업으로
                    val newPosition = CameraPosition.Builder()
                        .target(currentPosition.target)
                        .zoom(currentPosition.zoom)
                        .bearing(0.0)
                        .build()
                    map.cameraPosition = newPosition
                    PMTilesLoader.removeCourseLine(map)
                    newPosition
                }
            }
            else -> {
                // 기본값: 노스업
                val newPosition = CameraPosition.Builder()
                    .target(currentPosition.target)
                    .zoom(currentPosition.zoom)
                    .bearing(0.0)
                    .build()
                map.cameraPosition = newPosition
                PMTilesLoader.removeCourseLine(map)
                newPosition
            }
        }
    }
}

