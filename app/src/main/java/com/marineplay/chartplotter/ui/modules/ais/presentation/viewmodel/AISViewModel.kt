package com.marineplay.chartplotter.ui.modules.ais.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marineplay.chartplotter.domain.usecases.*
import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.domain.entities.RiskEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AIS 화면 ViewModel
 * Presentation 레이어에서 UseCase를 통해 Domain 레이어와 통신
 */
class AISViewModel(
    private val getVesselsUseCase: GetVesselsUseCase,
    private val getEventsUseCase: GetEventsUseCase,
    private val updateLocationUseCase: UpdateLocationUseCase,
    private val connectAISUseCase: ConnectAISUseCase,
    private val toggleWatchlistUseCase: ToggleWatchlistUseCase
) : ViewModel() {
    
    // 선박 목록
    val vessels: StateFlow<List<AISVessel>> = getVesselsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 이벤트 목록
    val events: StateFlow<List<RiskEvent>> = getEventsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 연결 상태
    val isConnected: StateFlow<Boolean> = connectAISUseCase.isConnected
    
    init {
        // AIS 연결 시도
        connect()
    }
    
    /**
     * 위치 업데이트
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        updateLocationUseCase(latitude, longitude)
    }
    
    /**
     * AIS 연결
     */
    fun connect(baudRate: Int = 38400) {
        viewModelScope.launch {
            connectAISUseCase(baudRate)
        }
    }
    
    /**
     * AIS 연결 해제
     */
    fun disconnect() {
        viewModelScope.launch {
            connectAISUseCase.disconnect()
        }
    }
    
    /**
     * 관심 선박 토글
     */
    fun toggleWatchlist(vesselId: String) {
        toggleWatchlistUseCase(vesselId)
    }
    
    /**
     * 현재 위치 가져오기
     */
    fun getCurrentLocation(): Pair<Double?, Double?> {
        // Repository를 통해 직접 접근 (임시)
        // TODO: UseCase로 분리하는 것이 더 좋지만, 현재 구조상 필요
        return updateLocationUseCase.getCurrentLocation()
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}

