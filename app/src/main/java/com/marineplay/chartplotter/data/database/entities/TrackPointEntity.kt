package com.marineplay.chartplotter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 항적 포인트 Entity (Track에 직접 연결)
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("trackId"),
        Index("trackId", "timestamp"), // 항적별 시간순 조회를 위한 복합 인덱스
        Index("timestamp"), // 날짜별 조회를 위한 인덱스
        Index("trackId", "date") // 항적별 날짜별 조회를 위한 복합 인덱스
    ]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: String, // Track에 직접 연결 (recordId 제거)
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val date: String // 날짜 (yyyy-MM-dd 형식) - 날짜별 조회를 위해 추가
)

