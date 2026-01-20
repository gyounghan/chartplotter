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
    val xteAlertEnabled: Boolean = true, // XTE 경보 ON/OFF
    // 지도 설정
    val boat3DEnabled: Boolean = false, // 3D 보트 선택
    val distanceCircleRadius: Float = 100.0f, // 거리원 반경 (미터)
    val headingLineEnabled: Boolean = true, // 해딩연장 ON/OFF
    val courseLineEnabled: Boolean = true, // 코스 연장 (GPS선) ON/OFF
    val extensionLength: Float = 100.0f, // 연장선 길이 (미터)
    val gridLineEnabled: Boolean = false, // 위경도선 (격자) ON/OFF
    val destinationVisible: Boolean = true, // 목적지 표시 ON/OFF
    val routeVisible: Boolean = true, // 경로 표시 ON/OFF
    val trackVisible: Boolean = true, // 항적 표시 ON/OFF
    val mapHidden: Boolean = false, // 지도 감춤 ON/OFF
    // 경보 설정
    val alertEnabled: Boolean = true, // 경보 사용 가능 ON/OFF
    val alertSettings: Map<String, Boolean> = emptyMap(), // 개별 경보 설정
    // 단위 설정
    val distanceUnit: String = "nm", // 거리 (nm, km, mi)
    val smallDistanceUnit: String = "m", // 거리소 (m, ft, yd)
    val speedUnit: String = "노트", // 선속 (노트, 시속, mph)
    val windSpeedUnit: String = "노트", // 풍속 (노트, 시속, mph)
    val depthUnit: String = "m", // 수심 (m, ft, 패덤)
    val altitudeUnit: String = "m", // 고도 (m, ft)
    val altitudeDatum: String = "지오이드", // 고도데이텀 (지오이드, wgs-84)
    val headingUnit: String = "M", // 헤딩 (M, T)
    val temperatureUnit: String = "C", // 수온 (C, F)
    val capacityUnit: String = "L", // 용량
    val fuelEfficiencyUnit: String = "L/h", // 연비
    val pressureUnit: String = "bar", // 압력
    val atmosphericPressureUnit: String = "hPa", // 기압
    // 무선 설정
    val bluetoothEnabled: Boolean = false, // 블루투스 ON/OFF
    val bluetoothPairedDevices: List<String> = emptyList(), // 페어링 된 장치 목록
    val wifiEnabled: Boolean = false, // WiFi ON/OFF
    val wifiConnectedNetwork: String? = null, // 연결된 WiFi 네트워크
    // 네트워크 설정
    val nmea2000Enabled: Boolean = false, // NMEA 2000 ON/OFF
    val nmea2000Settings: Map<String, String> = emptyMap(), // NMEA 2000 설정
    val nmea0183Enabled: Boolean = false, // NMEA 0183 ON/OFF
    val nmea0183Settings: Map<String, String> = emptyMap(), // NMEA 0183 설정
    // 선박 설정
    val mmsi: String = "", // MMSI 번호
    val aisCourseExtension: Float = 100.0f, // AIS 코스 연장 (미터)
    val vesselTrackingSettings: Map<String, Boolean> = emptyMap(), // 선박 및 추적 물표 설정
    val recordLength: Int = 60 // 기록 길이 (분)
)

