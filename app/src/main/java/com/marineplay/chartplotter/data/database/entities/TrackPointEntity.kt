package com.marineplay.chartplotter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 항적 포인트 Entity
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = TrackRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("recordId"),
        Index("recordId", "sequence") // 순서대로 조회를 위한 복합 인덱스
    ]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val sequence: Int // 포인트 순서 (0부터 시작)
)

