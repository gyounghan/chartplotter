package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.domain.entities.Destination
import com.marineplay.chartplotter.domain.repositories.DestinationRepository
import java.util.UUID

class DestinationUseCase(
    private val destinationRepository: DestinationRepository
) {
    
    suspend fun getAllDestinations(): List<Destination> {
        return destinationRepository.getAllDestinations()
    }
    
    suspend fun addDestination(
        name: String,
        latitude: Double,
        longitude: Double
    ): List<Destination> {
        val destination = Destination(
            id = UUID.randomUUID().toString(),
            name = name,
            latitude = latitude,
            longitude = longitude
        )
        return destinationRepository.addDestination(destination)
    }
    
    suspend fun updateDestination(destination: Destination): List<Destination> {
        return destinationRepository.updateDestination(destination)
    }
    
    suspend fun deleteDestination(destinationId: String): List<Destination> {
        return destinationRepository.deleteDestination(destinationId)
    }
    
    suspend fun findClosestDestination(
        latitude: Double,
        longitude: Double,
        maxDistance: Double = 100.0
    ): Destination? {
        return destinationRepository.findClosestDestination(latitude, longitude, maxDistance)
    }
    
    suspend fun setCourseDestination(destination: Destination?) {
        destinationRepository.setCourseDestination(destination)
    }
    
    suspend fun getCourseDestination(): Destination? {
        return destinationRepository.getCourseDestination()
    }
    
    fun generateUniqueDestinationName(existingDestinations: List<Destination>): String {
        var counter = 1
        var name = "target001"
        
        while (existingDestinations.any { it.name == name }) {
            counter++
            name = "target${counter.toString().padStart(3, '0')}"
        }
        
        return name
    }
}