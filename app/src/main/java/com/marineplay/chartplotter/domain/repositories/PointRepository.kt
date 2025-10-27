package com.marineplay.chartplotter.domain.repositories

import com.marineplay.chartplotter.domain.entities.Point

interface PointRepository {
    suspend fun getAllPoints(): List<Point>
    suspend fun addPoint(point: Point): List<Point>
    suspend fun updatePoint(point: Point): List<Point>
    suspend fun deletePoint(pointId: String): List<Point>
    suspend fun findClosestPoint(latitude: Double, longitude: Double, maxDistance: Double): Point?
}