package com.marineplay.chartplotter.data.parsers

import android.util.Log
import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.domain.entities.RiskLevel
import com.marineplay.chartplotter.domain.entities.VesselType
import kotlin.math.*

/**
 * AIS NMEA 메시지 파서
 * NMEA 0183 형식의 AIS 메시지(!AIVDM, !AIVDO)를 파싱하여 Domain Entity로 변환
 * Data 레이어에 위치하여 외부 데이터를 Domain Entity로 변환
 */
object AISNMEAParser {
    
    /**
     * 소수점 자릿수 제한 (반올림)
     */
    private fun roundToDecimalPlaces(value: Double, decimalPlaces: Int): Double {
        val multiplier = 10.0.pow(decimalPlaces)
        return round(value * multiplier) / multiplier
    }
    
    /**
     * NMEA 메시지를 파싱하여 AISVessel로 변환
     * @param nmeaMessage NMEA 형식의 메시지 (예: "!AIVDM,1,1,,A,133m@ogP00PD;88MD5MTDww@2D7D,0*46")
     * @param currentLatitude 현재 위치 위도 (null이면 거리 계산 불가)
     * @param currentLongitude 현재 위치 경도 (null이면 거리 계산 불가)
     * @return 파싱된 AISVessel 또는 null (파싱 실패 시)
     */
    fun parseNMEAMessage(
        nmeaMessage: String,
        currentLatitude: Double? = null,
        currentLongitude: Double? = null
    ): AISVessel? {
        try {
            // NMEA 메시지 형식: !AIVDM,총_메시지_수,메시지_번호,순차_메시지_ID,채널,데이터,패딩
            if (!nmeaMessage.startsWith("!AIVDM") && !nmeaMessage.startsWith("!AIVDO")) {
                return null
            }
            
            // 완전한 메시지인지 확인 (체크섬 구분자 '*'가 있어야 함)
            if (!nmeaMessage.contains("*")) {
                // 아직 완성되지 않은 메시지 - 조용히 무시
                return null
            }
            
            val parts = nmeaMessage.split(",")
            if (parts.size < 6) {
                return null
            }
            val encodedData = parts[5]
            if (encodedData.isEmpty()) {
                return null
            }
            
            Log.d("[AISNMEAParser]", "NMEA 메시지 parts: $parts")
            // padding 정보 추출 (parts[6]에 있음, 없으면 0)
            val padding = if (parts.size > 6) {
                // parts[6]은 "패딩*체크섬" 형태이므로 '*' 이전 숫자만 추출
                parts[6].split("*").firstOrNull()?.toIntOrNull() ?: 0
            } else {
                0
            }
            
            // 6비트 ASCII 인코딩된 데이터를 바이너리로 디코딩
            var binaryData = decode6BitASCII(encodedData)
            
            // padding 비트 제거 (마지막 padding 비트만큼 제거)
            if (padding > 0 && binaryData.isNotEmpty()) {
                val bitsToRemove = padding
                if (bitsToRemove < binaryData.length) {
                    binaryData = binaryData.substring(0, binaryData.length - bitsToRemove)
                }
            }
            if (binaryData.isEmpty()) {
                return null
            }
            
            // 메시지 타입 확인 전에 최소 길이 검증 (최소 6비트 필요)
            if (binaryData.length < 6) {
                Log.w("[AISNMEAParser]", "바이너리 데이터가 너무 짧습니다: ${binaryData.length} 비트 (메시지 타입 확인 불가)")
                return null
            }
            
            // 메시지 타입 확인 (첫 6비트)
            val messageType = try {
                binaryData.substring(0, 6).toInt(2)
            } catch (e: Exception) {
                Log.w("[AISNMEAParser]", "메시지 타입 파싱 실패: ${e.message}, binaryData 길이: ${binaryData.length}")
                return null
            }
            
            // 메시지 타입별 최소 길이 검증
            when (messageType) {
                1, 2, 3 -> {
                    // Position Report는 최소 168비트 필요
                    if (binaryData.length < 168) {
                        // Log.w("[AISNMEAParser]", "Position Report 메시지가 너무 짧습니다: 타입=$messageType, 길이=${binaryData.length} 비트 (최소 168비트 필요)")
                        return null
                    }
                    return parsePositionReport(binaryData, currentLatitude, currentLongitude)
                }
                5 -> {
                    // Static Data는 최소 424비트 필요
                    if (binaryData.length < 424) {
                        // Log.w("[AISNMEAParser]", "Static Data 메시지가 너무 짧습니다: 길이=${binaryData.length} 비트 (최소 424비트 필요)")
                        return null
                    }
                    return parseStaticData(binaryData)
                }
                else -> {
                    // Log.d("[AISNMEAParser]", "지원하지 않는 메시지 타입: $messageType (길이: ${binaryData.length} 비트)")
                    return null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 위치 보고 메시지 파싱 (타입 1, 2, 3)
     */
    private fun parsePositionReport(
        binaryData: String,
        currentLatitude: Double?,
        currentLongitude: Double?
    ): AISVessel? {
        try {
            // Type 1/2/3 (Position Report): msgType(6) + repeat(2) 이후부터 시작 => MMSI는 bit 8부터
            var bitIndex = 8
            
            // 최소 길이 검증 (Position Report는 최소 168비트 필요)
            // 이미 parseNMEAMessage에서 검증했지만, 추가 안전장치
            if (binaryData.length < 168) {
                Log.w("[AISNMEAParser]", "Position Report 메시지 길이가 너무 짧습니다: ${binaryData.length} 비트 (최소 168비트 필요)")
                return null
            }
            
            // MMSI (30비트)
            if (bitIndex + 30 > binaryData.length) {
                Log.e("[AISNMEAParser]", "MMSI 파싱 실패: bitIndex=$bitIndex, length=${binaryData.length}")
                return null
            }
            // MMSI는 30bit이며 Int 범위를 넘지 않지만 안전하게 Long으로 변환 후 문자열화
            val mmsi = binaryData.substring(bitIndex, bitIndex + 30).toLong(2).toString()
            bitIndex += 30
            
            // 내비게이션 상태 (4비트)
            if (bitIndex + 4 > binaryData.length) return null
            bitIndex += 4
            
            // 회전율 (8비트)
            if (bitIndex + 8 > binaryData.length) return null
            bitIndex += 8
            
            // 속도 (10비트, 0.1 노트 단위)
            // 1023 = not available (데이터 없음)
            if (bitIndex + 10 > binaryData.length) return null
            val speedRaw = binaryData.substring(bitIndex, bitIndex + 10).toInt(2)
            val speed = if (speedRaw == 1023) {
                null // 데이터 없음 (0.0으로 처리하지 않음)
            } else {
                speedRaw / 10.0
            }
            bitIndex += 10
            
            // 위치 정확도 (1비트)
            if (bitIndex + 1 > binaryData.length) return null
            bitIndex += 1
            
            // 경도 (28비트 signed integer, 1/10000 minute 단위)
            // degree = raw / 600000.0
            if (bitIndex + 28 > binaryData.length) {
                Log.w("[AISNMEAParser]", "경도 비트 추출 실패: bitIndex=$bitIndex, length=${binaryData.length}")
                return null
            }
            val longitudeRawBits = binaryData.substring(bitIndex, bitIndex + 28)
            // Signed integer 변환: 최상위 비트가 1이면 음수
            val longitudeRaw = if (longitudeRawBits[0] == '1') {
                // 음수: 2의 보수 변환
                val unsigned = longitudeRawBits.toLong(2)
                val signed = unsigned - (1L shl 28)
                signed.toInt()
            } else {
                longitudeRawBits.toInt(2)
            }
            // Invalid longitude = 0x6791AC0 (181 deg in 1/10000 minute)
            val longitudeDeg = if (longitudeRaw == 0x6791AC0.toInt()) {
                null
            } else {
                val lon = longitudeRaw / 600000.0
                // 경도 범위 검증: -180 ~ 180
                if (lon < -180.0 || lon > 180.0) {
                    Log.w("[AISNMEAParser]", "잘못된 경도 값: $lon (raw: $longitudeRaw, bits: $longitudeRawBits, binaryLength: ${binaryData.length})")
                    null
                } else {
                    // 소수점 6자리로 제한 (약 10cm 정밀도)
                    roundToDecimalPlaces(lon, 6)
                }
            }
            bitIndex += 28
            
            // 위도 (27비트 signed integer, 1/10000 minute 단위)
            // degree = raw / 600000.0
            if (bitIndex + 27 > binaryData.length) {
                Log.w("[AISNMEAParser]", "위도 비트 추출 실패: bitIndex=$bitIndex, length=${binaryData.length}")
                return null
            }
            val latitudeRawBits = binaryData.substring(bitIndex, bitIndex + 27)
            // Signed integer 변환: 최상위 비트가 1이면 음수
            val latitudeRaw = if (latitudeRawBits[0] == '1') {
                // 음수: 2의 보수 변환
                val unsigned = latitudeRawBits.toLong(2)
                val signed = unsigned - (1L shl 27)
                signed.toInt()
            } else {
                latitudeRawBits.toInt(2)
            }
            // Invalid latitude = 0x3412140 (91 deg in 1/10000 minute)
            val latitudeDeg = if (latitudeRaw == 0x3412140.toInt()) {
                null
            } else {
                val lat = latitudeRaw / 600000.0
                // 위도 범위 검증: -90 ~ 90
                if (lat < -90.0 || lat > 90.0) {
                    Log.w("[AISNMEAParser]", "잘못된 위도 값: $lat (raw: $latitudeRaw, bits: $latitudeRawBits, binaryLength: ${binaryData.length})")
                    null
                } else {
                    // 소수점 6자리로 제한 (약 10cm 정밀도)
                    roundToDecimalPlaces(lat, 6)
                }
            }
            bitIndex += 27

            // 위/경도 파싱 성공 로그를 한번에 출력 (MMSI 포함)
            if (latitudeDeg != null && longitudeDeg != null) {
                Log.d(
                    "[AISNMEAParser]",
                    "좌표 파싱 성공 (MMSI=$mmsi): lat=$latitudeDeg (raw=$latitudeRaw bits=$latitudeRawBits), lon=$longitudeDeg (raw=$longitudeRaw bits=$longitudeRawBits)"
                )
            }
            
            // 침로 (12비트, 0.1도 단위)
            if (bitIndex + 12 > binaryData.length) return null
            val cogRaw = binaryData.substring(bitIndex, bitIndex + 12).toInt(2)
            val course = if (cogRaw == 3600) 0.0 else cogRaw / 10.0
            bitIndex += 12
            
            // 선박 타입은 정적 데이터 메시지(타입 5)에서만 제공되므로 기본값으로 OTHER 사용
            val vesselType = VesselType.OTHER
            
            // 거리, 베어링, CPA, TCPA 계산
            // speed가 null이면 (1023 = not available) CPA/TCPA 계산 불가
            val (distance, bearing, cpa, tcpa) = if (currentLatitude != null && currentLongitude != null && 
                latitudeDeg != null && longitudeDeg != null && speed != null) {
                calculateVesselMetrics(
                    currentLatitude!!,
                    currentLongitude!!,
                    latitudeDeg,
                    longitudeDeg,
                    speed,
                    course
                )
            } else {
                // 위치 정보만으로 거리와 베어링 계산
                if (currentLatitude != null && currentLongitude != null && 
                    latitudeDeg != null && longitudeDeg != null) {
                    val distanceMeters = calculateDistance(currentLatitude!!, currentLongitude!!, latitudeDeg, longitudeDeg)
                    val distanceNauticalMiles = distanceMeters / 1852.0
                    val bearing = calculateBearing(currentLatitude!!, currentLongitude!!, latitudeDeg, longitudeDeg).toInt()
                    // speed가 없으면 CPA/TCPA 계산 불가
                    Quadruple(distanceNauticalMiles, bearing, Double.MAX_VALUE, Int.MAX_VALUE)
                } else {
                    Quadruple(0.0, 0, 0.0, 0)
                }
            }
            val riskLevel = calculateRiskLevel(distance, cpa, tcpa)
            
            return AISVessel(
                id = "ais_$mmsi",
                name = "MMSI:$mmsi", // 정적 데이터 메시지에서 이름을 받아야 함
                mmsi = mmsi,
                type = vesselType,
                distance = distance,
                bearing = bearing,
                speed = speed ?: 0.0, // null이면 0.0으로 저장 (표시용, 계산에는 사용 안 함)
                course = course.toInt(),
                cpa = cpa,
                tcpa = tcpa,
                riskLevel = riskLevel,
                isWatchlisted = false,
                lastUpdate = System.currentTimeMillis(),
                latitude = latitudeDeg,
                longitude = longitudeDeg
            )
        } catch (e: Exception) {
            Log.e("[AISNMEAParser]", "parsePositionReport 실패: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 정적 데이터 메시지 파싱 (타입 5)
     */
    private fun parseStaticData(binaryData: String): AISVessel? {
        try {
            // Type 5 (Static and Voyage Related Data): msgType(6) + repeat(2) + MMSI(30) ...
            // MMSI는 bit 8부터 시작해야 함
            var bitIndex = 8
            
            // 최소 길이 검증 (메시지 타입 6 + MMSI 30 + 버전 2 + IMO 30 + 호출부호 42 + 이름 120 = 230비트)
            if (binaryData.length < 230) {
                Log.e("[AISNMEAParser]", "Static Data 메시지 길이가 너무 짧습니다: ${binaryData.length} 비트")
                return null
            }
            
            // MMSI (30비트)
            if (bitIndex + 30 > binaryData.length) return null
            val mmsi = binaryData.substring(bitIndex, bitIndex + 30).toLong(2).toString()
            bitIndex += 30
            
            // AIS 버전 (2비트)
            if (bitIndex + 2 > binaryData.length) return null
            bitIndex += 2
            
            // IMO 번호 (30비트)
            if (bitIndex + 30 > binaryData.length) return null
            bitIndex += 30
            
            // 호출 부호 (42비트, 7자)
            if (bitIndex + 42 > binaryData.length) return null
            bitIndex += 42
            
            // 선박 이름 (120비트, 20자)
            if (bitIndex + 120 > binaryData.length) {
                Log.e("[AISNMEAParser]", "선박 이름 파싱 실패: bitIndex=$bitIndex, length=${binaryData.length}")
                return null
            }
            val nameBytes = binaryData.substring(bitIndex, bitIndex + 120)
            val name = decode6BitString(nameBytes).trim { it <= ' ' }
            bitIndex += 120
            
            // 선박 타입 (8비트)
            if (bitIndex + 8 > binaryData.length) return null
            val shipTypeCode = binaryData.substring(bitIndex, bitIndex + 8).toInt(2)
            val vesselType = mapShipTypeCode(shipTypeCode)
            bitIndex += 8
            
            // 기본 선박 정보만 반환 (위치는 위치 보고 메시지에서 받아야 함)
            return AISVessel(
                id = "ais_$mmsi",
                name = if (name.isNotEmpty()) name else "MMSI:$mmsi",
                mmsi = mmsi,
                type = vesselType,
                distance = 0.0,
                bearing = 0,
                speed = 0.0,
                course = 0,
                cpa = 0.0,
                tcpa = 0,
                riskLevel = RiskLevel.SAFE,
                isWatchlisted = false,
                lastUpdate = System.currentTimeMillis(),
                latitude = null,
                longitude = null
            )
        } catch (e: Exception) {
            Log.e("[AISNMEAParser]", "parsePositionReport 실패: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 선박 메트릭 계산 (거리, 베어링, CPA, TCPA)
     */
    private fun calculateVesselMetrics(
        currentLat: Double,
        currentLon: Double,
        vesselLat: Double,
        vesselLon: Double,
        vesselSpeed: Double, // 노트
        vesselCourse: Double // 도
    ): Quadruple<Double, Int, Double, Int> {
        // 거리 계산 (해리 단위)
        val distanceMeters = calculateDistance(currentLat, currentLon, vesselLat, vesselLon)
        val distanceNauticalMiles = distanceMeters / 1852.0
        
        // 베어링 계산 (도)
        val bearing = calculateBearing(currentLat, currentLon, vesselLat, vesselLon).toInt()
        
        // CPA (Closest Point of Approach) 계산
        // 간단한 선형 추정 사용 (실제로는 더 정교한 알고리즘 필요)
        val cpa = calculateCPA(
            currentLat, currentLon, 0.0, 0.0, // 현재 선박 위치 및 속도/침로 (0으로 가정)
            vesselLat, vesselLon, vesselSpeed, vesselCourse
        )
        
        // TCPA (Time to Closest Point of Approach) 계산 (분)
        val tcpa = calculateTCPA(
            currentLat, currentLon, 0.0, 0.0,
            vesselLat, vesselLon, vesselSpeed, vesselCourse
        )
        
        return Quadruple(distanceNauticalMiles, bearing, cpa, tcpa)
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
     * 두 지점 간 베어링 계산 (도)
     */
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        
        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        
        return ((bearingDeg % 360) + 360) % 360
    }
    
    /**
     * CPA (Closest Point of Approach) 계산 (해리)
     * 간단한 선형 추정 사용
     */
    private fun calculateCPA(
        ownLat: Double, ownLon: Double, ownSpeed: Double, ownCourse: Double,
        targetLat: Double, targetLon: Double, targetSpeed: Double, targetCourse: Double
    ): Double {
        // 상대 속도 벡터 계산
        val ownSpeedKnots = ownSpeed // 이미 노트 단위
        val targetSpeedKnots = targetSpeed // 이미 노트 단위
        
        // 현재 거리
        val currentDistance = calculateDistance(ownLat, ownLon, targetLat, targetLon) / 1852.0 // 해리로 변환
        
        // 간단한 추정: 상대 속도가 0이면 현재 거리가 CPA
        if (ownSpeedKnots == 0.0 && targetSpeedKnots == 0.0) {
            return currentDistance
        }
        
        // 더 정교한 계산을 위해서는 상대 속도 벡터를 계산해야 하지만,
        // 여기서는 간단히 현재 거리의 일정 비율로 추정
        // 실제로는 두 선박의 상대 속도와 방향을 고려한 정확한 계산 필요
        return max(0.0, currentDistance * 0.8) // 임시 추정값
    }
    
    /**
     * TCPA (Time to Closest Point of Approach) 계산 (분)
     */
    private fun calculateTCPA(
        ownLat: Double, ownLon: Double, ownSpeed: Double, ownCourse: Double,
        targetLat: Double, targetLon: Double, targetSpeed: Double, targetCourse: Double
    ): Int {
        val currentDistance = calculateDistance(ownLat, ownLon, targetLat, targetLon) / 1852.0 // 해리
        
        // 상대 속도 계산 (간단한 추정)
        val relativeSpeed = abs(targetSpeed - ownSpeed) // 노트
        
        if (relativeSpeed == 0.0) {
            return Int.MAX_VALUE // 접근하지 않음
        }
        
        // 시간 = 거리 / 속도 (해리 / 노트 = 시간)
        val timeHours = currentDistance / relativeSpeed
        val timeMinutes = (timeHours * 60).toInt()
        
        return max(0, timeMinutes)
    }
    
    /**
     * 거리, CPA, TCPA로부터 위험 수준 계산
     */
    private fun calculateRiskLevel(distance: Double, cpa: Double, tcpa: Int): RiskLevel {
        // 거리, CPA, TCPA가 0이면 즉시 위험 (데이터 없음 또는 같은 위치)
        if (distance == 0.0 || cpa == 0.0 || tcpa == 0) {
            return RiskLevel.CRITICAL
        }
        
        // CPA/TCPA가 계산 불가능한 경우 (Double.MAX_VALUE, Int.MAX_VALUE)
        if (cpa >= Double.MAX_VALUE / 2 || tcpa >= Int.MAX_VALUE / 2) {
            // 속도 정보가 없어서 CPA/TCPA 계산 불가 - 거리만으로 판단
            return when {
                distance < 0.5 -> RiskLevel.CRITICAL
                distance < 2.0 -> RiskLevel.WARNING
                else -> RiskLevel.SAFE
            }
        }
        
        return when {
            cpa < 0.5 && tcpa < 10 -> RiskLevel.CRITICAL
            cpa < 2.0 && tcpa < 30 -> RiskLevel.WARNING
            else -> RiskLevel.SAFE
        }
    }
    
    /**
     * 선박 메트릭 재계산 (내 위치 변경 시 사용)
     * @param currentLat 현재 위도
     * @param currentLon 현재 경도
     * @param vesselLat 선박 위도
     * @param vesselLon 선박 경도
     * @param vesselSpeed 선박 속도 (노트)
     * @param vesselCourse 선박 침로 (도)
     * @return Triple<distance(해리), bearing(도), cpa(해리), tcpa(분), riskLevel>
     */
    fun recalculateVesselMetrics(
        currentLat: Double,
        currentLon: Double,
        vesselLat: Double?,
        vesselLon: Double?,
        vesselSpeed: Double,
        vesselCourse: Double
    ): Triple<Double, Int, Triple<Double, Int, RiskLevel>>? {
        // 선박 위치가 없으면 계산 불가
        if (vesselLat == null || vesselLon == null || 
            vesselLat.isNaN() || vesselLon.isNaN()) {
            return null
        }
        
        // 거리 계산 (해리 단위)
        val distanceMeters = calculateDistance(currentLat, currentLon, vesselLat, vesselLon)
        val distanceNauticalMiles = distanceMeters / 1852.0
        
        // 베어링 계산 (도)
        val bearing = calculateBearing(currentLat, currentLon, vesselLat, vesselLon).toInt()
        
        // CPA (Closest Point of Approach) 계산
        val cpa = calculateCPA(
            currentLat, currentLon, 0.0, 0.0, // 현재 선박 위치 및 속도/침로 (0으로 가정)
            vesselLat, vesselLon, vesselSpeed, vesselCourse
        )
        
        // TCPA (Time to Closest Point of Approach) 계산 (분)
        val tcpa = calculateTCPA(
            currentLat, currentLon, 0.0, 0.0,
            vesselLat, vesselLon, vesselSpeed, vesselCourse
        )
        
        // 위험 수준 계산
        val riskLevel = calculateRiskLevel(distanceNauticalMiles, cpa, tcpa)
        
        return Triple(distanceNauticalMiles, bearing, Triple(cpa, tcpa, riskLevel))
    }
    
    /**
     * 선박 타입 코드를 VesselType으로 매핑
     */
    private fun mapShipTypeCode(code: Int): VesselType {
        return when {
            code in 70..79 -> VesselType.CARGO
            code in 80..89 -> VesselType.TANKER
            code in 60..69 -> VesselType.PASSENGER
            code in 30..39 -> VesselType.FISHING
            code in 37..38 -> VesselType.PLEASURE
            else -> VesselType.OTHER
        }
    }
    
    /**
     * 6비트 ASCII를 바이너리 문자열로 디코딩
     * AIS 6-bit encoding 규칙 (ITU-R M.1371):
     * - 0-63: ASCII 48-87 (0-9, :, ;, <, =, >, ?, @, A-W)
     * - 64-127: ASCII 96-127 (`, a-w) -> 64-87로 매핑
     * 정확한 규칙: if (char <= 'W') value = char - 48, else value = char - 56
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
                Log.w("[AISNMEAParser]", "6비트 디코딩 실패: char='$char' (code=${char.code}), value=$value")
                continue
            }
            val binary = Integer.toBinaryString(value)
            builder.append(binary.padStart(6, '0'))
        }
        return builder.toString()
    }
    
    /**
     * 6비트 바이너리를 문자열로 디코딩
     */
    private fun decode6BitString(binaryData: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < binaryData.length) {
            if (i + 6 > binaryData.length) break
            val bits = binaryData.substring(i, i + 6)
            val value = bits.toInt(2)
            val char = when {
                value == 0 -> '@'
                value in 1..26 -> ('A' + value - 1).toChar()
                value in 27..31 -> ('0' + value - 27).toChar()
                value in 32..37 -> ('0' + value - 32).toChar()
                value in 38..63 -> '@'
                else -> '@'
            }
            if (char != '@') {
                builder.append(char)
            }
            i += 6
        }
        return builder.toString()
    }
}

/**
 * 4개의 값을 담는 데이터 클래스
 */
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

