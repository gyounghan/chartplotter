package com.marineplay.chartplotter.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 항적 설정 Entity
 * 단일 행만 존재 (id = "default")
 */
@Entity(tableName = "track_settings")
data class TrackSettingsEntity(
    @PrimaryKey
    val id: String = "default",
    val intervalType: String, // "auto", "time", "distance"
    val timeInterval: Long = 5000L, // 밀리초
    val distanceInterval: Double = 10.0 // 미터
)

