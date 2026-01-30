package com.marineplay.chartplotter.data.models

/**
 * 저장된 포인트 데이터 모델
 * 기존 PointHelper.SavedPoint와 호환성을 위해 유지
 */
data class SavedPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val color: Int, // android.graphics.Color는 실제로 Int 타입
    val iconType: String = "circle",
    val timestamp: Long = System.currentTimeMillis()
)

