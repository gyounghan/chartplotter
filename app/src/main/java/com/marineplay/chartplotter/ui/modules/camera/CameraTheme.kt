package com.marineplay.chartplotter.ui.modules.camera

import androidx.compose.ui.graphics.Color

/**
 * 카메라 모듈 테마 색상 정의
 * 선박 환경 최적화: 밝은 배경, 남색 프라이머리, 블루 서브프라이머리
 */
object CameraTheme {
    // 배경색 (흰색 계열)
    val backgroundColor = Color(0xFFFFFFFF) // 흰색
    val surfaceColor = Color(0xFFF5F7FA) // 약간의 회색 톤
    val borderColor = Color(0xFFE2E8F0) // 연한 회색
    
    // 프라이머리 색상 (남색)
    val primaryColor = Color(0xFF1E3A5F) // 남색
    val primaryLight = Color(0xFF2C4A6F) // 밝은 남색
    
    // 서브프라이머리 색상 (블루)
    val subPrimaryColor = Color(0xFF2563EB) // 블루
    val subPrimaryLight = Color(0xFF3B82F6) // 밝은 블루
    
    // 텍스트 색상 (어두운 색)
    val textPrimary = Color(0xFF1A202C) // 진한 회색/검정
    val textSecondary = Color(0xFF4A5568) // 중간 회색
    
    // 상태 색상
    val successColor = Color(0xFF059669) // 초록색
    val errorColor = Color(0xFFDC2626) // 빨간색
}

