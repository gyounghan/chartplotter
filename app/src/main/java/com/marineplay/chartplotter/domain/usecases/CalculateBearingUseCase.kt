package com.marineplay.chartplotter.domain.usecases

/**
 * 방위각(Bearing) 계산 UseCase
 */
class CalculateBearingUseCase {
    /**
     * 두 지점 간의 방위각을 계산합니다.
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 방위각 (도, 0-360)
     */
    fun execute(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val y = Math.sin(deltaLonRad) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)

        val bearingRad = Math.atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)

        return (((bearingDeg % 360) + 360) % 360).toFloat()
    }
}

