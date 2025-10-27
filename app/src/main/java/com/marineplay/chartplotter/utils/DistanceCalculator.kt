package com.marineplay.chartplotter.utils

import android.util.Log
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

object DistanceCalculator {
    
    /**
     * 두 지점 간의 실제 거리 계산 (미터)
     */
    fun calculateGeographicDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = earthRadius * c
        Log.d("[DistanceCalculator]", "지리적 거리 계산: (${lat1}, ${lon1}) -> (${lat2}, ${lon2}) = ${distance}미터")
        return distance
    }
    
    /**
     * 두 지점 간의 화면 거리 계산 (픽셀)
     */
    fun calculateScreenDistance(
        clickLatLng: LatLng, 
        targetLatLng: LatLng, 
        map: MapLibreMap
    ): Double {
        val clickScreenPoint = map.projection.toScreenLocation(clickLatLng)
        val targetScreenPoint = map.projection.toScreenLocation(targetLatLng)
        
        val dx = clickScreenPoint.x - targetScreenPoint.x
        val dy = clickScreenPoint.y - targetScreenPoint.y
        val screenDistance = Math.sqrt((dx * dx + dy * dy).toDouble())
        
        Log.d("[DistanceCalculator]", "화면 거리 계산: ${screenDistance}픽셀")
        return screenDistance
    }
}