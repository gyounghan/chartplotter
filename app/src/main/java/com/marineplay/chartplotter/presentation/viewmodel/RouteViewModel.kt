package com.marineplay.chartplotter.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marineplay.chartplotter.data.models.Route
import com.marineplay.chartplotter.data.models.RoutePoint
import com.marineplay.chartplotter.domain.repositories.RouteRepository
import com.marineplay.chartplotter.domain.usecases.RouteUseCase

/**
 * 경로(Route) 관련 UI 상태
 */
data class RouteUiState(
    val selectedRoute: Route? = null,
    val isEditingRoute: Boolean = false,
    val editingRoutePoints: List<RoutePoint> = emptyList(),
    val movingPointOrder: Int? = null,
    val showRouteCreateDialog: Boolean = false
)

/**
 * 경로(Route) 전용 ViewModel
 * 경로 CRUD, 편집 등 경로 관련 모든 비즈니스 로직을 처리합니다.
 */
class RouteViewModel(
    private val routeUseCase: RouteUseCase
) : ViewModel() {

    // ========== UI 상태 ==========
    var routeUiState by mutableStateOf(RouteUiState())
        private set

    // ========== Route CRUD ==========

    fun getAllRoutes(): List<Route> {
        return routeUseCase.getAllRoutes()
    }

    fun createRoute(name: String, points: List<RoutePoint>): Route {
        return routeUseCase.createRoute(name, points)
    }

    fun updateRoute(route: Route) {
        routeUseCase.updateRoute(route)
    }

    fun deleteRoute(routeId: String) {
        routeUseCase.deleteRoute(routeId)
    }

    // ========== Route 편집 상태 ==========

    fun selectRoute(route: Route?) {
        routeUiState = routeUiState.copy(selectedRoute = route)
    }

    fun setEditingRoute(isEditing: Boolean) {
        android.util.Log.d("[RouteViewModel]", "setEditingRoute 호출: $isEditing")
        routeUiState = routeUiState.copy(isEditingRoute = isEditing)
        android.util.Log.d("[RouteViewModel]", "상태 업데이트 완료: isEditingRoute=${routeUiState.isEditingRoute}")
    }

    fun setEditingRoutePoints(points: List<RoutePoint>) {
        routeUiState = routeUiState.copy(editingRoutePoints = points)
    }

    fun addPointToEditingRoute(latitude: Double, longitude: Double, name: String = "") {
        val currentPoints = routeUiState.editingRoutePoints.toMutableList()
        val newPoint = RoutePoint(
            latitude = latitude,
            longitude = longitude,
            order = currentPoints.size,
            name = name
        )
        currentPoints.add(newPoint)
        routeUiState = routeUiState.copy(editingRoutePoints = currentPoints)
    }

    fun removePointFromEditingRoute(order: Int) {
        val currentPoints = routeUiState.editingRoutePoints.toMutableList()
        currentPoints.removeAll { it.order == order }
        val reorderedPoints = currentPoints.mapIndexed { index, point ->
            point.copy(order = index)
        }
        routeUiState = routeUiState.copy(editingRoutePoints = reorderedPoints)
    }

    /**
     * 경로 편집 중 점의 위치 변경
     */
    fun updatePointInEditingRoute(order: Int, latitude: Double, longitude: Double) {
        val currentPoints = routeUiState.editingRoutePoints.toMutableList()
        val pointIndex = currentPoints.indexOfFirst { it.order == order }
        if (pointIndex != -1) {
            currentPoints[pointIndex] = currentPoints[pointIndex].copy(
                latitude = latitude,
                longitude = longitude
            )
            routeUiState = routeUiState.copy(editingRoutePoints = currentPoints)
        }
    }

    /**
     * 경로 편집 중 점의 순서 변경 (위로 이동)
     */
    fun movePointUpInEditingRoute(order: Int) {
        if (order <= 0) return
        val sorted = routeUiState.editingRoutePoints.sortedBy { it.order }.toMutableList()
        if (order >= sorted.size) return

        // swap
        val current = sorted[order]
        val previous = sorted[order - 1]
        sorted[order - 1] = current.copy(order = order - 1)
        sorted[order] = previous.copy(order = order)

        routeUiState = routeUiState.copy(editingRoutePoints = sorted)
    }

    /**
     * 경로 편집 중 점의 순서 변경 (아래로 이동)
     */
    fun movePointDownInEditingRoute(order: Int) {
        val sorted = routeUiState.editingRoutePoints.sortedBy { it.order }.toMutableList()
        if (order < 0 || order >= sorted.size - 1) return

        // swap
        val current = sorted[order]
        val next = sorted[order + 1]
        sorted[order] = next.copy(order = order)
        sorted[order + 1] = current.copy(order = order + 1)

        routeUiState = routeUiState.copy(editingRoutePoints = sorted)
    }

    /**
     * 경로 점 위치 이동 모드 설정/해제
     */
    fun setMovingPointOrder(order: Int?) {
        routeUiState = routeUiState.copy(movingPointOrder = order)
    }

    // ========== 다이얼로그 ==========

    fun updateShowRouteCreateDialog(show: Boolean) {
        routeUiState = routeUiState.copy(showRouteCreateDialog = show)
    }

    /**
     * Factory for RouteViewModel
     */
    companion object {
        fun provideFactory(
            routeRepository: RouteRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val routeUseCase = RouteUseCase(routeRepository)
                    return RouteViewModel(
                        routeUseCase = routeUseCase
                    ) as T
                }
            }
        }
    }
}
