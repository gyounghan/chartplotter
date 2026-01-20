package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.domain.entities.TrackPoint
import org.maplibre.android.geometry.LatLng

/**
 * 항적 점 추가 UseCase
 */
class AddTrackPointUseCase(
    private val calculateDistanceUseCase: CalculateDistanceUseCase
) {
    /**
     * 항적 점을 추가해야 하는지 판단하고 추가합니다.
     * @param latitude 현재 위도
     * @param longitude 현재 경도
     * @param currentTime 현재 시간
     * @param lastTrackPointTime 마지막 항적 점 시간
     * @param lastTrackPointLocation 마지막 항적 점 위치
     * @param intervalType 기록 간격 타입 ("time" or "distance")
     * @param timeInterval 시간 간격 (밀리초, intervalType이 "time"일 때 사용)
     * @param distanceInterval 거리 간격 (미터, intervalType이 "distance"일 때 사용)
     * @param isTimerTriggered 타이머에서 호출되었는지 여부 (시간 기준일 때만 사용)
     * @return 추가된 항적 점 (추가하지 않았으면 null)
     */
    fun execute(
        latitude: Double,
        longitude: Double,
        currentTime: Long,
        lastTrackPointTime: Long,
        lastTrackPointLocation: LatLng?,
        intervalType: String,
        timeInterval: Long = 5000L,
        distanceInterval: Double = 10.0,
        isTimerTriggered: Boolean = false
    ): TrackPoint? {
        val currentLocation = LatLng(latitude, longitude)
        
        when (intervalType) {
            "time" -> {
                // 시간 간격: 첫 번째 점이거나 타이머에서 호출된 경우 추가
                if (lastTrackPointTime == 0L) {
                    return TrackPoint(latitude, longitude, currentTime)
                }
                // 타이머에서 호출된 경우 무조건 추가 (시간 간격이 지났다는 것이 보장됨)
                if (isTimerTriggered) {
                    return TrackPoint(latitude, longitude, currentTime)
                }
                // GPS 업데이트에서 호출된 경우는 null 반환 (타이머가 처리)
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
                    if (distance >= distanceInterval) {
                        return TrackPoint(latitude, longitude, currentTime)
                    }
                    // 정지 상태에서도 최소 간격(30초)으로 점 추가
                    if (lastTrackPointTime > 0L && currentTime - lastTrackPointTime >= 30000L) {
                        return TrackPoint(latitude, longitude, currentTime)
                    }
                }
                return null
            }
            else -> return null
        }
    }
}

