package com.marineplay.chartplotter.data.repositories

import com.marineplay.chartplotter.data.datasources.LocalDataSource
import com.marineplay.chartplotter.domain.entities.Point
import com.marineplay.chartplotter.domain.repositories.PointRepository
import com.marineplay.chartplotter.data.models.SavedPoint
import com.marineplay.chartplotter.utils.DistanceCalculator
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class PointRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val mapLibreMap: MapLibreMap?
) : PointRepository {
    
    override suspend fun getAllPoints(): List<Point> {
        return localDataSource.loadPoints()
    }
    
    override suspend fun addPoint(point: Point): List<Point> {
        val currentPoints = getAllPoints().toMutableList()
        currentPoints.add(point)
        localDataSource.savePoints(currentPoints)
        return currentPoints
    }
    
    override suspend fun updatePoint(point: Point): List<Point> {
        val currentPoints = getAllPoints().toMutableList()
        val index = currentPoints.indexOfFirst { it.id == point.id }
        if (index != -1) {
            currentPoints[index] = point
            localDataSource.savePoints(currentPoints)
        }
        return currentPoints
    }
    
    override suspend fun deletePoint(pointId: String): List<Point> {
        val currentPoints = getAllPoints().toMutableList()
        currentPoints.removeAll { it.id == pointId }
        localDataSource.savePoints(currentPoints)
        return currentPoints
    }
    
    override suspend fun findClosestPoint(
        latitude: Double,
        longitude: Double,
        maxDistance: Double
    ): Point? {
        val allPoints = getAllPoints()
        if (allPoints.isEmpty() || mapLibreMap == null) return null
        
        val clickLatLng = LatLng(latitude, longitude)
        val closestPoint = allPoints.minByOrNull { point ->
            val pointLatLng = point.toLatLng()
            DistanceCalculator.calculateScreenDistance(clickLatLng, pointLatLng, mapLibreMap)
        }
        
        if (closestPoint != null) {
            val pointLatLng = closestPoint.toLatLng()
            val screenDistance = DistanceCalculator.calculateScreenDistance(clickLatLng, pointLatLng, mapLibreMap)
            
            return if (screenDistance <= maxDistance) {
                closestPoint
            } else null
        }
        
        return null
    }
    
    // SavedPoint 지원
    override fun getAllSavedPoints(): List<SavedPoint> {
        return localDataSource.loadSavedPoints()
    }
    
    override fun addSavedPoint(point: SavedPoint): List<SavedPoint> {
        val currentPoints = getAllSavedPoints().toMutableList()
        currentPoints.add(point)
        localDataSource.saveSavedPoints(currentPoints)
        return currentPoints
    }
    
    override fun updateSavedPoint(
        originalPoint: SavedPoint,
        newName: String,
        newColor: Int
    ): List<SavedPoint> {
        val existingPoints = getAllSavedPoints().toMutableList()
        val pointIndex = existingPoints.indexOfFirst { it.timestamp == originalPoint.timestamp }
        
        if (pointIndex != -1) {
            val updatedPoint = originalPoint.copy(
                name = newName,
                color = newColor
            )
            existingPoints[pointIndex] = updatedPoint
            localDataSource.saveSavedPoints(existingPoints)
        }
        
        return existingPoints
    }
    
    override fun deleteSavedPoint(point: SavedPoint): List<SavedPoint> {
        val existingPoints = getAllSavedPoints().toMutableList()
        existingPoints.removeAll { it.timestamp == point.timestamp }
        localDataSource.saveSavedPoints(existingPoints)
        return existingPoints
    }
}