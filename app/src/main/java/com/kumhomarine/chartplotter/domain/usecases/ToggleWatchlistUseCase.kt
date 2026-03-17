package com.kumhomarine.chartplotter.domain.usecases

import com.kumhomarine.chartplotter.domain.repositories.AISRepository

/**
 * 관심 선박 토글 UseCase
 */
class ToggleWatchlistUseCase(
    private val repository: AISRepository
) {
    /**
     * 관심 선박 토글 실행
     */
    operator fun invoke(vesselId: String) {
        repository.toggleWatchlist(vesselId)
    }
}

