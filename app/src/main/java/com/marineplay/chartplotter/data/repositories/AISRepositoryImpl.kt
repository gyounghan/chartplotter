package com.marineplay.chartplotter.data.repositories

import android.util.Log
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
import java.util.regex.Pattern

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
    
    // 멀티 프래그먼트 메시지 버퍼
    // Key: sequentialMessageId, Value: Map<fragmentIndex, Pair<payload, padding>>
    private val fragmentBuffer = mutableMapOf<String, MutableMap<Int, Pair<String, Int>>>()
    private val fragmentBufferTimestamp = mutableMapOf<String, Long>()
    private val fragmentBufferTotalFragments = mutableMapOf<String, Int>()
    private val FRAGMENT_TIMEOUT_MS = 10000L // 10초 타임아웃
    
    // A/B 채널 간 위치 오차 필터링: 같은 MMSI가 500ms 이내에 오면 무시 (parseNMEAMessage 전에 적용)
    private val POSITION_UPDATE_MIN_INTERVAL_MS = 500L // 500ms 이내 중복 업데이트 방지
    private val mmsiLastProcessedTime = mutableMapOf<String, Long>() // MMSI -> 마지막 처리 시간 (parseNMEAMessage 전 필터링용)
    
    // 의미 있는 이동 필터: 5m 이내 이동은 GPS 노이즈로 간주하여 display 좌표 업데이트 스킵
    private val MEANINGFUL_MOVE_DISTANCE_METERS = 5.0 // 5m 이상 이동만 의미 있음
    
    // 완전한 NMEA 메시지 정규식: !AIVDM 또는 !AIVDO로 시작하고 체크섬으로 끝나는 메시지
    private val nmeaMessagePattern = Pattern.compile("^!AIVD[MO],.*\\*[0-9A-Fa-f]{2}\\r?\\n?$")
    
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
     * 소수점 자릿수 제한 (반올림)
     */
    private fun roundToDecimalPlaces(value: Double, decimalPlaces: Int): Double {
        val multiplier = 10.0.pow(decimalPlaces)
        return round(value * multiplier) / multiplier
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
        // 메시지 수신 콜백 설정 (중복 방지를 위해 콜백만 사용)
        dataSource.setOnMessageReceived { message ->
            processNMEAMessage(message)
        }
    }
    
    /**
     * NMEA 체크섬 검증
     * NMEA 메시지에서 '$' 또는 '!' 다음부터 '*' 전까지의 모든 문자를 XOR 연산하여 체크섬 계산
     */
    private fun validateChecksum(message: String): Boolean {
        val checksumIndex = message.indexOf("*")
        if (checksumIndex == -1) {
            return false // 체크섬 구분자가 없음
        }
        
        // 체크섬 값 추출 (16진수 2자리)
        if (checksumIndex + 2 >= message.length) {
            return false // 체크섬 값이 없음
        }
        
        val expectedChecksum = message.substring(checksumIndex + 1, checksumIndex + 3)
        
        // 데이터 부분 추출 ('!' 또는 '$' 다음부터 '*' 전까지)
        val dataStart = if (message.startsWith("!")) 1 else if (message.startsWith("$")) 1 else 0
        val dataPart = message.substring(dataStart, checksumIndex)
        
        // XOR 체크섬 계산
        var checksum = 0
        for (char in dataPart) {
            checksum = checksum xor char.code
        }
        
        val calculatedChecksum = String.format("%02X", checksum)
        return calculatedChecksum.equals(expectedChecksum, ignoreCase = true)
    }
    
    /**
     * 6비트 ASCII 디코딩 (MMSI 추출용)
     */
    private fun decode6BitASCII(data: String): String {
        val builder = StringBuilder()
        for (char in data) {
            val value = if (char <= 'W') {
                char.code - 48
            } else {
                char.code - 56
            }
            if (value < 0 || value > 63) {
                continue
            }
            val binary = Integer.toBinaryString(value)
            builder.append(binary.padStart(6, '0'))
        }
        return builder.toString()
    }
    
    /**
     * NMEA 메시지에서 MMSI를 빠르게 추출 (parseNMEAMessage 전 필터링용)
     * Position Report (타입 1,2,3)만 지원
     */
    private fun extractMMSIFromMessage(message: String): String? {
        try {
            val parts = message.split(",")
            if (parts.size < 6) return null
            
            val encodedData = parts[5]
            if (encodedData.isEmpty()) return null
            
            val padding = if (parts.size > 6) {
                parts[6].split("*").firstOrNull()?.toIntOrNull() ?: 0
            } else {
                0
            }
            
            // 6비트 ASCII 디코딩
            var binaryData = decode6BitASCII(encodedData)
            
            // padding 제거
            if (padding > 0 && binaryData.isNotEmpty() && padding < binaryData.length) {
                binaryData = binaryData.substring(0, binaryData.length - padding)
            }
            
            if (binaryData.length < 38) return null // 최소 타입(6) + repeat(2) + MMSI(30)
            
            // 메시지 타입 확인
            val messageType = binaryData.substring(0, 6).toIntOrNull(2) ?: return null
            if (messageType !in 1..3) return null // Position Report만
            
            // MMSI 추출 (bit 8부터 30비트)
            if (binaryData.length < 38) return null
            val mmsiBits = binaryData.substring(8, 38)
            return mmsiBits.toLongOrNull(2)?.toString()
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * NMEA 메시지 처리 (멀티 프래그먼트 지원)
     */
    private fun processNMEAMessage(message: String) {
        try {
            // 정규식으로 완전한 NMEA 메시지 검증
            val trimmed = message.trim()
            if (!nmeaMessagePattern.matcher(trimmed).matches()) {
                return // 완전한 메시지가 아님
            }
            
            // 체크섬 검증
            if (!validateChecksum(trimmed)) {
                // Log.w("[AISRepositoryImpl]", "체크섬 검증 실패: ${trimmed.take(80)}...")
                return // 체크섬이 유효하지 않음
            }

            val parts = trimmed.split(",")
            if (parts.size < 6) {
                return
            }
            
            val totalFragments = parts[1].toIntOrNull() ?: 1
            val fragmentIndex = parts[2].toIntOrNull() ?: 1
            val sequentialMessageId = parts[3].ifEmpty { "default" } // 빈 문자열이면 기본값
            val channel = parts.getOrNull(4).orEmpty()
            val encodedData = parts[5]
            // parts[6]은 "padding*checksum" 형태가 일반적이므로 '*' 이전 숫자만 파싱해야 함
            val padding = if (parts.size > 6) {
                parts[6].split("*").firstOrNull()?.toIntOrNull() ?: 0
            } else {
                0
            }
            
            // 단일 프래그먼트 메시지 처리
            if (totalFragments == 1) {
                // MMSI 기반 필터링 (parseNMEAMessage 전에 적용)
                val mmsi = extractMMSIFromMessage(trimmed)
                if (mmsi != null) {
                    val now = System.currentTimeMillis()
                    val lastProcessedTime = mmsiLastProcessedTime[mmsi]
                    if (lastProcessedTime != null && (now - lastProcessedTime) < POSITION_UPDATE_MIN_INTERVAL_MS) {
                        // 같은 MMSI가 500ms 이내에 오면 무시 (A/B 채널 중복 방지)
                        return
                    }
                    mmsiLastProcessedTime[mmsi] = now
                }
                
                val vessel = AISNMEAParser.parseNMEAMessage(
                    trimmed, // 정규식으로 검증된 메시지 사용
                    currentLatitude,
                    currentLongitude
                )
                if (vessel != null) {
                    updateVessel(vessel)
                }
                return
            }
            
            // 멀티 프래그먼트 메시지 처리
            // fragment buffer key 개선: seqId + channel + payload 일부 (충돌 방지)
            val bufferKey = "$sequentialMessageId|$channel|${encodedData.take(6)}"
            val now = System.currentTimeMillis()
            
            // 타임아웃된 프래그먼트 제거
            fragmentBufferTimestamp.entries.removeAll { (key, timestamp) ->
                if (now - timestamp > FRAGMENT_TIMEOUT_MS) {
                    fragmentBuffer.remove(key)
                    fragmentBufferTotalFragments.remove(key)
                    true
                } else {
                    false
                }
            }
            
            // 프래그먼트 버퍼에 추가
            if (!fragmentBuffer.containsKey(bufferKey)) {
                fragmentBuffer[bufferKey] = mutableMapOf()
                fragmentBufferTimestamp[bufferKey] = now
                fragmentBufferTotalFragments[bufferKey] = totalFragments
            }
            
            // 프래그먼트 저장 (payload와 padding 정보)
            fragmentBuffer[bufferKey]!![fragmentIndex] = Pair(encodedData, padding)
            
            // 모든 프래그먼트가 수집되었는지 확인
            val expectedTotal = fragmentBufferTotalFragments[bufferKey] ?: totalFragments
            if (fragmentBuffer[bufferKey]!!.size == expectedTotal) {
                // 프래그먼트를 인덱스 순서대로 정렬하여 결합
                val sortedFragments = fragmentBuffer[bufferKey]!!
                    .toList()
                    .sortedBy { it.first }
                    .map { it.second }
                
                // 모든 프래그먼트의 payload 결합 (중간 프래그먼트는 padding 0)
                val completePayload = sortedFragments.mapIndexed { index, (payload, fragPadding) ->
                    // 마지막 프래그먼트만 padding 정보 사용, 나머지는 padding 0
                    if (index == sortedFragments.size - 1) {
                        payload // 마지막 프래그먼트는 padding 정보와 함께 전달
                    } else {
                        payload // 중간 프래그먼트는 padding 없음
                    }
                }.joinToString("")
                
                // 마지막 프래그먼트의 padding 정보 사용
                val lastPadding = sortedFragments.lastOrNull()?.second ?: padding
                val completeMessage = assembleMessage(completePayload, lastPadding, channel)
                
                android.util.Log.d("[AISRepositoryImpl]", "멀티 프래그먼트 메시지 조합 완료: seqId=$bufferKey, fragments=${sortedFragments.size}, padding=$lastPadding, channel=$channel")
                
                // 조합된 메시지도 체크섬 검증
                if (!validateChecksum(completeMessage)) {
                    // Log.w("[AISRepositoryImpl]", "멀티 프래그먼트 조합 메시지 체크섬 검증 실패: seqId=$bufferKey")
                    // 버퍼에서 제거
                    fragmentBuffer.remove(bufferKey)
                    fragmentBufferTimestamp.remove(bufferKey)
                    fragmentBufferTotalFragments.remove(bufferKey)
                    return
                }
                
                // MMSI 기반 필터링 (parseNMEAMessage 전에 적용)
                val mmsi = extractMMSIFromMessage(completeMessage)
                if (mmsi != null) {
                    val now = System.currentTimeMillis()
                    val lastProcessedTime = mmsiLastProcessedTime[mmsi]
                    if (lastProcessedTime != null && (now - lastProcessedTime) < POSITION_UPDATE_MIN_INTERVAL_MS) {
                        // 같은 MMSI가 500ms 이내에 오면 무시 (A/B 채널 중복 방지)
                        fragmentBuffer.remove(bufferKey)
                        fragmentBufferTimestamp.remove(bufferKey)
                        fragmentBufferTotalFragments.remove(bufferKey)
                        return
                    }
                    mmsiLastProcessedTime[mmsi] = now
                }
                
                // 완전한 메시지 파싱
                val vessel = AISNMEAParser.parseNMEAMessage(
                    completeMessage,
                    currentLatitude,
                    currentLongitude
                )
                if (vessel != null) {
                    updateVessel(vessel)
                }
                
                // 버퍼에서 제거
                fragmentBuffer.remove(bufferKey)
                fragmentBufferTimestamp.remove(bufferKey)
                fragmentBufferTotalFragments.remove(bufferKey)
            } else {
                android.util.Log.d("[AISRepositoryImpl]", "프래그먼트 수집 중: seqId=$bufferKey, 현재=${fragmentBuffer[bufferKey]!!.size}/$expectedTotal")
            }
        } catch (e: Exception) {
            android.util.Log.e("[AISRepositoryImpl]", "메시지 처리 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * 완전한 NMEA 메시지 조립 (프래그먼트 결합 후)
     * padding은 마지막 프래그먼트에만 적용되므로, 결합된 데이터에서 padding 비트 제거
     * 원래 채널을 유지 (A 또는 B)
     */
    private fun assembleMessage(encodedData: String, padding: Int, channel: String): String {
        // 메시지 구성: !AIVDM,1,1,,채널,데이터,패딩 (원래 채널 유지)
        val channelPart = if (channel.isNotEmpty()) channel else "A" // 기본값 A
        val messageWithoutChecksum = "!AIVDM,1,1,,$channelPart,$encodedData,$padding"
        
        // 체크섬 계산 ('!' 다음부터 '*' 전까지 XOR)
        var checksum = 0
        for (i in 1 until messageWithoutChecksum.length) {
            checksum = checksum xor messageWithoutChecksum[i].code
        }
        val checksumHex = String.format("%02X", checksum)
        
        // 완전한 메시지 반환
        return "$messageWithoutChecksum*$checksumHex"
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
     * A/B 채널 간 위치 오차로 인한 깜빡임을 방지하기 위해 위치 필터링 적용
     */
    private fun updateVessel(vessel: AISVessel) {
        val currentVessels = _vessels.value.toMutableMap()
        val existingVessel = currentVessels[vessel.mmsi]
        val now = System.currentTimeMillis()
        
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
            
            // 위치 정보 업데이트 (raw 좌표는 항상 업데이트, display 좌표는 의미 있는 이동만 smoothing 적용)
            val finalLat: Double?
            val finalLon: Double?
            val displayLat: Double?
            val displayLon: Double?
            
            if (vessel.latitude != null && vessel.longitude != null) {
                // 새 위치가 있는 경우
                val rawLat = vessel.latitude!!
                val rawLon = vessel.longitude!!
                
                // Raw 좌표는 항상 업데이트 (내부 상태, 소수점 6자리로 제한)
                finalLat = roundToDecimalPlaces(rawLat, 6)
                finalLon = roundToDecimalPlaces(rawLon, 6)
                
                // Display 좌표: 의미 있는 이동인지 확인
                val prevDisplayLat = existingVessel.displayLatitude ?: existingVessel.latitude
                val prevDisplayLon = existingVessel.displayLongitude ?: existingVessel.longitude
                
                if (prevDisplayLat != null && prevDisplayLon != null) {
                    // 기존 display 좌표와 새 raw 좌표 간 거리 계산
                    val moveDistance = calculateDistance(prevDisplayLat, prevDisplayLon, rawLat, rawLon)
                    
                    if (moveDistance >= MEANINGFUL_MOVE_DISTANCE_METERS) {
                        // 5m 이상 이동: smoothing 적용 (0.8 * 기존 + 0.2 * 새), 소수점 6자리로 제한
                        displayLat = roundToDecimalPlaces(prevDisplayLat * 0.8 + rawLat * 0.2, 6)
                        displayLon = roundToDecimalPlaces(prevDisplayLon * 0.8 + rawLon * 0.2, 6)
                    } else {
                        // 5m 이내 이동: GPS 노이즈로 간주, display 좌표는 기존 값 유지
                        displayLat = prevDisplayLat
                        displayLon = prevDisplayLon
                    }
                } else {
                    // 기존 display 좌표가 없으면 raw 좌표로 초기화 (소수점 6자리로 제한)
                    displayLat = finalLat
                    displayLon = finalLon
                }
            } else {
                // 새 위치가 없으면 기존 위치 유지
                finalLat = existingVessel.latitude
                finalLon = existingVessel.longitude
                displayLat = existingVessel.displayLatitude
                displayLon = existingVessel.displayLongitude
            }
            
            vessel.copy(
                name = name,
                type = type,
                isWatchlisted = existingVessel.isWatchlisted,
                latitude = finalLat,
                longitude = finalLon,
                displayLatitude = displayLat,
                displayLongitude = displayLon
            )
        } else {
            // 새 선박: 정적 데이터가 있으면 사용, 없으면 OTHER
            val type = if (vessel.type != VesselType.OTHER) {
                vessel.type
            } else {
                staticDataCache[vessel.mmsi]?.second ?: VesselType.OTHER
            }
            // 새 선박: display 좌표는 raw 좌표와 동일하게 초기화
            vessel.copy(
                type = type,
                displayLatitude = vessel.latitude,
                displayLongitude = vessel.longitude
            )
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

