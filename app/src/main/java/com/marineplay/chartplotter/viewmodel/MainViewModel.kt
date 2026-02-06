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
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.entities.TrackPoint
import com.marineplay.chartplotter.domain.repositories.TrackRepository
import com.marineplay.chartplotter.domain.repositories.PointRepository
import com.marineplay.chartplotter.domain.repositories.RouteRepository
import com.marineplay.chartplotter.domain.usecases.*
import com.marineplay.chartplotter.LocationManager
import com.marineplay.chartplotter.data.SystemSettings
import com.marineplay.chartplotter.data.SystemSettingsReader
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
    val currentNavigationRoute: Route? = null // 현재 항해 중인 경로
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
 * 항적별 기록 상태
 */
data class TrackRecordingState(
    val trackId: String,
    val startTime: Long,
    val points: MutableList<TrackPoint> = mutableListOf(),
    val lastTrackPointTime: Long = 0,
    val lastTrackPointLocation: LatLng? = null
)

/**
 * 항적 관련 UI 상태
 */
data class TrackUiState(
    val isRecordingTrack: Boolean = false, // 하위 호환성 유지
    val currentRecordingTrack: Track? = null, // 하위 호환성 유지
    val trackRecordingStartTime: Long = 0, // 하위 호환성 유지
    val trackPoints: List<TrackPoint> = emptyList(), // 하위 호환성 유지
    val lastTrackPointTime: Long = 0, // 하위 호환성 유지
    val lastTrackPointLocation: LatLng? = null, // 하위 호환성 유지
    // 여러 항적 동시 기록 지원
    val recordingTracks: Map<String, TrackRecordingState> = emptyMap(), // trackId -> 상태
    val selectedTrackForRecords: Track? = null,
    val selectedTrackForSettings: Track? = null, // 설정 다이얼로그용 선택된 항적
    val highlightedTrackRecord: Pair<String, String>? = null // (trackId, date) - 날짜별 하이라이트
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
    private val startTrackRecordingUseCase: StartTrackRecordingUseCase,
    private val stopTrackRecordingUseCase: StopTrackRecordingUseCase,
    private val addTrackPointUseCase: AddTrackPointUseCase,
    private val routeUseCase: RouteUseCase,
    private val connectRouteToNavigationUseCase: ConnectRouteToNavigationUseCase,
    // Repository
    private val pointRepository: PointRepository,
    private val trackRepository: TrackRepository,
    private val routeRepository: RouteRepository,
    private val systemSettingsReader: SystemSettingsReader
) : ViewModel() {
    
    // ========== UI 상태 ==========
    var pointUiState by mutableStateOf(PointUiState())
        private set
    
    var mapUiState by mutableStateOf(MapUiState())
        private set
    
    var gpsUiState by mutableStateOf(GpsUiState())
        private set
    
    var trackUiState by mutableStateOf(TrackUiState())
        private set
    
    var dialogUiState by mutableStateOf(DialogUiState())
        private set
    
    // 시스템 설정 상태 (읽기 전용 - SystemSetting 앱에서 관리)
    var systemSettings by mutableStateOf(SystemSettings())
        private set
    
    // 항적 정보 캐시 (성능 최적화)
    private val trackCache = mutableMapOf<String, Track>()
    
    init {
        // 시스템 설정 로드 (SystemSetting 앱의 ContentProvider를 통해)
        loadSystemSettings()
        
        // 앱 시작 시 자동 기록 시작 (isRecording=true인 항적)
        // 지연 로드: UI가 준비된 후 백그라운드에서 처리하여 초기 로딩 시간 단축
        viewModelScope.launch {
            // 약간의 지연을 두어 UI가 먼저 표시되도록 함
            kotlinx.coroutines.delay(100)
            
            val recordingTracks = trackRepository.getRecordingTracks()
            if (recordingTracks.isNotEmpty()) {
                val updatedRecordingTracks = mutableMapOf<String, TrackRecordingState>()
                recordingTracks.forEach { track ->
                    val recordingState = TrackRecordingState(
                        trackId = track.id,
                        startTime = System.currentTimeMillis() // 새로운 시작 시간
                    )
                    updatedRecordingTracks[track.id] = recordingState
                    
                    // 캐시에 저장
                    trackCache[track.id] = track
                    
                    // UseCase 실행 (타이머 시작 등)
                    startTrackRecordingUseCase.execute(track)
                }
                
                trackUiState = trackUiState.copy(
                    recordingTracks = updatedRecordingTracks,
                    isRecordingTrack = true,
                    currentRecordingTrack = recordingTracks.firstOrNull(),
                    trackRecordingStartTime = recordingTracks.firstOrNull()?.let { 
                        updatedRecordingTracks[it.id]?.startTime ?: 0L 
                    } ?: 0L
                )
            }
        }
    }
    
    /**
     * 항적 정보를 캐시에서 가져오거나 DB에서 로드하여 캐시에 저장
     */
    private suspend fun getTrackFromCache(trackId: String): Track? {
        return trackCache[trackId] ?: run {
            trackRepository.getAllTracks().find { it.id == trackId }?.also {
                trackCache[trackId] = it
            }
        }
    }
    
    /**
     * 항적 캐시 무효화 (항적이 변경되었을 때 호출)
     */
    private fun invalidateTrackCache(trackId: String? = null) {
        if (trackId != null) {
            trackCache.remove(trackId)
        } else {
            trackCache.clear()
        }
    }
    
    fun loadSystemSettings() {
        systemSettings = systemSettingsReader.loadSettings()
    }
    
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
    
    // ========== TrackUiState 업데이트 함수들 ==========
    
    fun updateIsRecordingTrack(recording: Boolean) {
        trackUiState = trackUiState.copy(isRecordingTrack = recording)
    }
    
    fun updateCurrentRecordingTrack(track: Track?) {
        trackUiState = trackUiState.copy(currentRecordingTrack = track)
    }
    
    fun updateTrackRecordingStartTime(time: Long) {
        trackUiState = trackUiState.copy(trackRecordingStartTime = time)
    }
    
    fun updateTrackPoints(points: List<TrackPoint>) {
        trackUiState = trackUiState.copy(trackPoints = points)
    }
    
    fun addTrackPoint(point: TrackPoint) {
        trackUiState = trackUiState.copy(
            trackPoints = trackUiState.trackPoints + point,
            lastTrackPointTime = point.timestamp,
            lastTrackPointLocation = LatLng(point.latitude, point.longitude)
        )
    }
    
    fun clearTrackPoints() {
        trackUiState = trackUiState.copy(
            trackPoints = emptyList(),
            lastTrackPointTime = 0,
            lastTrackPointLocation = null
        )
    }
    
    fun updateSelectedTrackForRecords(track: Track?) {
        trackUiState = trackUiState.copy(selectedTrackForRecords = track)
    }
    
    fun updateSelectedTrackForSettings(track: Track?) {
        trackUiState = trackUiState.copy(selectedTrackForSettings = track)
    }
    
    fun updateHighlightedTrackRecord(record: Pair<String, String>?) {
        trackUiState = trackUiState.copy(highlightedTrackRecord = record)
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
    
    // 시스템 설정은 읽기 전용입니다.
    // 설정 변경은 SystemSetting 앱에서만 가능합니다.
    // 이 함수는 로컬 상태만 업데이트합니다 (실제 저장은 SystemSetting 앱에서 수행).
    private fun updateSystemSettings(settings: SystemSettings) {
        systemSettings = settings
        // 실제 저장은 SystemSetting 앱에서만 수행됩니다.
        // 필요시 loadSystemSettings()를 호출하여 최신 설정을 다시 로드할 수 있습니다.
    }
    
    fun updateLanguage(language: String) {
        val newSettings = systemSettings.copy(language = language)
        updateSystemSettings(newSettings)
    }
    
    fun updateVesselSettings(length: Float, width: Float) {
        val newSettings = systemSettings.copy(vesselLength = length, vesselWidth = width)
        updateSystemSettings(newSettings)
    }
    
    fun updateFontSize(size: Float) {
        val newSettings = systemSettings.copy(fontSize = size)
        updateSystemSettings(newSettings)
    }
    
    fun updateButtonVolume(volume: Int) {
        val newSettings = systemSettings.copy(buttonVolume = volume)
        updateSystemSettings(newSettings)
    }
    
    fun updateTimeFormat(format: String) {
        val newSettings = systemSettings.copy(timeFormat = format)
        updateSystemSettings(newSettings)
    }
    
    fun updateDateFormat(format: String) {
        val newSettings = systemSettings.copy(dateFormat = format)
        updateSystemSettings(newSettings)
    }
    
    fun updateGeodeticSystem(system: String) {
        val newSettings = systemSettings.copy(geodeticSystem = system)
        updateSystemSettings(newSettings)
    }
    
    fun updateCoordinateFormat(format: String) {
        val newSettings = systemSettings.copy(coordinateFormat = format)
        updateSystemSettings(newSettings)
    }
    
    fun updateDeclinationMode(mode: String) {
        val newSettings = systemSettings.copy(declinationMode = mode)
        updateSystemSettings(newSettings)
    }
    
    fun updateDeclinationValue(value: Float) {
        val newSettings = systemSettings.copy(declinationValue = value)
        updateSystemSettings(newSettings)
    }
    
    fun updatePingSync(enabled: Boolean) {
        val newSettings = systemSettings.copy(pingSync = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateAdvancedFeature(key: String, enabled: Boolean) {
        val newFeatures = systemSettings.advancedFeatures.toMutableMap()
        newFeatures[key] = enabled
        val newSettings = systemSettings.copy(advancedFeatures = newFeatures)
        updateSystemSettings(newSettings)
    }
    
    // 설정 초기화는 SystemSetting 앱에서만 가능합니다.
    // 이 함수는 최신 설정을 다시 로드합니다.
    fun reloadSystemSettings() {
        loadSystemSettings()
    }
    
    // 항해 설정 업데이트 함수들
    fun updateArrivalRadius(radius: Float) {
        val newSettings = systemSettings.copy(arrivalRadius = radius)
        updateSystemSettings(newSettings)
    }
    
    fun updateXteLimit(limit: Float) {
        val newSettings = systemSettings.copy(xteLimit = limit)
        updateSystemSettings(newSettings)
    }
    
    fun updateXteAlertEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(xteAlertEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    // 지도 설정 업데이트 함수들
    fun updateBoat3DEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(boat3DEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateDistanceCircleRadius(radius: Float) {
        val newSettings = systemSettings.copy(distanceCircleRadius = radius)
        updateSystemSettings(newSettings)
    }
    
    fun updateHeadingLineEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(headingLineEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateCourseLineEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(courseLineEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateExtensionLength(length: Float) {
        val newSettings = systemSettings.copy(extensionLength = length)
        updateSystemSettings(newSettings)
    }
    
    fun updateGridLineEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(gridLineEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateDestinationVisible(visible: Boolean) {
        val newSettings = systemSettings.copy(destinationVisible = visible)
        updateSystemSettings(newSettings)
    }
    
    fun updateRouteVisible(visible: Boolean) {
        val newSettings = systemSettings.copy(routeVisible = visible)
        updateSystemSettings(newSettings)
    }
    
    fun updateTrackVisible(visible: Boolean) {
        val newSettings = systemSettings.copy(trackVisible = visible)
        updateSystemSettings(newSettings)
    }
    
    fun updateMapHidden(hidden: Boolean) {
        val newSettings = systemSettings.copy(mapHidden = hidden)
        updateSystemSettings(newSettings)
    }
    
    // 경보 설정 업데이트 함수들
    fun updateAlertEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(alertEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateAlertSetting(alertType: String, enabled: Boolean) {
        val newAlertSettings = systemSettings.alertSettings.toMutableMap()
        newAlertSettings[alertType] = enabled
        val newSettings = systemSettings.copy(alertSettings = newAlertSettings)
        updateSystemSettings(newSettings)
    }
    
    // 단위 설정 업데이트 함수들
    fun updateDistanceUnit(unit: String) {
        val newSettings = systemSettings.copy(distanceUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateSmallDistanceUnit(unit: String) {
        val newSettings = systemSettings.copy(smallDistanceUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateSpeedUnit(unit: String) {
        val newSettings = systemSettings.copy(speedUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateWindSpeedUnit(unit: String) {
        val newSettings = systemSettings.copy(windSpeedUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateDepthUnit(unit: String) {
        val newSettings = systemSettings.copy(depthUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateAltitudeUnit(unit: String) {
        val newSettings = systemSettings.copy(altitudeUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateAltitudeDatum(datum: String) {
        val newSettings = systemSettings.copy(altitudeDatum = datum)
        updateSystemSettings(newSettings)
    }
    
    fun updateHeadingUnit(unit: String) {
        val newSettings = systemSettings.copy(headingUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateTemperatureUnit(unit: String) {
        val newSettings = systemSettings.copy(temperatureUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateCapacityUnit(unit: String) {
        val newSettings = systemSettings.copy(capacityUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateFuelEfficiencyUnit(unit: String) {
        val newSettings = systemSettings.copy(fuelEfficiencyUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updatePressureUnit(unit: String) {
        val newSettings = systemSettings.copy(pressureUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    fun updateAtmosphericPressureUnit(unit: String) {
        val newSettings = systemSettings.copy(atmosphericPressureUnit = unit)
        updateSystemSettings(newSettings)
    }
    
    // 무선 설정 업데이트 함수들
    fun updateBluetoothEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(bluetoothEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateBluetoothPairedDevices(devices: List<String>) {
        val newSettings = systemSettings.copy(bluetoothPairedDevices = devices)
        updateSystemSettings(newSettings)
    }
    
    fun updateWifiEnabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(wifiEnabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateWifiConnectedNetwork(network: String?) {
        val newSettings = systemSettings.copy(wifiConnectedNetwork = network)
        updateSystemSettings(newSettings)
    }
    
    // 네트워크 설정 업데이트 함수들
    fun updateNmea2000Enabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(nmea2000Enabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateNmea2000Setting(key: String, value: String) {
        val newNmea2000Settings = systemSettings.nmea2000Settings.toMutableMap()
        newNmea2000Settings[key] = value
        val newSettings = systemSettings.copy(nmea2000Settings = newNmea2000Settings)
        updateSystemSettings(newSettings)
    }
    
    fun updateNmea0183Enabled(enabled: Boolean) {
        val newSettings = systemSettings.copy(nmea0183Enabled = enabled)
        updateSystemSettings(newSettings)
    }
    
    fun updateNmea0183Setting(key: String, value: String) {
        val newNmea0183Settings = systemSettings.nmea0183Settings.toMutableMap()
        newNmea0183Settings[key] = value
        val newSettings = systemSettings.copy(nmea0183Settings = newNmea0183Settings)
        updateSystemSettings(newSettings)
    }
    
    // 선박 설정 업데이트 함수들
    fun updateMmsi(mmsi: String) {
        val newSettings = systemSettings.copy(mmsi = mmsi)
        updateSystemSettings(newSettings)
    }
    
    fun updateAisCourseExtension(extension: Float) {
        val newSettings = systemSettings.copy(aisCourseExtension = extension)
        updateSystemSettings(newSettings)
    }
    
    fun updateVesselTrackingSetting(key: String, enabled: Boolean) {
        val newVesselTrackingSettings = systemSettings.vesselTrackingSettings.toMutableMap()
        newVesselTrackingSettings[key] = enabled
        val newSettings = systemSettings.copy(vesselTrackingSettings = newVesselTrackingSettings)
        updateSystemSettings(newSettings)
    }
    
    fun updateRecordLength(length: Int) {
        val newSettings = systemSettings.copy(recordLength = length)
        updateSystemSettings(newSettings)
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
     * 항적 기록 시작 (단일 항적만 기록 가능)
     * 다른 항적이 기록 중이면 자동으로 중지하고 저장
     */
    fun startTrackRecording(track: Track) {
        // 이미 기록 중인 항적이 있으면 자동으로 중지하고 저장
        val currentRecordingTracks = trackUiState.recordingTracks
        if (currentRecordingTracks.isNotEmpty()) {
            // 기존 기록 중인 항적 모두 중지
            currentRecordingTracks.keys.forEach { existingTrackId ->
                stopTrackRecording(existingTrackId)
            }
        }
        
        // TrackRepository에서 항적 기록 상태 업데이트
        viewModelScope.launch {
            trackRepository.setTrackRecording(track.id, true)
        }
        
        // 새로운 기록 상태 추가
        val newRecordingState = TrackRecordingState(
            trackId = track.id,
            startTime = System.currentTimeMillis()
        )
        val updatedRecordingTracks = mutableMapOf<String, TrackRecordingState>()
        updatedRecordingTracks[track.id] = newRecordingState
        
        trackUiState = trackUiState.copy(
            recordingTracks = updatedRecordingTracks,
            // 하위 호환성 유지
            isRecordingTrack = true,
            currentRecordingTrack = track,
            trackRecordingStartTime = newRecordingState.startTime
        )
        
        startTrackRecordingUseCase.execute(track)
    }
    
    /**
     * 항적 기록 중지 (특정 항적)
     * TrackPoint는 이미 실시간으로 저장되었으므로, 기록 상태만 업데이트
     */
    fun stopTrackRecording(trackId: String? = null) {
        val currentTrackUiState = trackUiState
        val targetTrackId = trackId ?: currentTrackUiState.currentRecordingTrack?.id
        
        if (targetTrackId == null) {
            return
        }
        
        val recordingState = currentTrackUiState.recordingTracks[targetTrackId] ?: return
        
        // TrackRepository에서 항적 기록 상태 업데이트
        viewModelScope.launch {
            trackRepository.setTrackRecording(targetTrackId, false)
        }
        
        // 기록 상태에서 제거
        val updatedRecordingTracks = currentTrackUiState.recordingTracks.toMutableMap()
        updatedRecordingTracks.remove(targetTrackId)
        
        // 하위 호환성 유지
        val isAnyRecording = updatedRecordingTracks.isNotEmpty()
        val remainingTrack = if (isAnyRecording) {
            runBlocking {
                trackRepository.getAllTracks().find { updatedRecordingTracks.containsKey(it.id) }
            }
        } else {
            null
        }
        
        trackUiState = trackUiState.copy(
            recordingTracks = updatedRecordingTracks,
            isRecordingTrack = isAnyRecording,
            currentRecordingTrack = remainingTrack,
            trackRecordingStartTime = if (remainingTrack != null) {
                updatedRecordingTracks[remainingTrack.id]?.startTime ?: 0L
            } else {
                0L
            }
        )
    }
    
    /**
     * 모든 항적 기록 중지
     */
    fun stopAllTrackRecording() {
        val recordingTrackIds = trackUiState.recordingTracks.keys.toList()
        
        recordingTrackIds.forEach { trackId ->
            stopTrackRecording(trackId)
        }
    }
    
    /**
     * 항적 점 추가 (필요한 경우) - 여러 항적 동시 기록 지원
     * @param latitude 현재 위도
     * @param longitude 현재 경도
     * @param isTimerTriggered 타이머에서 호출되었는지 여부 (시간 기준 항적일 때만 사용)
     */
    fun addTrackPointIfNeeded(latitude: Double, longitude: Double, isTimerTriggered: Boolean = false): List<Pair<String, TrackPoint>> {
        val currentTrackUiState = trackUiState
        if (currentTrackUiState.recordingTracks.isEmpty()) {
            return emptyList()
        }
        
        val currentTime = System.currentTimeMillis()
        updateGpsLocation(latitude, longitude, true)
        
        val addedPoints = mutableListOf<Pair<String, TrackPoint>>()
        val updatedRecordingTracks = currentTrackUiState.recordingTracks.toMutableMap()
        
        // 각 기록 중인 항적에 대해 점 추가
        currentTrackUiState.recordingTracks.forEach { (trackId, recordingState) ->
            // 캐시에서 항적 정보 가져오기 (성능 최적화)
            val track = runBlocking { getTrackFromCache(trackId) } ?: return@forEach
        
        val newPoint = addTrackPointUseCase.execute(
            latitude = latitude,
            longitude = longitude,
            currentTime = currentTime,
                lastTrackPointTime = recordingState.lastTrackPointTime,
                lastTrackPointLocation = recordingState.lastTrackPointLocation,
                intervalType = track.intervalType,
                timeInterval = track.timeInterval,
                distanceInterval = track.distanceInterval,
                isTimerTriggered = isTimerTriggered
        )
        
        if (newPoint != null) {
                // TrackPoint를 실시간으로 DB에 저장 (앱 종료 시에도 데이터 손실 없음)
                viewModelScope.launch {
                    trackRepository.addTrackPoint(trackId, newPoint)
                }
                
                val updatedState = recordingState.copy(
                    points = (recordingState.points + newPoint).toMutableList(),
                    lastTrackPointTime = currentTime,
                    lastTrackPointLocation = LatLng(latitude, longitude)
                )
                updatedRecordingTracks[trackId] = updatedState
                addedPoints.add(Pair(trackId, newPoint))
                
                // 하위 호환성 유지 (첫 번째 기록 중인 항적)
                if (trackId == currentTrackUiState.currentRecordingTrack?.id) {
            addTrackPoint(newPoint)
        }
            }
        }
        
        if (updatedRecordingTracks != currentTrackUiState.recordingTracks) {
            trackUiState = trackUiState.copy(
                recordingTracks = updatedRecordingTracks,
                lastTrackPointTime = currentTime,
                lastTrackPointLocation = LatLng(latitude, longitude)
            )
        }
        
        return addedPoints
    }
    
    /**
     * 포인트 목록 로드
     */
    fun loadPointsFromLocal(): List<DataSavedPoint> {
        return runBlocking {
            pointRepository.getAllSavedPoints()
        }
    }
    
    /**
     * 항적 목록 가져오기
     */
    fun getTracks(): List<Track> {
        return runBlocking {
            trackRepository.getAllTracks()
        }
    }
    
    /**
     * 특정 항적 가져오기 (캐시 사용, 성능 최적화)
     */
    fun getTrack(trackId: String): Track? {
        return runBlocking {
            getTrackFromCache(trackId)
        }
    }
    
    /**
     * 항적 추가
     */
    fun addTrack(
        name: String, 
        color: Color,
        intervalType: String = "time",
        timeInterval: Long = 5000L,
        distanceInterval: Double = 10.0
    ) {
        viewModelScope.launch {
            val newTrack = trackRepository.addTrack(name, color, intervalType, timeInterval, distanceInterval)
            // 새 항적을 캐시에 추가
            trackCache[newTrack.id] = newTrack
        }
    }
    
    /**
     * 항적 설정 업데이트
     */
    fun updateTrackSettings(
        trackId: String,
        intervalType: String? = null,
        timeInterval: Long? = null,
        distanceInterval: Double? = null
    ): Boolean {
        val result = runBlocking {
            trackRepository.updateTrackSettings(trackId, intervalType, timeInterval, distanceInterval)
        }
        // 캐시 무효화 (항적 설정이 변경되었으므로)
        if (result) {
            invalidateTrackCache(trackId)
        }
        return result
    }
    
    /**
     * 항적 기록 on/off 토글
     */
    fun toggleTrackRecording(trackId: String) {
        val isRecording = runBlocking { trackRepository.isTrackRecording(trackId) }
        // 캐시에서 항적 정보 가져오기 (성능 최적화)
        val track = runBlocking { getTrackFromCache(trackId) } ?: return
        
        if (isRecording) {
            // 기록 중지
            stopTrackRecording(trackId)
        } else {
            // 기록 시작
            startTrackRecording(track)
        }
    }
    
    /**
     * 날짜별 항적 포인트 가져오기 (모든 항적)
     */
    fun getPointsByDate(date: String): List<Pair<String, TrackPoint>> {
        return runBlocking {
            trackRepository.getPointsByDate(date)
        }
    }
    
    /**
     * 특정 항적의 날짜별 포인트 가져오기
     */
    fun getTrackPointsByDate(trackId: String, date: String): List<TrackPoint> {
        return runBlocking {
            trackRepository.getTrackPointsByDate(trackId, date)
        }
    }
    
    /**
     * 특정 항적의 시간 범위별 포인트 가져오기
     */
    fun getTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): List<TrackPoint> {
        return runBlocking {
            trackRepository.getTrackPointsByTimeRange(trackId, startTime, endTime)
        }
    }
    
    /**
     * 특정 항적의 최근 N개 포인트 가져오기
     */
    fun getRecentTrackPoints(trackId: String, limit: Int = 2000): List<TrackPoint> {
        return runBlocking {
            trackRepository.getRecentTrackPoints(trackId, limit)
        }
    }
    
    /**
     * 모든 날짜 목록 가져오기
     */
    fun getAllDates(): List<String> {
        return runBlocking {
            trackRepository.getAllDates()
        }
    }
    
    /**
     * 특정 항적의 날짜 목록 가져오기
     */
    fun getDatesByTrackId(trackId: String): List<String> {
        return runBlocking {
            trackRepository.getDatesByTrackId(trackId)
        }
    }
    
    /**
     * 특정 항적의 모든 포인트 가져오기
     */
    fun getTrackPoints(trackId: String): List<TrackPoint> {
        return runBlocking {
            trackRepository.getTrackPoints(trackId)
        }
    }
    
    /**
     * 항적 삭제
     */
    fun deleteTrack(trackId: String) {
        viewModelScope.launch {
            trackRepository.deleteTrack(trackId)
            // 캐시에서 제거
            invalidateTrackCache(trackId)
        }
    }
    
    /**
     * 항적 표시/숨김 설정
     * @return true: 성공, false: 제한으로 인해 실패
     */
    fun setTrackVisibility(trackId: String, isVisible: Boolean): Boolean {
        // 표시를 켜려고 할 때만 제한 체크
        if (isVisible) {
            // 현재 표시 중인 포인트 수 계산 (최근 2000개 제한)
            val currentVisiblePointCount = getTracks()
                .filter { it.isVisible && it.id != trackId } // 현재 항적 제외
                .sumOf { it.points.size.coerceAtMost(2000) }
            
            // 해당 항적의 포인트 수
            val track = runBlocking { getTrackFromCache(trackId) }
            val trackPointCount = track?.points?.size?.coerceAtMost(2000) ?: 0
            
            // 제한 체크 (최대 20000개 포인트만 표시 가능, 항적당 최대 2000개)
            if (currentVisiblePointCount + trackPointCount > 20000) {
                // 제한 알림 표시
                dialogUiState = dialogUiState.copy(showTrackLimitDialog = true)
                return false
            }
        }
        
        viewModelScope.launch {
            trackRepository.setTrackVisibility(trackId, isVisible)
            // 캐시 무효화 (항적 표시 상태가 변경되었으므로)
            invalidateTrackCache(trackId)
        }
        return true
    }
    
    /**
     * 항적 표시 제한 알림 다이얼로그 표시/숨김
     */
    fun updateShowTrackLimitDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showTrackLimitDialog = show)
    }
    
    fun updateShowRouteCreateDialog(show: Boolean) {
        dialogUiState = dialogUiState.copy(showRouteCreateDialog = show)
    }
    
    /**
     * 항적 기록 삭제
     */
    /**
     * 특정 항적의 날짜별 포인트 삭제
     */
    fun deleteTrackPointsByDate(trackId: String, date: String) {
        viewModelScope.launch {
            trackRepository.deleteTrackPointsByDate(trackId, date)
            invalidateTrackCache(trackId)
        }
    }
    
    /**
     * 특정 항적의 시간 범위별 포인트 삭제
     */
    fun deleteTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            trackRepository.deleteTrackPointsByTimeRange(trackId, startTime, endTime)
            invalidateTrackCache(trackId)
        }
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
            trackRepository: TrackRepository,
            routeRepository: RouteRepository,
            locationManager: LocationManager?,
            systemSettingsReader: SystemSettingsReader
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    // UseCase 생성
                    val calculateBearingUseCase = CalculateBearingUseCase()
                    val calculateDistanceUseCase = CalculateDistanceUseCase()
                    val getNextAvailablePointNumberUseCase = GetNextAvailablePointNumberUseCase(pointRepository)
                    val registerPointUseCase = RegisterPointUseCase(pointRepository, getNextAvailablePointNumberUseCase)
                    val deletePointUseCase = DeletePointUseCase(pointRepository)
                    val updatePointUseCase = UpdatePointUseCase(pointRepository)
                    val mapRotationUseCase = MapRotationUseCase(locationManager, calculateBearingUseCase)
                    val zoomUseCase = ZoomUseCase()
                    val startTrackRecordingUseCase = StartTrackRecordingUseCase()
                    val stopTrackRecordingUseCase = StopTrackRecordingUseCase()
                    val addTrackPointUseCase = AddTrackPointUseCase(calculateDistanceUseCase)
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
                        startTrackRecordingUseCase = startTrackRecordingUseCase,
                        stopTrackRecordingUseCase = stopTrackRecordingUseCase,
                        addTrackPointUseCase = addTrackPointUseCase,
                        routeUseCase = routeUseCase,
                        connectRouteToNavigationUseCase = connectRouteToNavigationUseCase,
                        pointRepository = pointRepository,
                        trackRepository = trackRepository,
                        routeRepository = routeRepository,
                        systemSettingsReader = systemSettingsReader
                    ) as T
                }
            }
        }
    }
}
