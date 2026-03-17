package com.kumhomarine.chartplotter.presentation.modules.ais.models

/**
 * AIS UI 모델
 * Presentation 레이어에서만 사용되는 UI 관련 모델
 * Domain Entity와는 분리되어 있음
 */

/**
 * AIS 탭 (AIS design 폴더 기준)
 */
enum class AISTab(val label: String) {
    MONITOR("모니터"),
    GUARD("경계구역"),
    WATCH("즐겨찾기"),
    LOG("기록")
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
 * AIS 설정 (UI 설정)
 */
data class AISSettings(
    val cpaWarningThreshold: Double = 2.0,
    val cpaCriticalThreshold: Double = 0.5,
    val tcpaWarningThreshold: Int = 30,
    val tcpaCriticalThreshold: Int = 10,
    val showAnchoredVessels: Boolean = true,
    val showCargoVessels: Boolean = true,
    val showTankers: Boolean = true,
    val showPassengerVessels: Boolean = true,
    val showFishingVessels: Boolean = true,
    val showPleasureVessels: Boolean = true,
    val showOtherVessels: Boolean = true,
    val watchlistAlerts: Boolean = true,
    // 경계 구역 (Guard screen)
    val guardEnabled: Boolean = true,
    val guardRadius: Double = 1.5,
    val guardBowExtension: Double = 0.5,
    val alarmEnabled: Boolean = true
)
