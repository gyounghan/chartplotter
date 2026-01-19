package com.marineplay.chartplotter.data

/**
 * 시스템 설정 데이터 클래스
 */
data class SystemSettings(
    val language: String = "한국어", // 한국어, 영어, 일본어, 중국어
    val vesselLength: Float = 10.0f, // 자선 길이 (미터)
    val vesselWidth: Float = 3.0f, // 자선 폭 (미터)
    val fontSize: Float = 14f, // 문자 크기 (sp)
    val buttonVolume: Int = 50, // 버튼 음량 (0-100)
    val timeFormat: String = "24시간", // 24시간, 12시간
    val dateFormat: String = "YYYY-MM-DD", // YYYY-MM-DD, MM/DD/YYYY, DD/MM/YYYY
    val geodeticSystem: String = "WGS84", // WGS84, GRS80, Bessel 등
    val coordinateFormat: String = "도", // 도, 도분, 도분초
    val declinationMode: String = "자동", // 자동, 수동
    val declinationValue: Float = 0f, // 자기변량 값 (수동 모드일 때)
    val pingSync: Boolean = true, // 핑 동기화
    val advancedFeatures: Map<String, Boolean> = emptyMap(), // 고급 기능 on/off
    val mobileConnected: Boolean = false, // 모바일 연결 상태
    val softwareVersion: String = "1.0.0", // SW 버전
    // 항해 설정
    val arrivalRadius: Float = 10.0f, // 도착반경 (미터)
    val xteLimit: Float = 50.0f, // XTE 제한 반경 (미터)
    val xteAlertEnabled: Boolean = true // XTE 경보 ON/OFF
)

