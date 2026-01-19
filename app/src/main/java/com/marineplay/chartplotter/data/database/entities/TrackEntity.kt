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
    val createdAt: Long = System.currentTimeMillis()
)

