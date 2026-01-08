package com.marineplay.chartplotter.ui.utils

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.*
import com.marineplay.chartplotter.domain.mappers.PointMapper
import com.marineplay.chartplotter.helpers.PointHelper
import com.marineplay.chartplotter.viewmodel.MainViewModel
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.*

/**
 * ChartPlotterScreen에서 사용하는 헬퍼 함수들
 */
object ChartPlotterHelpers {
    
    /**
     * 사용 가능한 최소 포인트 번호 반환
     */
    fun getNextAvailablePointNumber(viewModel: MainViewModel): Int {
        return viewModel.getNextAvailablePointNumber()
    }
    
    /**
     * 로컬에서 포인트 목록 로드
     */
    fun loadPointsFromLocal(viewModel: MainViewModel): List<SavedPoint> {
        return PointMapper.toUiPoints(viewModel.loadPointsFromLocal())
    }
    
    /**
     * 두 지점 간의 bearing 계산
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return (((bearingDeg % 360) + 360) % 360).toFloat()
    }
    
    /**
     * 두 지점 간의 실제 거리 계산 (미터)
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return com.marineplay.chartplotter.utils.DistanceCalculator.calculateGeographicDistance(
            lat1,
            lon1,
            lat2,
            lon2
        )
    }
    
    /**
     * 두 지점 간의 화면 거리 계산 (픽셀)
     */
    fun calculateScreenDistance(latLng1: LatLng, latLng2: LatLng, map: MapLibreMap): Double {
        return com.marineplay.chartplotter.utils.DistanceCalculator.calculateScreenDistance(
            latLng1,
            latLng2,
            map
        )
    }
}

