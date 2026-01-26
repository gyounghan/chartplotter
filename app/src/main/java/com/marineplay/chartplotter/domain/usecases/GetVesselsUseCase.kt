package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.domain.repositories.AISRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * 선박 목록 조회 UseCase
 */
class GetVesselsUseCase(
    private val repository: AISRepository
) {
    /**
     * 선박 목록 Flow 반환
     */
    operator fun invoke(): StateFlow<List<AISVessel>> {
        return repository.vessels
    }
}

