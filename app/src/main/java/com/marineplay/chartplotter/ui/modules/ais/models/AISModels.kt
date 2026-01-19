package com.marineplay.chartplotter.ui.modules.ais.models

import androidx.compose.ui.graphics.Color

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
 * AIS 탭
 */
enum class AISTab(val label: String) {
    RISK("위험"),
    VESSELS("선박"),
    EVENTS("이벤트"),
    SETTINGS("설정")
}

/**
 * 정렬 옵션
 */
enum class SortOption(val label: String) {
    DISTANCE("거리순"),
    RISK("위험도순"),
    NAME("이름순")
}

/**
 * AIS 선박 정보
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
    val lastUpdate: Long // 타임스탬프
)

/**
 * 위험 이벤트
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

/**
 * AIS 설정
 */
data class AISSettings(
    val cpaWarningThreshold: Double = 2.0, // 해리
    val cpaCriticalThreshold: Double = 0.5, // 해리
    val tcpaWarningThreshold: Int = 30, // 분
    val tcpaCriticalThreshold: Int = 10, // 분
    val showAnchoredVessels: Boolean = true,
    val showCargoVessels: Boolean = true,
    val showTankers: Boolean = true,
    val showPassengerVessels: Boolean = true,
    val showFishingVessels: Boolean = true,
    val showPleasureVessels: Boolean = true,
    val showOtherVessels: Boolean = true,
    val watchlistAlerts: Boolean = true
)

