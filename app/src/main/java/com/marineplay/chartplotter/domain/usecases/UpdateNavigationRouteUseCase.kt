package com.marineplay.chartplotter.domain.usecases

import android.location.Location
import com.marineplay.chartplotter.PMTilesLoader
import com.marineplay.chartplotter.SavedPoint
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * 항해 경로 업데이트 UseCase
 * 현재 위치, 경유지, 목적지를 기반으로 항해 경로를 지도에 표시합니다.
 */
class UpdateNavigationRouteUseCase {
    
    /**
     * 항해 경로를 업데이트합니다.
     * 
     * @param map MapLibreMap 인스턴스
     * @param currentLocation 현재 위치 (Location 객체)
     * @param waypoints 경유지 리스트
     * @param navigationPoint 목적지 (SavedPoint)
     */
    fun execute(
        map: MapLibreMap?,
        currentLocation: Location?,
        waypoints: List<SavedPoint>,
        navigationPoint: SavedPoint?
    ) {
        if (map == null || currentLocation == null || navigationPoint == null) {
            return
        }
        
        val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val waypointLatLngs = waypoints.map { LatLng(it.latitude, it.longitude) }
        val navigationLatLng = LatLng(navigationPoint.latitude, navigationPoint.longitude)
        
        PMTilesLoader.addNavigationRoute(map, currentLatLng, waypointLatLngs, navigationLatLng)
    }
}

