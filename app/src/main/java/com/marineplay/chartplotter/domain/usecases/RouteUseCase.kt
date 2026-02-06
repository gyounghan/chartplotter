package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.data.models.Route
import com.marineplay.chartplotter.data.models.RoutePoint
import com.marineplay.chartplotter.domain.repositories.RouteRepository
import java.util.UUID

/**
 * Route UseCase
 */
class RouteUseCase(
    private val routeRepository: RouteRepository
) {
    
    fun getAllRoutes(): List<Route> {
        return routeRepository.getAllRoutes()
    }
    
    fun createRoute(
        name: String,
        points: List<RoutePoint>
    ): Route {
        val route = Route(
            id = UUID.randomUUID().toString(),
            name = name,
            points = points.sortedBy { it.order },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        routeRepository.addRoute(route)
        return route
    }
    
    fun updateRoute(route: Route): Route {
        val updatedRoute = route.copy(updatedAt = System.currentTimeMillis())
        routeRepository.updateRoute(updatedRoute)
        return updatedRoute
    }
    
    fun deleteRoute(routeId: String): Boolean {
        val routes = routeRepository.deleteRoute(routeId)
        return routes.none { it.id == routeId }
    }
    
    fun getRouteById(routeId: String): Route? {
        return routeRepository.getRouteById(routeId)
    }
    
    /**
     * 경로에 포인트 추가
     */
    fun addPointToRoute(routeId: String, point: RoutePoint): Route? {
        val route = routeRepository.getRouteById(routeId) ?: return null
        val updatedPoints = route.points.toMutableList()
        updatedPoints.add(point)
        val updatedRoute = route.copy(
            points = updatedPoints.sortedBy { it.order },
            updatedAt = System.currentTimeMillis()
        )
        routeRepository.updateRoute(updatedRoute)
        return updatedRoute
    }
    
    /**
     * 경로에서 포인트 삭제
     */
    fun removePointFromRoute(routeId: String, order: Int): Route? {
        val route = routeRepository.getRouteById(routeId) ?: return null
        val updatedPoints = route.points.filter { it.order != order }
            .mapIndexed { index, point -> point.copy(order = index) }
        val updatedRoute = route.copy(
            points = updatedPoints,
            updatedAt = System.currentTimeMillis()
        )
        routeRepository.updateRoute(updatedRoute)
        return updatedRoute
    }
    
    /**
     * 경로 포인트 순서 변경
     */
    fun reorderRoutePoints(routeId: String, fromOrder: Int, toOrder: Int): Route? {
        val route = routeRepository.getRouteById(routeId) ?: return null
        val updatedPoints = route.points.toMutableList()
        
        if (fromOrder < 0 || fromOrder >= updatedPoints.size ||
            toOrder < 0 || toOrder >= updatedPoints.size) {
            return null
        }
        
        val movedPoint = updatedPoints.removeAt(fromOrder)
        updatedPoints.add(toOrder, movedPoint)
        
        val reorderedPoints = updatedPoints.mapIndexed { index, point ->
            point.copy(order = index)
        }
        
        val updatedRoute = route.copy(
            points = reorderedPoints,
            updatedAt = System.currentTimeMillis()
        )
        routeRepository.updateRoute(updatedRoute)
        return updatedRoute
    }
    
    /**
     * 고유한 경로 이름 생성
     */
    fun generateUniqueRouteName(): String {
        val existingRoutes = getAllRoutes()
        var counter = 1
        var name = "Route 1"
        
        while (existingRoutes.any { it.name == name }) {
            counter++
            name = "Route $counter"
        }
        
        return name
    }
}
