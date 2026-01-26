package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.domain.repositories.AISRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * AIS 연결 UseCase
 */
class ConnectAISUseCase(
    private val repository: AISRepository
) {
    /**
     * 연결 상태 Flow
     */
    val isConnected: StateFlow<Boolean> = repository.isConnected
    
    /**
     * USB 연결 시작
     */
    operator fun invoke(baudRate: Int = 38400): Boolean {
        return repository.connect(baudRate)
    }
    
    /**
     * USB 연결 해제
     */
    fun disconnect() {
        repository.disconnect()
    }
}

