package com.marineplay.chartplotter.domain.repositories

import com.marineplay.chartplotter.domain.entities.Point
import com.marineplay.chartplotter.data.models.SavedPoint

interface PointRepository {
    suspend fun getAllPoints(): List<Point>
    suspend fun addPoint(point: Point): List<Point>
    suspend fun updatePoint(point: Point): List<Point>
    suspend fun deletePoint(pointId: String): List<Point>
    suspend fun findClosestPoint(latitude: Double, longitude: Double, maxDistance: Double): Point?
    
    // SavedPoint 지원 (기존 코드와 호환)
    fun getAllSavedPoints(): List<SavedPoint>
    fun addSavedPoint(point: SavedPoint): List<SavedPoint>
    fun updateSavedPoint(originalPoint: SavedPoint, newName: String, newColor: Int): List<SavedPoint>
    fun deleteSavedPoint(point: SavedPoint): List<SavedPoint>
}