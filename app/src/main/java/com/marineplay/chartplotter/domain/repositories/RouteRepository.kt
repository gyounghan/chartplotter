package com.marineplay.chartplotter.domain.repositories

import com.marineplay.chartplotter.data.models.Route

/**
 * Route Repository 인터페이스
 */
interface RouteRepository {
    /**
     * 모든 경로 목록 가져오기
     */
    fun getAllRoutes(): List<Route>
    
    /**
     * 경로 추가
     */
    fun addRoute(route: Route): List<Route>
    
    /**
     * 경로 업데이트
     */
    fun updateRoute(route: Route): List<Route>
    
    /**
     * 경로 삭제
     */
    fun deleteRoute(routeId: String): List<Route>
    
    /**
     * ID로 경로 가져오기
     */
    fun getRouteById(routeId: String): Route?
}
