package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.TrackManager
import com.marineplay.chartplotter.TrackPoint
import org.maplibre.android.geometry.LatLng

/**
 * 항적 점 추가 UseCase
 */
class AddTrackPointUseCase(
    private val trackManager: TrackManager,
    private val calculateDistanceUseCase: CalculateDistanceUseCase
) {
    /**
     * 항적 점을 추가해야 하는지 판단하고 추가합니다.
     * @param latitude 현재 위도
     * @param longitude 현재 경도
     * @param currentTime 현재 시간
     * @param lastTrackPointTime 마지막 항적 점 시간
     * @param lastTrackPointLocation 마지막 항적 점 위치
     * @return 추가된 항적 점 (추가하지 않았으면 null)
     */
    fun execute(
        latitude: Double,
        longitude: Double,
        currentTime: Long,
        lastTrackPointTime: Long,
        lastTrackPointLocation: LatLng?
    ): TrackPoint? {
        val settings = trackManager.settings
        val currentLocation = LatLng(latitude, longitude)
        
        when (settings.intervalType) {
            "time" -> {
                // 시간 간격: 첫 번째 점이거나 설정된 시간 간격이 지났으면 추가
                if (lastTrackPointTime == 0L) {
                    return TrackPoint(latitude, longitude, currentTime)
                }
                // 시간 간격 체크는 타이머에서 처리하므로 여기서는 null 반환
                return null
            }
            "distance" -> {
                // 거리 간격: 첫 번째 점이거나 설정된 거리 이상 이동했으면 추가
                if (lastTrackPointLocation == null) {
                    return TrackPoint(latitude, longitude, currentTime)
                } else {
                    val distance = calculateDistanceUseCase.execute(
                        lastTrackPointLocation.latitude,
                        lastTrackPointLocation.longitude,
                        latitude,
                        longitude
                    )
                    if (distance >= settings.distanceInterval) {
                        return TrackPoint(latitude, longitude, currentTime)
                    }
                }
                return null
            }
            else -> return null
        }
    }
}

