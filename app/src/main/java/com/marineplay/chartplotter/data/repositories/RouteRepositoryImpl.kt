package com.marineplay.chartplotter.data.repositories

import com.marineplay.chartplotter.data.datasources.LocalDataSource
import com.marineplay.chartplotter.data.models.Route
import com.marineplay.chartplotter.domain.repositories.RouteRepository

class RouteRepositoryImpl(
    private val localDataSource: LocalDataSource
) : RouteRepository {
    
    override fun getAllRoutes(): List<Route> {
        return localDataSource.loadRoutes()
    }
    
    override fun addRoute(route: Route): List<Route> {
        val currentRoutes = getAllRoutes().toMutableList()
        currentRoutes.add(route)
        localDataSource.saveRoutes(currentRoutes)
        return currentRoutes
    }
    
    override fun updateRoute(route: Route): List<Route> {
        val currentRoutes = getAllRoutes().toMutableList()
        val index = currentRoutes.indexOfFirst { it.id == route.id }
        if (index != -1) {
            currentRoutes[index] = route.copy(updatedAt = System.currentTimeMillis())
            localDataSource.saveRoutes(currentRoutes)
        }
        return currentRoutes
    }
    
    override fun deleteRoute(routeId: String): List<Route> {
        val currentRoutes = getAllRoutes().toMutableList()
        currentRoutes.removeAll { it.id == routeId }
        localDataSource.saveRoutes(currentRoutes)
        return currentRoutes
    }
    
    override fun getRouteById(routeId: String): Route? {
        return getAllRoutes().find { it.id == routeId }
    }
}
