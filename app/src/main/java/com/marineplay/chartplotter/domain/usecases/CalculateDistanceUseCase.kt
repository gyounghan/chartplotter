package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.utils.DistanceCalculator

/**
 * 거리 계산 UseCase
 */
class CalculateDistanceUseCase {
    /**
     * 두 지점 간의 실제 거리를 계산합니다 (미터).
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 거리 (미터)
     */
    fun execute(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        return DistanceCalculator.calculateGeographicDistance(lat1, lon1, lat2, lon2)
    }
}

