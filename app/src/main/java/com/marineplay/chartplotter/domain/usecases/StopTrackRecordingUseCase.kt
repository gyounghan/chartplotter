package com.marineplay.chartplotter.domain.usecases

/**
 * 항적 기록 중지 UseCase
 * TrackPoint는 이미 실시간으로 저장되므로, 이 UseCase는 더 이상 필요 없습니다.
 * 하지만 하위 호환성을 위해 유지합니다.
 */
class StopTrackRecordingUseCase {
    /**
     * 항적 기록을 중지합니다.
     * TrackPoint는 이미 실시간으로 저장되었으므로, 별도 저장 작업은 필요 없습니다.
     */
    fun execute() {
        // TrackPoint는 이미 실시간으로 저장되었으므로, 아무 작업도 하지 않습니다.
    }
}

