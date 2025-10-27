package com.marineplay.chartplotter.data.repositories

import com.marineplay.chartplotter.data.datasources.LocalDataSource
import com.marineplay.chartplotter.domain.entities.Destination
import com.marineplay.chartplotter.domain.repositories.DestinationRepository
import com.marineplay.chartplotter.utils.DistanceCalculator
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class DestinationRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val mapLibreMap: MapLibreMap?
) : DestinationRepository {
    
    override suspend fun getAllDestinations(): List<Destination> {
        return localDataSource.loadDestinations()
    }
    
    override suspend fun addDestination(destination: Destination): List<Destination> {
        val currentDestinations = getAllDestinations().toMutableList()
        currentDestinations.add(destination)
        localDataSource.saveDestinations(currentDestinations)
        return currentDestinations
    }
    
    override suspend fun updateDestination(destination: Destination): List<Destination> {
        val currentDestinations = getAllDestinations().toMutableList()
        val index = currentDestinations.indexOfFirst { it.id == destination.id }
        if (index != -1) {
            currentDestinations[index] = destination
            localDataSource.saveDestinations(currentDestinations)
        }
        return currentDestinations
    }
    
    override suspend fun deleteDestination(destinationId: String): List<Destination> {
        val currentDestinations = getAllDestinations().toMutableList()
        currentDestinations.removeAll { it.id == destinationId }
        localDataSource.saveDestinations(currentDestinations)
        return currentDestinations
    }
    
    override suspend fun findClosestDestination(
        latitude: Double,
        longitude: Double,
        maxDistance: Double
    ): Destination? {
        val allDestinations = getAllDestinations()
        if (allDestinations.isEmpty() || mapLibreMap == null) return null
        
        val clickLatLng = LatLng(latitude, longitude)
        val closestDestination = allDestinations.minByOrNull { destination ->
            val destinationLatLng = destination.toLatLng()
            DistanceCalculator.calculateScreenDistance(clickLatLng, destinationLatLng, mapLibreMap)
        }
        
        if (closestDestination != null) {
            val destinationLatLng = closestDestination.toLatLng()
            val screenDistance = DistanceCalculator.calculateScreenDistance(clickLatLng, destinationLatLng, mapLibreMap)
            
            return if (screenDistance <= maxDistance) {
                closestDestination
            } else null
        }
        
        return null
    }
    
    override suspend fun setCourseDestination(destination: Destination?) {
        if (destination != null) {
            localDataSource.saveCourseDestination(destination.latitude, destination.longitude)
        }
    }
    
    override suspend fun getCourseDestination(): Destination? {
        val courseDest = localDataSource.loadCourseDestination()
        return if (courseDest != null) {
            Destination(
                id = "course_destination",
                name = "Course Destination",
                latitude = courseDest.first,
                longitude = courseDest.second
            )
        } else null
    }
}