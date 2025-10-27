package com.marineplay.chartplotter.domain.repositories

import com.marineplay.chartplotter.domain.entities.Destination

interface DestinationRepository {
    suspend fun getAllDestinations(): List<Destination>
    suspend fun addDestination(destination: Destination): List<Destination>
    suspend fun updateDestination(destination: Destination): List<Destination>
    suspend fun deleteDestination(destinationId: String): List<Destination>
    suspend fun findClosestDestination(latitude: Double, longitude: Double, maxDistance: Double): Destination?
    suspend fun setCourseDestination(destination: Destination?)
    suspend fun getCourseDestination(): Destination?
}