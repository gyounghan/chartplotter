package com.marineplay.chartplotter.domain.usecases

import android.graphics.Color
import com.marineplay.chartplotter.domain.entities.Point
import com.marineplay.chartplotter.domain.repositories.PointRepository
import java.util.UUID

class PointUseCase(
    private val pointRepository: PointRepository
) {
    
    suspend fun getAllPoints(): List<Point> {
        return pointRepository.getAllPoints()
    }
    
    suspend fun addPoint(
        name: String,
        latitude: Double,
        longitude: Double,
        color: Color,
        iconType: String = "circle"
    ): List<Point> {
        val point = Point(
            id = UUID.randomUUID().toString(),
            name = name,
            latitude = latitude,
            longitude = longitude,
            color = color,
            iconType = iconType
        )
        return pointRepository.addPoint(point)
    }
    
    suspend fun updatePoint(point: Point): List<Point> {
        return pointRepository.updatePoint(point)
    }
    
    suspend fun deletePoint(pointId: String): List<Point> {
        return pointRepository.deletePoint(pointId)
    }
    
    suspend fun findClosestPoint(
        latitude: Double,
        longitude: Double,
        maxDistance: Double = 100.0
    ): Point? {
        return pointRepository.findClosestPoint(latitude, longitude, maxDistance)
    }
    
    fun generateUniquePointName(existingPoints: List<Point>): String {
        var counter = 1
        var name = "point001"
        
        while (existingPoints.any { it.name == name }) {
            counter++
            name = "point${counter.toString().padStart(3, '0')}"
        }
        
        return name
    }
}