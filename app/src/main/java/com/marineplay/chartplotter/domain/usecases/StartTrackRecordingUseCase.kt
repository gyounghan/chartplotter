package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.domain.entities.Track

/**
 * 항적 기록 시작 UseCase
 */
class StartTrackRecordingUseCase {
    /**
     * 항적 기록을 시작합니다.
     * @param track 기록할 항적
     * @return 항적 (설정 정보는 Track에 포함됨)
     */
    fun execute(track: Track): Track {
        return track
    }
}

