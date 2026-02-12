package com.marineplay.chartplotter.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.marineplay.chartplotter.SavedPoint
import com.marineplay.chartplotter.data.models.SavedPoint as DataSavedPoint
import com.marineplay.chartplotter.data.models.Route
import com.marineplay.chartplotter.data.models.RoutePoint
import com.marineplay.chartplotter.domain.repositories.PointRepository
import com.marineplay.chartplotter.domain.repositories.RouteRepository
import com.marineplay.chartplotter.domain.usecases.*
import com.marineplay.chartplotter.LocationManager

import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * 포인트 관련 UI 상태
 */
data class PointUiState(
    val pointName: String = "",
    val selectedColor: Color = Color.Red,
    val selectedIconType: String = "circle", // "circle", "triangle", "square"
    val pointCount: Int = 0,
    val centerCoordinates: String = "",
    val currentLatLng: LatLng? = null,
    val selectedPoint: SavedPoint? = null,
    val editPointName: String = "",
    val editSelectedColor: Color = Color.Red
)

/**
 * 지도 관련 UI 상태
 */
data class MapUiState(
    val mapDisplayMode: String = "노스업", // 노스업, 헤딩업, 코스업
    val coursePoint: SavedPoint? = null, // 코스업용 포인트
    val navigationPoint: SavedPoint? = null, // 항해용 포인트
    val waypoints: List<SavedPoint> = emptyList(), // 경유지 리스트
    val showCursor: Boolean = false,
    val cursorLatLng: LatLng? = null,
    val cursorScreenPosition: android.graphics.PointF? = null,
    val isMapInitialized: Boolean = false,
    val showMenu: Boolean = false,
    val currentMenu: String = "main", // "main", "point", "ais", "navigation", "track", "display", "route"
    val isZoomInLongPressed: Boolean = false,
    val isZoomOutLongPressed: Boolean = false,
    val popupPosition: android.graphics.PointF? = null,
    val showSettingsScreen: Boolean = false, // 설정 화면 표시 여부
    val selectedRoute: Route? = null, // 선택된 경로
    val isEditingRoute: Boolean = false, // 경로 편집 중
    val editingRoutePoints: List<RoutePoint> = emptyList(), // 편집 중인 경로 포인트
    val currentNavigationRoute: Route? = null, // 현재 항해 중인 경로
    val movingPointOrder: Int? = null // 위치 이동 중인 경로 점의 order (null이면 이동 모드 아님)
)

/**
 * GPS 관련 UI 상태
 */
data class GpsUiState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isAvailable: Boolean = false,
    val cog: Float = 0.0f, // Course Over Ground (선박 방향)
    val lastGpsLocation: LatLng? = null // 마지막 GPS 위치 저장
)

/**
 * 다이얼로그 관련 UI 상태
 */
data class DialogUiState(
    val showDialog: Boolean = false, // 포인트 등록 다이얼로그
    val showPointManageDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showPointSelectionDialog: Boolean = false,
    val showPointDeleteList: Boolean = false,
    val showWaypointDialog: Boolean = false,
    val isAddingWaypoint: Boolean = false,
    val showTrackSettingsDialog: Boolean = false,
    val showTrackListDialog: Boolean = false,
    val showTrackRecordListDialog: Boolean = false,
    // 시스템 설정 다이얼로그들
    val showLanguageDialog: Boolean = false,
    val showVesselSettingsDialog: Boolean = false,
    val showFontSizeDialog: Boolean = false,
    val showVolumeDialog: Boolean = false,
    val showTimeDialog: Boolean = false,
    val showGeodeticDialog: Boolean = false,
    val showCoordinateDialog: Boolean = false,
    val showDeclinationDialog: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val showPowerDialog: Boolean = false,
    val showAdvancedDialog: Boolean = false,
    val showConnectionDialog: Boolean = false,
    val showInfoDialog: Boolean = false,
    val showTrackLimitDialog: Boolean = false, // 항적 표시 제한 알림 다이얼로그
    val showRouteCreateDialog: Boolean = false // 경로 생성 설명 다이얼로그
)

/**
 * 메인 ViewModel
 * 모든 UI 상태를 관리하고 UseCase를 통해 비즈니스 로직을 처리합니다.
 */
class MainViewModel(
    // UseCase들
    private val registerPointUseCase: RegisterPointUseCase,
    private val deletePointUseCase: DeletePointUseCase,
    private val updatePointUseCase: UpdatePointUseCase,
    private val getNextAvailablePointNumberUseCase: GetNextAvailablePointNumberUseCase,
    private val calculateBearingUseCase: CalculateBearingUseCase,
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val mapRotationUseCase: MapRotationUseCase,
    private val zoomUseCase: ZoomUseCase,
    private val routeUseCase: RouteUseCase,
    private val connectRouteToNavigationUseCase: ConnectRouteToNavigationUseCase,
    // Repository
    private val pointRepository: PointRepository,
    private val routeRepository: RouteRepository,
) : ViewModel() {
    
    // ========== UI 상태 ==========
    var pointUiState by mutableStateOf(PointUiState())
        private set
    
    var mapUiState by mutableStateOf(MapUiState())
        private set
    
    var gpsUiState by mutableStateOf(GpsUiState())
        private set
    
    var dialogUiState by mutableStateOf(DialogUiState())
        private set
    
    // ========== PointUiState 업데이트 함수들 ==========
    
    fun updatePointName(name: String) {
        pointUiState = pointUiState.copy(pointName = name)
    }
    
    fun updateSelectedColor(color: Color) {
        pointUiState = pointUiState.copy(selectedColor = color)
    }
    
    fun updateSelectedIconType(iconType: String) {
        pointUiState = pointUiState.copy(selectedIconType = iconType)
    }
    
    fun updatePointCount(count: Int) {
        pointUiState = pointUiState.copy(pointCount = count)
    }
    
    fun updateCenterCoordinates(coordinates: String) {
        pointUiState = pointUiState.copy(centerCoordinates = coordinates)
    }
    
    fun updateCurrentLatLng(latLng: LatLng?) {
        pointUiState = pointUiState.copy(currentLatLng = latLng)
    }
    
    fun updateSelectedPoint(point: SavedPoint?) {
        pointUiState = pointUiState.copy(selectedPoint = point)
    }
    
    fun updateEditPointName(name: String) {
        pointUiState = pointUiState.copy(editPointName = name)
    }
    
    fun updateEditSelectedColor(color: Color) {
        pointUiState = pointUiState.copy(editSelectedColor = color)
    }
    
    // ========== MapUiState 업데이트 함수들 ==========
    
    fun updateMapDisplayMode(mode: String) {
        mapUiState = mapUiState.copy(mapDisplayMode = mode)
    }
    
    fun updateCoursePoint(point: SavedPoint?) {
        mapUiState = mapUiState.copy(coursePoint = point)
    }
    
    fun updateNavigationPoint(point: SavedPoint?) {
        mapUiState = mapUiState.copy(navigationPoint = point)
    }
    
    fun updateWaypoints(waypoints: List<SavedPoint>) {
        mapUiState = mapUiState.copy(waypoints = waypoints)
    }
    
    // ========== Route 관련 함수 ==========
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
    
    fun selectRoute(route: Route?) {
        mapUiState = mapUiState.copy(selectedRoute = route)
    }
    
    fun setEditingRoute(isEditing: Boolean) {
        android.util.Log.d("[MainViewModel]", "setEditingRoute 호출: $isEditing")
        mapUiState = mapUiState.copy(isEditingRoute = isEditing)
        android.util.Log.d("[MainViewModel]", "상태 업데이트 완료: isEditingRoute=${mapUiState.isEditingRoute}")
    }
    
    fun setEditingRoutePoints(points: List<RoutePoint>) {
        mapUiState = mapUiState.copy(editingRoutePoints = points)
    }
    
    fun addPointToEditingRoute(latitude: Double, longitude: Double, name: String = "") {
        val currentPoints = mapUiState.editingRoutePoints.toMutableList()
        val newPoint = RoutePoint(
            latitude = latitude,
            longitude = longitude,
            order = currentPoints.size,
            name = name
        )
        currentPoints.add(newPoint)
        mapUiState = mapUiState.copy(editingRoutePoints = currentPoints)
    }
    
    fun removePointFromEditingRoute(order: Int) {
        val currentPoints = mapUiState.editingRoutePoints.toMutableList()
        currentPoints.removeAll { it.order == order }
        val reorderedPoints = currentPoints.mapIndexed { index, point ->
            point.copy(order = index)
        }
        mapUiState = mapUiState.copy(editingRoutePoints = reorderedPoints)
    }
    
    /**
     * 경로 편집 중 점의 위치 변경
     */
    fun updatePointInEditingRoute(order: Int, latitude: Double, longitude: Double) {
        val currentPoints = mapUiState.editingRoutePoints.toMutableList()
        val pointIndex = currentPoints.indexOfFirst { it.order == order }
        if (pointIndex != -1) {
            currentPoints[pointIndex] = currentPoints[pointIndex].copy(
                latitude = latitude,
                longitude = longitude
            )
            mapUiState = mapUiState.copy(editingRoutePoints = currentPoints)
        }
    }
    
    /**
     * 경로 편집 중 점의 순서 변경 (위로 이동)
     */
    fun movePointUpInEditingRoute(order: Int) {
        if (order <= 0) return
        val sorted = mapUiState.editingRoutePoints.sortedBy { it.order }.toMutableList()
        if (order >= sorted.size) return
        
        // swap
        val current = sorted[order]
        val previous = sorted[order - 1]
        sorted[order - 1] = current.copy(order = order - 1)
        sorted[order] = previous.copy(order = order)
        
        mapUiState = mapUiState.copy(editingRoutePoints = sorted)
    }
    
    /**
     * 경로 편집 중 점의 순서 변경 (아래로 이동)
     */
    fun movePointDownInEditingRoute(order: Int) {
        val sorted = mapUiState.editingRoutePoints.sortedBy { it.order }.toMutableList()
        if (order < 0 || order >= sorted.size - 1) return
        
        // swap
        val current = sorted[order]
        val next = sorted[order + 1]
        sorted[order] = next.copy(order = order)
        sorted[order + 1] = current.copy(order = order + 1)
        
        mapUiState = mapUiState.copy(editingRoutePoints = sorted)
    }
    
    /**
     * 경로 점 위치 이동 모드 설정/해제
     */
    fun setMovingPointOrder(order: Int?) {
        mapUiState = mapUiState.copy(movingPointOrder = order)
    }
    
    /**
     * 경로를 항해로 지정
     */
    fun setRouteAsNavigation(route: Route, currentLocation: LatLng?) {
        android.util.Log.d("[MainViewModel]", "setRouteAsNavigation 호출: route=${route.name}, currentLocation=${currentLocation?.let { "(${it.latitude}, ${it.longitude})" } ?: "null"}")
        // 현재 위치가 있으면 가장 가까운 점부터 연결, 없으면 첫 번째 포인트부터 연결
        if (currentLocation == null) {
            android.util.Log.d("[MainViewModel]", "currentLocation이 null - 첫 번째 점부터 연결")
            // 현재 위치가 없으면 경로 시작점으로 연결
            val firstPoint = route.points.firstOrNull()
            if (firstPoint != null) {
                val waypoints = route.points.drop(1).dropLast(1).mapIndexed { index, routePoint ->
                    SavedPoint(
                        name = routePoint.name.ifEmpty { "Waypoint ${index + 1}" },
                        latitude = routePoint.latitude,
                        longitude = routePoint.longitude,
                        color = Color.Blue,
                        iconType = "circle",
                        timestamp = System.currentTimeMillis()
                    )
                }
                val destination = route.points.last().let { lastPoint ->
                    SavedPoint(
                        name = lastPoint.name.ifEmpty { "Destination" },
                        latitude = lastPoint.latitude,
                        longitude = lastPoint.longitude,
                        color = Color.Red,
                        iconType = "circle",
                        timestamp = System.currentTimeMillis()
                    )
                }
                mapUiState = mapUiState.copy(
                    waypoints = waypoints,
                    navigationPoint = destination,
                    currentNavigationRoute = route // 현재 항해 중인 경로 저장
                )
            }
        } else {
            // 현재 위치를 기준으로 경로상의 가장 가까운 점부터 연결
            android.util.Log.d("[MainViewModel]", "currentLocation 있음 - ConnectRouteToNavigationUseCase.execute 호출")
            try {
                val (waypoints, destination) = connectRouteToNavigationUseCase.execute(route, currentLocation)
                android.util.Log.d("[MainViewModel]", "경로 연결 완료: waypoints=${waypoints.size}개, destination=${destination.name}")
                mapUiState = mapUiState.copy(
                    waypoints = waypoints,
                    navigationPoint = destination,
                    currentNavigationRoute = route // 현재 항해 중인 경로 저장
                )
            } catch (e: Exception) {
                // 에러 처리
                android.util.Log.e("[MainViewModel]", "경로 연결 실패: ${e.message}", e)
            }
        }
    }
    
    /**
     * 항해 중지 시 현재 항해 경로 초기화
     */
    fun clearNavigationRoute() {
        mapUiState = mapUiState.copy(
            currentNavigationRoute = null
        )
    }
    
    fun updateShowCursor(show: Boolean) {
        mapUiState = mapUiState.copy(showCursor = show)
    }
    
    fun updateCursorLatLng(latLng: LatLng?) {
        mapUiState = mapUiState.copy(cursorLatLng = latLng)
    }
    
    fun updateCursorScreenPosition(position: android.graphics.PointF?) {
        mapUiState = mapUiState.copy(cursorScreenPosition = position)
    }
    
    fun updateIsMapInitialized(initialized: Boolean) {
        mapUiState = mapUiState.copy(isMapInitialized = initialized)
    }
    
    fun updateShowMenu(show: Boolean) {
        mapUiState = mapUiState.copy(showMenu = show)
    }
    
    fun updateCurrentMenu(menu: String) {
        mapUiState = mapUiState.copy(currentMenu = menu)
    }
    
    fun updateShowSettingsScreen(show: Boolean) {
        mapUiState = mapUiState.copy(showSettingsScreen = show)
    }
    
    fun updateIsZoomInLongPressed(pressed: Boolean) {
        mapUiState = mapUiState.copy(isZoomInLongPressed = pressed)
    }
    
    fun updateIsZoomOutLongPressed(pressed: Boolean) {
        mapUiState = mapUiState.copy(isZoomOutLongPressed = pressed)
    }
    
    fun updatePopupPosition(position: android.graphics.PointF?) {
        mapUiState = mapUiState.copy(popupPosition = position)
    }
    
    // ========== GpsUiState 업데이트 함수들 ==========
    
    fun updateGpsLocation(latitude: Double, longitude: Double, isAvailable: Boolean) {
        gpsUiState = gpsUiState.copy(
            latitude = latitude,
            longitude = longitude,
            isAvailable = isAvailable,
            lastGpsLocation = if (isAvailable) LatLng(latitude, longitude) else gpsUiState.lastGpsLocation
        )
    }
    
    fun updateCog(cog: Float) {
        gpsUiState = gpsUiState.copy(cog = cog)
    }
    
    // ========== DialogUiState 업데이트 함수들 ==========
    
    fun updateShowDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showDialog = show)
    }
    
    fun updateShowPointManageDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showPointManageDialog = show)
    }
    
    fun updateShowEditDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showEditDialog = show)
    }
    
    fun updateShowPointSelectionDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showPointSelectionDialog = show)
    }
    
    fun updateShowPointDeleteList(show: Boolean) {
        dialogUiState = dialogUiState.copy(showPointDeleteList = show)
    }
    
    fun updateShowWaypointDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showWaypointDialog = show)
    }
    
    fun updateIsAddingWaypoint(adding: Boolean) {
        dialogUiState = dialogUiState.copy(isAddingWaypoint = adding)
    }
    
    fun updateShowTrackSettingsDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showTrackSettingsDialog = show)
    }
    
    fun updateShowTrackListDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showTrackListDialog = show)
    }
    
    fun updateShowTrackRecordListDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showTrackRecordListDialog = show)
    }
    
    // 시스템 설정 다이얼로그 업데이트 함수들
    fun updateShowLanguageDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showLanguageDialog = show)
    }
    
    fun updateShowVesselSettingsDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showVesselSettingsDialog = show)
    }
    
    fun updateShowFontSizeDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showFontSizeDialog = show)
    }
    
    fun updateShowVolumeDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showVolumeDialog = show)
    }
    
    fun updateShowTimeDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showTimeDialog = show)
    }
    
    fun updateShowGeodeticDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showGeodeticDialog = show)
    }
    
    fun updateShowCoordinateDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showCoordinateDialog = show)
    }
    
    fun updateShowDeclinationDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showDeclinationDialog = show)
    }
    
    fun updateShowResetConfirmDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showResetConfirmDialog = show)
    }
    
    fun updateShowPowerDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showPowerDialog = show)
    }
    
    fun updateShowAdvancedDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showAdvancedDialog = show)
    }
    
    fun updateShowConnectionDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showConnectionDialog = show)
    }
    
    fun updateShowInfoDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showInfoDialog = show)
    }
    
    // ========== 비즈니스 로직 함수들 (UseCase 사용) ==========
    
    /**
     * 포인트 등록
     */
    fun registerPoint(
        latLng: LatLng,
        name: String,
        color: Color,
        iconType: String
    ) {
        viewModelScope.launch {
            val savedPoints = registerPointUseCase.execute(latLng, name, color, iconType)
            pointUiState = pointUiState.copy(pointCount = savedPoints.size)
        }
    }
    
    /**
     * 포인트 삭제
     */
    fun deletePoint(point: DataSavedPoint) {
        viewModelScope.launch {
            val savedPoints = deletePointUseCase.execute(point)
            pointUiState = pointUiState.copy(pointCount = savedPoints.size)
        }
    }
    
    /**
     * 포인트 업데이트
     */
    fun updatePoint(
        originalPoint: DataSavedPoint,
        newName: String,
        newColor: Color
    ) {
        viewModelScope.launch {
            val savedPoints = updatePointUseCase.execute(originalPoint, newName, newColor)
            pointUiState = pointUiState.copy(pointCount = savedPoints.size)
        }
    }
    
    /**
     * 다음 사용 가능한 포인트 번호 가져오기
     */
    fun getNextAvailablePointNumber(): Int {
        return runBlocking {
            getNextAvailablePointNumberUseCase.execute()
        }
    }
    
    /**
     * 방위각 계산
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return calculateBearingUseCase.execute(lat1, lon1, lat2, lon2)
    }
    
    /**
     * 거리 계산
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return calculateDistanceUseCase.execute(lat1, lon1, lat2, lon2)
    }
    
    /**
     * 지도 회전 업데이트
     */
    fun updateMapRotation(
        map: MapLibreMap,
        locationManager: LocationManager?
    ) {
        val currentLocation = locationManager?.getCurrentLocationObject()
        mapRotationUseCase.execute(
            map = map,
            displayMode = mapUiState.mapDisplayMode,
            coursePoint = mapUiState.coursePoint, // com.marineplay.chartplotter.SavedPoint 타입 그대로 전달
            currentLocation = currentLocation
        )
        
        // 코스업 모드일 때 커서 숨기기
        if (mapUiState.mapDisplayMode == "코스업") {
            updateShowCursor(false)
            updateCursorLatLng(null)
            updateCursorScreenPosition(null)
        }
    }
    
    /**
     * 줌 인
     */
    fun zoomIn(map: MapLibreMap?) {
        map?.let {
            zoomUseCase.zoomIn(it, mapUiState.cursorLatLng)
            if (mapUiState.cursorLatLng != null) {
                val screenPosition = zoomUseCase.updateCursorScreenPosition(it, mapUiState.cursorLatLng!!)
                updateCursorScreenPosition(screenPosition)
            }
        }
    }
    
    /**
     * 줌 아웃
     */
    fun zoomOut(map: MapLibreMap?) {
        map?.let {
            zoomUseCase.zoomOut(it, mapUiState.cursorLatLng)
            if (mapUiState.cursorLatLng != null) {
                val screenPosition = zoomUseCase.updateCursorScreenPosition(it, mapUiState.cursorLatLng!!)
                updateCursorScreenPosition(screenPosition)
            }
        }
    }
    
    
    /**
     * 포인트 목록 로드
     */
    fun loadPointsFromLocal(): List<DataSavedPoint> {
        return runBlocking {
            pointRepository.getAllSavedPoints()
        }
    }
    
    fun updateShowTrackLimitDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showTrackLimitDialog = show)
    }
    
    fun updateShowRouteCreateDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showRouteCreateDialog = show)
    }
    
    
    /**
     * 포인트 등록 다이얼로그 준비
     */
    fun preparePointRegistration(latLng: LatLng?) {
        latLng?.let {
            updateCurrentLatLng(it)
            updateCenterCoordinates("위도: ${String.format("%.6f", it.latitude)}\n경도: ${String.format("%.6f", it.longitude)}")
            updatePointName("")
            updateSelectedColor(Color.Red)
            updateShowDialog(true)
        } ?: run {
            updateCenterCoordinates("좌표를 가져올 수 없습니다.")
            updateCurrentLatLng(null)
        }
    }
    
    /**
     * 지도 이동 (위/아래/왼쪽/오른쪽)
     */
    fun moveMap(map: MapLibreMap?, direction: String) {
        map?.let {
            val currentPosition = it.cameraPosition
            currentPosition.target?.let { target ->
                val currentLat = target.latitude
                val currentLng = target.longitude
                val zoom = currentPosition.zoom
                
                val delta = 0.01 / Math.pow(2.0, zoom - 8.0)
                val newLat: Double
                val newLng: Double
                
                when (direction) {
                    "up" -> {
                        newLat = currentLat + delta
                        newLng = currentLng
                    }
                    "down" -> {
                        newLat = currentLat - delta
                        newLng = currentLng
                    }
                    "left" -> {
                        newLat = currentLat
                        newLng = currentLng - delta
                    }
                    "right" -> {
                        newLat = currentLat
                        newLng = currentLng + delta
                    }
                    else -> return
                }
                
                val newPosition = LatLng(newLat, newLng)
                val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newLatLng(newPosition)
                it.animateCamera(cameraUpdate, 300)
            }
        }
    }
    
    /**
     * 커서 포인트 선택 처리 (하드웨어 키 이벤트용)
     */
    fun handleCursorPointSelection(
        map: MapLibreMap?,
        savedPoints: List<DataSavedPoint>,
        calculateScreenDistance: (LatLng, LatLng, MapLibreMap) -> Double
    ) {
        if (!mapUiState.showCursor || mapUiState.cursorLatLng == null || mapUiState.cursorScreenPosition == null) {
            return
        }
        
        map?.let { mapInstance ->
            val cursorLatLng = mapUiState.cursorLatLng!!
            val closestPoint = savedPoints.minByOrNull { point ->
                val pointLatLng = LatLng(point.latitude, point.longitude)
                calculateScreenDistance(cursorLatLng, pointLatLng, mapInstance)
            }
            
            if (closestPoint != null) {
                val pointLatLng = LatLng(closestPoint.latitude, closestPoint.longitude)
                val screenDistance = calculateScreenDistance(cursorLatLng, pointLatLng, mapInstance)
                
                if (screenDistance <= 40) {
                    // 포인트 편집/삭제 다이얼로그 표시
                    // DataSavedPoint를 com.marineplay.chartplotter.SavedPoint로 변환
                    val savedPoint = SavedPoint(
                        name = closestPoint.name,
                        latitude = closestPoint.latitude,
                        longitude = closestPoint.longitude,
                        color = Color(closestPoint.color),
                        iconType = closestPoint.iconType,
                        timestamp = closestPoint.timestamp
                    )
                    updateSelectedPoint(savedPoint)
                    updateEditPointName(closestPoint.name)
                    updateEditSelectedColor(Color(closestPoint.color))
                    updateShowEditDialog(true)
                }
            }
        }
    }
    
    /**
     * Factory for MainViewModel
     */
    companion object {
        fun provideFactory(
            pointRepository: PointRepository,
            routeRepository: RouteRepository,
            locationManager: LocationManager?
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val calculateBearingUseCase = CalculateBearingUseCase()
                    val calculateDistanceUseCase = CalculateDistanceUseCase()
                    val getNextAvailablePointNumberUseCase = GetNextAvailablePointNumberUseCase(pointRepository)
                    val registerPointUseCase = RegisterPointUseCase(pointRepository, getNextAvailablePointNumberUseCase)
                    val deletePointUseCase = DeletePointUseCase(pointRepository)
                    val updatePointUseCase = UpdatePointUseCase(pointRepository)
                    val mapRotationUseCase = MapRotationUseCase(locationManager, calculateBearingUseCase)
                    val zoomUseCase = ZoomUseCase()
                    val routeUseCase = RouteUseCase(routeRepository)
                    val connectRouteToNavigationUseCase = ConnectRouteToNavigationUseCase(calculateDistanceUseCase)
                    
                    return MainViewModel(
                        registerPointUseCase = registerPointUseCase,
                        deletePointUseCase = deletePointUseCase,
                        updatePointUseCase = updatePointUseCase,
                        getNextAvailablePointNumberUseCase = getNextAvailablePointNumberUseCase,
                        calculateBearingUseCase = calculateBearingUseCase,
                        calculateDistanceUseCase = calculateDistanceUseCase,
                        mapRotationUseCase = mapRotationUseCase,
                        zoomUseCase = zoomUseCase,
                        routeUseCase = routeUseCase,
                        connectRouteToNavigationUseCase = connectRouteToNavigationUseCase,
                        pointRepository = pointRepository,
                        routeRepository = routeRepository
                    ) as T
                }
            }
        }
    }
}
