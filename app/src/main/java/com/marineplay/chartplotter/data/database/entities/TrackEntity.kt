package com.marineplay.chartplotter.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 항적 Entity
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorValue: Long, // Color.value
    val isVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // 항적별 설정
    val intervalType: String = "time", // "time" or "distance"
    val timeInterval: Long = 5000L, // 밀리초 (기본 5초)
    val distanceInterval: Double = 10.0, // 미터 (기본 10m)
    val isRecording: Boolean = false // 현재 기록 중인지 여부
)

