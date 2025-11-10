package com.marineplay.chartplotter

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
 * 항적 기록 (시작시간~종료시간의 기록)
 */
data class TrackRecord(
    val id: String,
    val trackId: String, // 어떤 항적에 속하는지
    val startTime: Long,
    val endTime: Long,
    val points: List<TrackPoint>,
    val title: String // 시작시간+종료시간 조합
) {
    companion object {
        fun generateTitle(startTime: Long, endTime: Long): String {
            val startDate = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(startTime))
            val endDate = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(endTime))
            return "$startDate - $endDate"
        }
    }
}

/**
 * 항적 (여러 항적 기록을 포함)
 */
data class Track(
    val id: String,
    val name: String,
    val color: Color,
    val records: MutableList<TrackRecord> = mutableListOf(),
    var isVisible: Boolean = true // 화면에 표시할지 여부
)

/**
 * 항적 설정
 */
data class TrackSettings(
    val intervalType: String, // "time" or "distance"
    val timeInterval: Long = 5000L, // 밀리초 (기본 5초)
    val distanceInterval: Double = 10.0 // 미터 (기본 10m)
)

