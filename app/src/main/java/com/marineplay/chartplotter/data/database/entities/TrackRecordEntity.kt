package com.marineplay.chartplotter.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 항적 기록 Entity
 */
@Entity(
    tableName = "track_records",
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
        Index("date") // 날짜별 조회를 위한 인덱스
    ]
)
data class TrackRecordEntity(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val startTime: Long,
    val endTime: Long,
    val title: String,
    val date: String // 날짜 (yyyy-MM-dd 형식)
)

