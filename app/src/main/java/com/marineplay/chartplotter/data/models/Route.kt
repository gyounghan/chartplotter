package com.marineplay.chartplotter.data.models

/**
 * 경로 데이터 모델
 */
data class Route(
    val id: String,
    val name: String,
    val points: List<RoutePoint>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 경로 내 포인트 (순서 포함)
 */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val order: Int, // 경로 내 순서
    val name: String = "" // 선택적 이름
)
