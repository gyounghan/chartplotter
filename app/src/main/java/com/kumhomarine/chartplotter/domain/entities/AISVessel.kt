package com.kumhomarine.chartplotter.domain.entities

/**
 * AIS 선박 타입
 */
enum class VesselType(val label: String, val emoji: String) {
    CARGO("화물선", "📦"),
    TANKER("유조선", "⛽"),
    PASSENGER("여객선", "🚢"),
    FISHING("어선", "🎣"),
    PLEASURE("요트", "⛵"),
    OTHER("기타", "🚤")
}

/**
 * 위험 수준
 */
enum class RiskLevel(val label: String) {
    CRITICAL("즉시 위험"),
    WARNING("주의"),
    SAFE("정상")
}

/**
 * AIS 선박 정보 (Domain Entity)
 * 순수 비즈니스 로직만 포함, UI/Android 의존성 없음
 */
data class AISVessel(
    val id: String,
    val name: String,
    val mmsi: String,
    val type: VesselType,
    val distance: Double, // 해리 단위
    val bearing: Int, // 도 단위
    val speed: Double, // 노트 단위
    val course: Int, // 도 단위
    val cpa: Double, // 최근접점 거리 (해리)
    val tcpa: Int, // 최근접점 도달 시간 (분)
    val riskLevel: RiskLevel,
    val isWatchlisted: Boolean,
    val lastUpdate: Long, // 타임스탬프
    val latitude: Double? = null, // 위도 (raw)
    val longitude: Double? = null, // 경도 (raw)
    val displayLatitude: Double? = null, // 표시용 위도 (smoothing 적용)
    val displayLongitude: Double? = null // 표시용 경도 (smoothing 적용)
)

/**
 * 위험 이벤트 (Domain Entity)
 */
data class RiskEvent(
    val id: String,
    val timestamp: Long,
    val vesselId: String,
    val vesselName: String,
    val cpa: Double,
    val tcpa: Int,
    val riskLevel: RiskLevel,
    val description: String
)

