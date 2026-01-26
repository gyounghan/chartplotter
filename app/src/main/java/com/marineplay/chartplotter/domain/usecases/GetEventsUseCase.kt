package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.domain.entities.RiskEvent
import com.marineplay.chartplotter.domain.repositories.AISRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * 위험 이벤트 목록 조회 UseCase
 */
class GetEventsUseCase(
    private val repository: AISRepository
) {
    /**
     * 이벤트 목록 Flow 반환
     */
    operator fun invoke(): StateFlow<List<RiskEvent>> {
        return repository.events
    }
}

