package com.marineplay.chartplotter.domain.entities

import androidx.compose.ui.graphics.Color

/**
 * 항적 기록의 한 점
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

/**
 * 항적 (TrackPoint를 직접 포함)
 */
data class Track(
    val id: String,
    val name: String,
    val color: Color,
    val points: MutableList<TrackPoint> = mutableListOf(), // TrackPoint 직접 저장 (TrackRecord 제거)
    var isVisible: Boolean = true, // 화면에 표시할지 여부
    // 항적별 설정
    val intervalType: String = "time", // "time" or "distance"
    val timeInterval: Long = 5000L, // 밀리초 (기본 5초)
    val distanceInterval: Double = 10.0, // 미터 (기본 10m)
    val isRecording: Boolean = false // 현재 기록 중인지 여부
)

