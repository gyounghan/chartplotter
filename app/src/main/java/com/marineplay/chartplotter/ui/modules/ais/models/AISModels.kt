package com.marineplay.chartplotter.ui.modules.ais.models

/**
 * AIS UI 모델
 * Presentation 레이어에서만 사용되는 UI 관련 모델
 * Domain Entity와는 분리되어 있음
 */

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
 * AIS 설정 (UI 설정)
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
