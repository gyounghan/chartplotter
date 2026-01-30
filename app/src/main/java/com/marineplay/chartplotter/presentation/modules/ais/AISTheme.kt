package com.marineplay.chartplotter.presentation.modules.ais

import androidx.compose.ui.graphics.Color

/**
 * AIS 모듈 테마 색상
 * 선박 환경 최적화: 밝은 배경, 남색 프라이머리, 블루 서브프라이머리
 */
object AISTheme {
    // 배경색 (흰색 계열)
    val backgroundColor = Color(0xFFFFFFFF) // 흰색
    val cardBackground = Color(0xFFF5F7FA) // 약간의 회색 톤
    val cardBackgroundLight = Color(0xFFE8ECF1) // 더 밝은 회색
    
    // 텍스트 색상 (어두운 색)
    val textPrimary = Color(0xFF1A202C) // 진한 회색/검정
    val textSecondary = Color(0xFF4A5568) // 중간 회색
    
    // 테두리 색상
    val borderColor = Color(0xFFE2E8F0) // 연한 회색
    
    // 프라이머리 색상 (남색)
    val primary = Color(0xFF1E3A5F) // 남색
    val primaryLight = Color(0xFF2C4A6F) // 밝은 남색
    
    // 서브프라이머리 색상 (블루)
    val info = Color(0xFF2563EB) // 블루
    val infoLight = Color(0xFF3B82F6) // 밝은 블루
    
    // 상태 색상
    val danger = Color(0xFFDC2626) // 빨간색
    val dangerBackground = Color(0xFFFEE2E2) // 연한 빨간색 배경
    val warning = Color(0xFFD97706) // 주황색
    val warningBackground = Color(0xFFFEF3C7) // 연한 주황색 배경
    val safe = Color(0xFF059669) // 초록색
    val safeBackground = Color(0xFFD1FAE5) // 연한 초록색 배경
    
    // 그레이 색상
    val grayDark = Color(0xFF2D3748)
    val gray = Color(0xFF718096)
    val grayLight = Color(0xFFCBD5E0)
}

