package com.marineplay.chartplotter.data.parsers

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
            
            val parts = nmeaMessage.split(",")
            if (parts.size < 6) {
                return null
            }
            
            val encodedData = parts[5]
            if (encodedData.isEmpty()) {
                return null
            }
            
            // 6비트 ASCII 인코딩된 데이터를 바이너리로 디코딩
            val binaryData = decode6BitASCII(encodedData)
            if (binaryData.isEmpty()) {
                return null
            }
            
            // 메시지 타입 확인 (첫 6비트)
            val messageType = binaryData.substring(0, 6).toInt(2)
            
            return when (messageType) {
                1, 2, 3 -> parsePositionReport(binaryData, currentLatitude, currentLongitude)
                5 -> parseStaticData(binaryData)
                else -> null
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
            var bitIndex = 6 // 메시지 타입 이후부터 시작
            
            // MMSI (30비트)
            val mmsi = binaryData.substring(bitIndex, bitIndex + 30).toInt(2).toString()
            bitIndex += 30
            
            // 내비게이션 상태 (4비트)
            bitIndex += 4
            
            // 회전율 (8비트)
            bitIndex += 8
            
            // 속도 (10비트, 0.1 노트 단위)
            val speedRaw = binaryData.substring(bitIndex, bitIndex + 10).toInt(2)
            val speed = if (speedRaw == 1023) 0.0 else speedRaw / 10.0
            bitIndex += 10
            
            // 위치 정확도 (1비트)
            bitIndex += 1
            
            // 경도 (28비트, 1/10000분 단위)
            val longitudeRaw = binaryData.substring(bitIndex, bitIndex + 28).toInt(2)
            val longitude = if (longitudeRaw == 0x6791AC0) null else longitudeRaw / 600000.0
            bitIndex += 28
            
            // 위도 (27비트, 1/10000분 단위)
            val latitudeRaw = binaryData.substring(bitIndex, bitIndex + 27).toInt(2)
            val latitude = if (latitudeRaw == 0x3412140) null else latitudeRaw / 600000.0
            bitIndex += 27
            
            // 침로 (12비트, 0.1도 단위)
            val cogRaw = binaryData.substring(bitIndex, bitIndex + 12).toInt(2)
            val course = if (cogRaw == 3600) 0.0 else cogRaw / 10.0
            bitIndex += 12
            
            // 선박 타입은 정적 데이터 메시지(타입 5)에서만 제공되므로 기본값으로 OTHER 사용
            val vesselType = VesselType.OTHER
            
            // 거리, 베어링, CPA, TCPA 계산
            val (distance, bearing, cpa, tcpa) = if (currentLatitude != null && currentLongitude != null && 
                latitude != null && longitude != null) {
                calculateVesselMetrics(
                    currentLatitude!!,
                    currentLongitude!!,
                    latitude,
                    longitude,
                    speed,
                    course
                )
            } else {
                Quadruple(0.0, 0, 0.0, 0)
            }
            val riskLevel = calculateRiskLevel(distance, cpa, tcpa)
            
            return AISVessel(
                id = "ais_$mmsi",
                name = "MMSI:$mmsi", // 정적 데이터 메시지에서 이름을 받아야 함
                mmsi = mmsi,
                type = vesselType,
                distance = distance,
                bearing = bearing,
                speed = speed,
                course = course.toInt(),
                cpa = cpa,
                tcpa = tcpa,
                riskLevel = riskLevel,
                isWatchlisted = false,
                lastUpdate = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 정적 데이터 메시지 파싱 (타입 5)
     */
    private fun parseStaticData(binaryData: String): AISVessel? {
        try {
            var bitIndex = 6 // 메시지 타입 이후부터 시작
            
            // MMSI (30비트)
            val mmsi = binaryData.substring(bitIndex, bitIndex + 30).toInt(2).toString()
            bitIndex += 30
            
            // AIS 버전 (2비트)
            bitIndex += 2
            
            // IMO 번호 (30비트)
            bitIndex += 30
            
            // 호출 부호 (42비트, 7자)
            bitIndex += 42
            
            // 선박 이름 (120비트, 20자)
            val nameBytes = binaryData.substring(bitIndex, bitIndex + 120)
            val name = decode6BitString(nameBytes).trim { it <= ' ' }
            bitIndex += 120
            
            // 선박 타입 (8비트)
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
            e.printStackTrace()
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
     * 6비트 ASCII 디코딩
     */
    private fun decode6BitASCII(data: String): String {
        val builder = StringBuilder()
        for (char in data) {
            val value = char.code - 48
            if (value < 0 || value > 63) continue
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

