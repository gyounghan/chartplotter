package com.marineplay.chartplotter.domain.repositories

import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.domain.entities.RiskEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * AIS 데이터 저장소 인터페이스
 * Domain 레이어에서 정의하여 의존성 역전 원칙 준수
 */
interface AISRepository {
    /**
     * 선박 목록 Flow
     */
    val vessels: StateFlow<List<AISVessel>>
    
    /**
     * 위험 이벤트 목록 Flow
     */
    val events: StateFlow<List<RiskEvent>>
    
    /**
     * USB 연결 상태 Flow
     */
    val isConnected: StateFlow<Boolean>
    
    /**
     * 현재 위치 가져오기
     */
    fun getCurrentLocation(): Pair<Double?, Double?>
    
    /**
     * 현재 위치 업데이트
     */
    fun updateCurrentLocation(latitude: Double, longitude: Double)
    
    /**
     * USB 연결 시작
     */
    fun connect(baudRate: Int = 38400): Boolean
    
    /**
     * USB 연결 해제
     */
    fun disconnect()
    
    /**
     * 관심 선박 토글
     */
    fun toggleWatchlist(vesselId: String)
    
    /**
     * 리소스 정리
     */
    fun cleanup()
}

