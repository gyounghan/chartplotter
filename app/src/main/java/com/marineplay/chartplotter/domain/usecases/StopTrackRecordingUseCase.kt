package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.TrackManager
import com.marineplay.chartplotter.TrackPoint

/**
 * 항적 기록 중지 UseCase
 */
class StopTrackRecordingUseCase(
    private val trackManager: TrackManager
) {
    /**
     * 항적 기록을 중지하고 저장합니다.
     * @param trackId 항적 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param points 기록된 포인트 목록
     * @return 저장된 항적 기록 (실패 시 null)
     */
    fun execute(
        trackId: String,
        startTime: Long,
        endTime: Long,
        points: List<TrackPoint>
    ): com.marineplay.chartplotter.TrackRecord? {
        if (points.isEmpty()) {
            return null
        }
        
        return trackManager.addTrackRecord(
            trackId = trackId,
            startTime = startTime,
            endTime = endTime,
            points = points
        )
    }
}

