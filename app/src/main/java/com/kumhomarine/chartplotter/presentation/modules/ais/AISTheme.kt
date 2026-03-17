package com.kumhomarine.chartplotter.presentation.modules.ais

import androidx.compose.ui.graphics.Color

/**
 * AIS 모듈 테마 색상
 * AIS design 폴더 기준 Marine Industrial 컬러 팔레트 적용
 * @see AIS design/src/styles/theme.css
 */
object AISTheme {
    // 배경색 (검정 통일)
    val backgroundColor = Color(0xFF000000) // marine-black
    val cardBackground = Color(0xFF000000) // marine-black
    val cardBackgroundLight = Color(0xFF000000) // marine-black
    
    // 텍스트 색상
    val textPrimary = Color(0xFFFFFFFF) // white
    val textSecondary = Color(0xFF7A9AB2) // marine-text-normal
    val textDim = Color(0xFF5A7A92) // marine-text-dim
    
    // 테두리 색상
    val borderColor = Color(0xFF1A3A52) // marine-border
    val borderLight = Color(0xFF2A5A72) // marine-border-light
    
    // 프라이머리/액센트 (초록)
    val primary = Color(0xFF00FF00) // marine-green
    val primaryLight = Color(0xFF00FF00)
    
    // 서브프라이머리/정보
    val info = Color(0xFF00FF00)
    val infoLight = Color(0xFF00FF00)
    
    // 상태 색상
    val danger = Color(0xFFFF0000) // marine-red
    val dangerBackground = Color(0xFF2A0A0A)
    val warning = Color(0xFFFFAA00) // marine-amber
    val warningBackground = Color(0xFF2A1F0A)
    val safe = Color(0xFF00FF00) // marine-green
    val safeBackground = Color(0xFF0A1F0A)
    
    // 그레이
    val grayDark = Color(0xFF000000)
    val gray = Color(0xFF5A7A92)
    val grayLight = Color(0xFF7A9AB2)
}

