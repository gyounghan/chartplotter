package com.marineplay.chartplotter.data.models

import com.marineplay.chartplotter.domain.entities.AISVessel
import com.marineplay.chartplotter.domain.entities.RiskEvent
import com.marineplay.chartplotter.domain.entities.RiskLevel
import com.marineplay.chartplotter.domain.entities.VesselType

/**
 * AIS 선박 Data Transfer Object
 * Data 레이어에서 사용하는 모델
 */
data class AISVesselDto(
    val id: String,
    val name: String,
    val mmsi: String,
    val type: VesselType,
    val distance: Double,
    val bearing: Int,
    val speed: Double,
    val course: Int,
    val cpa: Double,
    val tcpa: Int,
    val riskLevel: RiskLevel,
    val isWatchlisted: Boolean,
    val lastUpdate: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * 위험 이벤트 Data Transfer Object
 */
data class RiskEventDto(
    val id: String,
    val timestamp: Long,
    val vesselId: String,
    val vesselName: String,
    val cpa: Double,
    val tcpa: Int,
    val riskLevel: RiskLevel,
    val description: String
)

/**
 * Domain Entity <-> DTO 변환 확장 함수
 */
fun AISVessel.toDto(): AISVesselDto {
    return AISVesselDto(
        id = id,
        name = name,
        mmsi = mmsi,
        type = type,
        distance = distance,
        bearing = bearing,
        speed = speed,
        course = course,
        cpa = cpa,
        tcpa = tcpa,
        riskLevel = riskLevel,
        isWatchlisted = isWatchlisted,
        lastUpdate = lastUpdate,
        latitude = latitude,
        longitude = longitude
    )
}

fun AISVesselDto.toEntity(): AISVessel {
    return AISVessel(
        id = id,
        name = name,
        mmsi = mmsi,
        type = type,
        distance = distance,
        bearing = bearing,
        speed = speed,
        course = course,
        cpa = cpa,
        tcpa = tcpa,
        riskLevel = riskLevel,
        isWatchlisted = isWatchlisted,
        lastUpdate = lastUpdate,
        latitude = latitude,
        longitude = longitude
    )
}

fun RiskEvent.toDto(): RiskEventDto {
    return RiskEventDto(
        id = id,
        timestamp = timestamp,
        vesselId = vesselId,
        vesselName = vesselName,
        cpa = cpa,
        tcpa = tcpa,
        riskLevel = riskLevel,
        description = description
    )
}

fun RiskEventDto.toEntity(): RiskEvent {
    return RiskEvent(
        id = id,
        timestamp = timestamp,
        vesselId = vesselId,
        vesselName = vesselName,
        cpa = cpa,
        tcpa = tcpa,
        riskLevel = riskLevel,
        description = description
    )
}

