package com.marineplay.chartplotter.data.repositories

import com.marineplay.chartplotter.data.datasources.AISDataSource
import com.marineplay.chartplotter.data.parsers.AISNMEAParser
import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.domain.entities.RiskEvent
import com.marineplay.chartplotter.domain.entities.RiskLevel
import com.marineplay.chartplotter.domain.entities.VesselType
import com.marineplay.chartplotter.domain.repositories.AISRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * AIS 데이터 저장소 구현체
 * USB 시리얼 통신을 통해 받은 실제 AIS 데이터를 관리
 */
class AISRepositoryImpl(
    private val dataSource: AISDataSource
) : AISRepository {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 선박 데이터 (MMSI를 키로 사용)
    private val _vessels = MutableStateFlow<Map<String, AISVessel>>(emptyMap())
    private val _vesselsList = MutableStateFlow<List<AISVessel>>(emptyList())
    override val vessels: StateFlow<List<AISVessel>> = _vesselsList.asStateFlow()
    
    // 이벤트 데이터
    private val _events = MutableStateFlow<List<RiskEvent>>(emptyList())
    override val events: StateFlow<List<RiskEvent>> = _events.asStateFlow()
    
    // 연결 상태
    override val isConnected: StateFlow<Boolean> = dataSource.isConnected
    
    // 정적 데이터 캐시 (MMSI -> 선박 이름, 타입 등)
    private val staticDataCache = mutableMapOf<String, Pair<String, VesselType>>()
    
    // 현재 위치 (위도, 경도)
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    
    // 재계산 작업 추적 (중복 계산 방지)
    private var recalculationJob: Job? = null
    
    /**
     * 현재 위치 가져오기
     */
    override fun getCurrentLocation(): Pair<Double?, Double?> {
        return Pair(currentLatitude, currentLongitude)
    }
    
    init {
        // 데이터 수신 시작
        startDataProcessing()
    }
    
    /**
     * 현재 위치 업데이트
     * 백그라운드 스레드에서 비동기로 재계산하여 메인 스레드 블로킹 방지
     */
    override fun updateCurrentLocation(latitude: Double, longitude: Double) {
        // 위치가 크게 변경되지 않았으면 스킵 (약 10m 이내)
        val lat = currentLatitude
        val lon = currentLongitude
        if (lat != null && lon != null) {
            val distanceMeters = calculateDistance(lat, lon, latitude, longitude)
            if (distanceMeters < 10.0) {
                // 10m 미만 이동은 무시 (성능 최적화)
                return
            }
        }
        
        currentLatitude = latitude
        currentLongitude = longitude
        
        // 기존 재계산 작업이 있으면 취소하고 새로 시작
        recalculationJob?.cancel()
        recalculationJob = scope.launch {
            // 백그라운드 스레드에서 재계산
            recalculateVesselMetrics()
        }
    }
    
    /**
     * 두 지점 간 거리 계산 (미터) - Haversine 공식
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    /**
     * USB 연결 시작
     */
    override fun connect(baudRate: Int): Boolean {
        val connected = dataSource.connect(baudRate)
        if (connected) {
            // 연결 성공 시 기존 데이터 정리 (선택사항)
            // _vessels.value = emptyMap()
        }
        return connected
    }
    
    /**
     * USB 연결 해제
     */
    override fun disconnect() {
        dataSource.disconnect()
    }
    
    /**
     * 데이터 처리 시작
     */
    private fun startDataProcessing() {
        // 메시지 수신 콜백 설정
        dataSource.setOnMessageReceived { message ->
            processNMEAMessage(message)
        }
        
        // StateFlow에서도 수신 (백업)
        scope.launch {
            dataSource.rawMessages.collect { messages ->
                messages.forEach { message ->
                    processNMEAMessage(message)
                }
            }
        }
    }
    
    /**
     * NMEA 메시지 처리
     */
    private fun processNMEAMessage(message: String) {
        try {
            val vessel = AISNMEAParser.parseNMEAMessage(
                message,
                currentLatitude,
                currentLongitude
            )
            if (vessel != null) {
                updateVessel(vessel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 기존 선박들의 거리, 베어링, CPA, TCPA 재계산
     * 내 위치가 변경되면 모든 선박의 메트릭을 실시간으로 재계산
     */
    private fun recalculateVesselMetrics() {
        val lat = currentLatitude ?: return
        val lon = currentLongitude ?: return
        
        // 재계산 전 선박 정보 저장 (이벤트 비교용)
        val previousVessels = _vessels.value
        
        val updatedVessels = previousVessels.mapValues { (_, vessel) ->
            // 선박 위치 정보가 있는 경우에만 재계산
            if (vessel.latitude != null && vessel.longitude != null) {
                val result = AISNMEAParser.recalculateVesselMetrics(
                    lat, lon,
                    vessel.latitude!!, vessel.longitude!!,
                    vessel.speed, vessel.course.toDouble()
                )
                
                if (result != null) {
                    val (distance, bearing, metrics) = result
                    val (cpa, tcpa, riskLevel) = metrics
                    
                    vessel.copy(
                        distance = distance,
                        bearing = bearing,
                        cpa = cpa,
                        tcpa = tcpa,
                        riskLevel = riskLevel
                    )
                } else {
                    vessel
                }
            } else {
                vessel
            }
        }
        
        _vessels.value = updatedVessels
        _vesselsList.value = updatedVessels.values.toList()
        
        // 위험 수준이 변경된 선박에 대해 이벤트 재확인
        updatedVessels.values.forEach { updatedVessel ->
            val previousVessel = previousVessels[updatedVessel.mmsi]
            checkAndCreateRiskEvent(updatedVessel, previousVessel)
        }
    }
    
    /**
     * 선박 정보 업데이트
     */
    private fun updateVessel(vessel: AISVessel) {
        val currentVessels = _vessels.value.toMutableMap()
        val existingVessel = currentVessels[vessel.mmsi]
        
        // 기존 선박 정보와 병합
        val updatedVessel = if (existingVessel != null) {
            // 정적 데이터가 있으면 이름과 타입 업데이트
            val name = if (vessel.name.startsWith("MMSI:")) {
                staticDataCache[vessel.mmsi]?.first ?: vessel.name
            } else {
                vessel.name
            }
            
            // 타입: 정적 데이터 메시지에서 온 타입이 있으면 사용, 없으면 캐시에서 가져오기, 둘 다 없으면 OTHER
            val type = if (vessel.type != VesselType.OTHER) {
                // 정적 데이터 메시지에서 온 실제 타입
                vessel.type
            } else {
                // 캐시에서 가져오기, 없으면 OTHER
                staticDataCache[vessel.mmsi]?.second ?: VesselType.OTHER
            }
            
            // 위치 정보 업데이트 (더 최신 데이터 사용, 위치가 있으면 업데이트)
            vessel.copy(
                name = name,
                type = type,
                isWatchlisted = existingVessel.isWatchlisted,
                latitude = vessel.latitude ?: existingVessel.latitude,
                longitude = vessel.longitude ?: existingVessel.longitude
            )
        } else {
            // 새 선박: 정적 데이터가 있으면 사용, 없으면 OTHER
            val type = if (vessel.type != VesselType.OTHER) {
                vessel.type
            } else {
                staticDataCache[vessel.mmsi]?.second ?: VesselType.OTHER
            }
            vessel.copy(type = type)
        }
        
        // 정적 데이터 캐시 업데이트 (이름과 타입이 실제 데이터인 경우)
        if (!updatedVessel.name.startsWith("MMSI:") && updatedVessel.type != VesselType.OTHER) {
            staticDataCache[updatedVessel.mmsi] = Pair(updatedVessel.name, updatedVessel.type)
        }
        
        currentVessels[updatedVessel.mmsi] = updatedVessel
        _vessels.value = currentVessels
        _vesselsList.value = currentVessels.values.toList()
        
        // 위험 이벤트 생성
        checkAndCreateRiskEvent(updatedVessel, existingVessel)
    }
    
    /**
     * 위험 이벤트 확인 및 생성
     */
    private fun checkAndCreateRiskEvent(
        currentVessel: AISVessel,
        previousVessel: AISVessel?
    ) {
        // 위험 수준이 CRITICAL 또는 WARNING인 경우 이벤트 생성
        if (currentVessel.riskLevel == RiskLevel.CRITICAL || currentVessel.riskLevel == RiskLevel.WARNING) {
            // 이전 이벤트와 동일한 선박이고 시간이 가까우면 업데이트, 아니면 새로 생성
            val recentEvent = _events.value.firstOrNull {
                it.vesselId == currentVessel.id &&
                (System.currentTimeMillis() - it.timestamp) < 60000 // 1분 이내
            }
            
            if (recentEvent == null) {
                val newEvent = RiskEvent(
                    id = "event_${currentVessel.mmsi}_${System.currentTimeMillis()}",
                    timestamp = System.currentTimeMillis(),
                    vesselId = currentVessel.id,
                    vesselName = currentVessel.name,
                    cpa = currentVessel.cpa,
                    tcpa = currentVessel.tcpa,
                    riskLevel = currentVessel.riskLevel,
                    description = generateRiskDescription(currentVessel)
                )
                
                _events.value = (listOf(newEvent) + _events.value).take(100) // 최근 100개만 유지
            }
        }
    }
    
    /**
     * 위험 설명 생성
     */
    private fun generateRiskDescription(vessel: AISVessel): String {
        return when (vessel.riskLevel) {
            RiskLevel.CRITICAL -> "즉시 위험: CPA ${String.format("%.1f", vessel.cpa)} 해리, TCPA ${vessel.tcpa}분"
            RiskLevel.WARNING -> "주의 필요: CPA ${String.format("%.1f", vessel.cpa)} 해리, TCPA ${vessel.tcpa}분"
            RiskLevel.SAFE -> "정상"
        }
    }
    
    /**
     * 관심 선박 토글
     */
    override fun toggleWatchlist(vesselId: String) {
        val currentVessels = _vessels.value.toMutableMap()
        val vessel = currentVessels.values.find { it.id == vesselId }
        if (vessel != null) {
            val updatedVessel = vessel.copy(isWatchlisted = !vessel.isWatchlisted)
            currentVessels[vessel.mmsi] = updatedVessel
            _vessels.value = currentVessels
            _vesselsList.value = currentVessels.values.toList()
        }
    }
    
    /**
     * 정리
     */
    override fun cleanup() {
        recalculationJob?.cancel()
        disconnect()
        dataSource.cleanup()
        scope.cancel()
    }
}

