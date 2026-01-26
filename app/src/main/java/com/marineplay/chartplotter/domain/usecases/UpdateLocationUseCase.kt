package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.domain.repositories.AISRepository

/**
 * 현재 위치 업데이트 UseCase
 */
class UpdateLocationUseCase(
    private val repository: AISRepository
) {
    /**
     * 위치 업데이트 실행
     */
    operator fun invoke(latitude: Double, longitude: Double) {
        repository.updateCurrentLocation(latitude, longitude)
    }
    
    /**
     * 현재 위치 가져오기
     */
    fun getCurrentLocation(): Pair<Double?, Double?> {
        return repository.getCurrentLocation()
    }
}

