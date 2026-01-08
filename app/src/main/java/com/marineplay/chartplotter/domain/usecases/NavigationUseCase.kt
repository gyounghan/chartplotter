package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.PMTilesLoader
import com.marineplay.chartplotter.SavedPoint
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * 항해 경로 UseCase
 */
class NavigationUseCase(
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val calculateBearingUseCase: CalculateBearingUseCase
) {
    /**
     * 항해 경로를 업데이트합니다.
     * @param map MapLibreMap 인스턴스
     * @param currentLocation 현재 위치
     * @param waypoints 경유지 목록
     * @param navigationPoint 목적지
     */
    fun updateNavigationRoute(
        map: MapLibreMap,
        currentLocation: LatLng,
        waypoints: List<SavedPoint>,
        navigationPoint: SavedPoint
    ) {
        val waypointLatLngs = waypoints.map { LatLng(it.latitude, it.longitude) }
        val navigationLatLng = LatLng(navigationPoint.latitude, navigationPoint.longitude)
        PMTilesLoader.addNavigationRoute(map, currentLocation, waypointLatLngs, navigationLatLng)
    }
    
    /**
     * 경유지 도달 여부를 확인합니다.
     * @param currentLat 현재 위도
     * @param currentLon 현재 경도
     * @param waypoint 경유지
     * @param thresholdMeters 도달 판단 거리 (미터, 기본값: 10m)
     * @return 도달 여부
     */
    fun isWaypointReached(
        currentLat: Double,
        currentLon: Double,
        waypoint: SavedPoint,
        thresholdMeters: Double = 10.0
    ): Boolean {
        val distance = calculateDistanceUseCase.execute(
            currentLat,
            currentLon,
            waypoint.latitude,
            waypoint.longitude
        )
        return distance <= thresholdMeters
    }
    
    /**
     * 현재 위치에서 목적지까지의 방위각을 계산합니다.
     * @param currentLat 현재 위도
     * @param currentLon 현재 경도
     * @param destination 목적지
     * @return 방위각 (도, 0-360)
     */
    fun calculateBearingToDestination(
        currentLat: Double,
        currentLon: Double,
        destination: SavedPoint
    ): Float {
        return calculateBearingUseCase.execute(
            currentLat,
            currentLon,
            destination.latitude,
            destination.longitude
        )
    }
}

