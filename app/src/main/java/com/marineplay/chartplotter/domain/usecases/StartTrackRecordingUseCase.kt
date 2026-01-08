package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.Track
import com.marineplay.chartplotter.TrackManager
import com.marineplay.chartplotter.TrackSettings

/**
 * 항적 기록 시작 UseCase
 */
class StartTrackRecordingUseCase(
    private val trackManager: TrackManager
) {
    /**
     * 항적 기록을 시작합니다.
     * @param track 기록할 항적
     * @return 항적 설정 정보
     */
    fun execute(track: Track): TrackSettings {
        return trackManager.settings
    }
}

