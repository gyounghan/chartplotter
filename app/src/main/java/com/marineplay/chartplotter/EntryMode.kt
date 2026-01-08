package com.marineplay.chartplotter

/**
 * EntryMode - 차트플로터 앱의 진입 모드 정의
 * 
 * 외부 런처 앱으로부터 Intent Extra "ENTRY_MODE"를 통해 전달받는 값입니다.
 * 이 값은 앱 시작 시 어떤 화면 구성을 할지 결정합니다.
 * 
 * Intent Contract:
 * - Action: android.intent.action.MAIN
 * - Component: com.marineplay.chartplotter.MainActivity
 * - Extra Key: ENTRY_MODE
 * - Extra Type: String
 * 
 * ⚠️ 주의: 이 값들은 enum처럼 취급되며 임의 변경 금지
 */
enum class EntryMode(val value: String) {
    /**
     * 차트플로터 화면만 표시
     */
    CHART_ONLY("CHART_ONLY"),
    
    /**
     * 블랙박스 화면만 표시
     */
    BLACKBOX_ONLY("BLACKBOX_ONLY"),
    
    /**
     * 카메라 화면만 표시
     */
    CAMERA_ONLY("CAMERA_ONLY"),
    
    /**
     * AIS 화면만 표시
     */
    AIS_ONLY("AIS_ONLY"),
    
    /**
     * 계기판 화면만 표시
     */
    DASHBOARD_ONLY("DASHBOARD_ONLY"),
    
    /**
     * 화면 분할 (차트 + 블랙박스)
     */
    SPLIT("SPLIT");
    
    companion object {
        /**
         * String 값으로부터 EntryMode를 찾습니다.
         * 값이 없거나 유효하지 않은 경우 기본값 CHART_ONLY를 반환합니다.
         */
        fun fromString(value: String?): EntryMode {
            return values().find { it.value == value } ?: CHART_ONLY
        }
        
        /**
         * Intent Extra Key 상수
         */
        const val INTENT_EXTRA_KEY = "ENTRY_MODE"
    }
}

