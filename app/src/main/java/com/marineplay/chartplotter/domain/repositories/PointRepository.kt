package com.marineplay.chartplotter.domain.repositories

import com.marineplay.chartplotter.domain.entities.Point
import com.marineplay.chartplotter.helpers.PointHelper

interface PointRepository {
    suspend fun getAllPoints(): List<Point>
    suspend fun addPoint(point: Point): List<Point>
    suspend fun updatePoint(point: Point): List<Point>
    suspend fun deletePoint(pointId: String): List<Point>
    suspend fun findClosestPoint(latitude: Double, longitude: Double, maxDistance: Double): Point?
    
    // PointHelper.SavedPoint 지원 (기존 코드와 호환)
    fun getAllSavedPoints(): List<PointHelper.SavedPoint>
    fun addSavedPoint(point: PointHelper.SavedPoint): List<PointHelper.SavedPoint>
    fun updateSavedPoint(originalPoint: PointHelper.SavedPoint, newName: String, newColor: android.graphics.Color): List<PointHelper.SavedPoint>
    fun deleteSavedPoint(point: PointHelper.SavedPoint): List<PointHelper.SavedPoint>
}