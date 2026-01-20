package com.marineplay.chartplotter.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marineplay.chartplotter.SavedPoint
import com.marineplay.chartplotter.Track
import com.marineplay.chartplotter.TrackPoint
import com.marineplay.chartplotter.domain.usecases.*
import com.marineplay.chartplotter.helpers.PointHelper
import com.marineplay.chartplotter.LocationManager
import com.marineplay.chartplotter.TrackManager
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
    val currentMenu: String = "main", // "main", "point", "ais", "navigation", "track", "display"
    val isZoomInLongPressed: Boolean = false,
    val isZoomOutLongPressed: Boolean = false,
    val popupPosition: android.graphics.PointF? = null,
    val showSettingsScreen: Boolean = false // 설정 화면 표시 여부
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
 * 항적 관련 UI 상태
 */
data class TrackUiState(
    val isRecordingTrack: Boolean = false,
    val currentRecordingTrack: Track? = null,
    val trackRecordingStartTime: Long = 0,
    val trackPoints: List<TrackPoint> = emptyList(),
    val lastTrackPointTime: Long = 0,
    val lastTrackPointLocation: LatLng? = null,
    val selectedTrackForRecords: Track? = null,
    val highlightedTrackRecord: Pair<String, String>? = null // (trackId, recordId)
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
    val showInfoDialog: Boolean = false
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
    // Helper들 (UseCase에서 사용)
    private val pointHelper: PointHelper,
    private val trackManager: TrackManager,
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
    
    init {
        // 시스템 설정 로드 (SystemSetting 앱의 ContentProvider를 통해)
        loadSystemSettings()
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
    ): List<com.marineplay.chartplotter.helpers.PointHelper.SavedPoint> {
        return registerPointUseCase.execute(latLng, name, color, iconType)
    }
    
    /**
     * 포인트 삭제
     */
    fun deletePoint(point: com.marineplay.chartplotter.helpers.PointHelper.SavedPoint): List<com.marineplay.chartplotter.helpers.PointHelper.SavedPoint> {
        return deletePointUseCase.execute(point)
    }
    
    /**
     * 포인트 업데이트
     */
    fun updatePoint(
        originalPoint: com.marineplay.chartplotter.helpers.PointHelper.SavedPoint,
        newName: String,
        newColor: Color
    ): List<com.marineplay.chartplotter.helpers.PointHelper.SavedPoint> {
        return updatePointUseCase.execute(originalPoint, newName, newColor)
    }
    
    /**
     * 다음 사용 가능한 포인트 번호 가져오기
     */
    fun getNextAvailablePointNumber(): Int {
        return getNextAvailablePointNumberUseCase.execute()
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
     * 항적 기록 시작
     */
    fun startTrackRecording(track: Track) {
        updateCurrentRecordingTrack(track)
        updateIsRecordingTrack(true)
        updateTrackRecordingStartTime(System.currentTimeMillis())
        clearTrackPoints()
        startTrackRecordingUseCase.execute(track)
    }
    
    /**
     * 항적 기록 중지
     */
    fun stopTrackRecording(): com.marineplay.chartplotter.TrackRecord? {
        val currentTrackUiState = trackUiState
        if (!currentTrackUiState.isRecordingTrack || currentTrackUiState.currentRecordingTrack == null) {
            return null
        }
        
        val endTime = System.currentTimeMillis()
        val record = if (currentTrackUiState.trackPoints.isNotEmpty()) {
            stopTrackRecordingUseCase.execute(
                trackId = currentTrackUiState.currentRecordingTrack!!.id,
                startTime = currentTrackUiState.trackRecordingStartTime,
                endTime = endTime,
                points = currentTrackUiState.trackPoints
            )
        } else {
            null
        }
        
        updateCurrentRecordingTrack(null)
        updateIsRecordingTrack(false)
        clearTrackPoints()
        
        return record
    }
    
    /**
     * 항적 점 추가 (필요한 경우)
     */
    fun addTrackPointIfNeeded(latitude: Double, longitude: Double): TrackPoint? {
        val currentTrackUiState = trackUiState
        if (!currentTrackUiState.isRecordingTrack || currentTrackUiState.currentRecordingTrack == null) {
            return null
        }
        
        val currentTime = System.currentTimeMillis()
        updateGpsLocation(latitude, longitude, true)
        
        val newPoint = addTrackPointUseCase.execute(
            latitude = latitude,
            longitude = longitude,
            currentTime = currentTime,
            lastTrackPointTime = currentTrackUiState.lastTrackPointTime,
            lastTrackPointLocation = currentTrackUiState.lastTrackPointLocation
        )
        
        if (newPoint != null) {
            addTrackPoint(newPoint)
        }
        
        return newPoint
    }
    
    /**
     * 포인트 목록 로드
     */
    fun loadPointsFromLocal(): List<com.marineplay.chartplotter.helpers.PointHelper.SavedPoint> {
        return pointHelper.loadPointsFromLocal()
    }
    
    /**
     * 항적 목록 가져오기
     */
    fun getTracks(): List<Track> {
        return trackManager.getTracks()
    }
    
    /**
     * 항적 설정 가져오기
     */
    fun getTrackSettings(): com.marineplay.chartplotter.TrackSettings {
        return trackManager.settings
    }
    
    /**
     * 항적 설정 저장
     */
    fun saveTrackSettings(settings: com.marineplay.chartplotter.TrackSettings) {
        trackManager.saveSettings(settings)
    }
    
    /**
     * 항적 추가
     */
    fun addTrack(name: String, color: Color) {
        trackManager.addTrack(name, color)
    }
    
    /**
     * 항적 삭제
     */
    fun deleteTrack(trackId: String) {
        trackManager.deleteTrack(trackId)
    }
    
    /**
     * 항적 표시/숨김 설정
     */
    fun setTrackVisibility(trackId: String, isVisible: Boolean) {
        trackManager.setTrackVisibility(trackId, isVisible)
    }
    
    /**
     * 항적 기록 삭제
     */
    fun deleteTrackRecord(trackId: String, recordId: String) {
        trackManager.deleteTrackRecord(trackId, recordId)
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
        savedPoints: List<com.marineplay.chartplotter.helpers.PointHelper.SavedPoint>,
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
                    // PointHelper.SavedPoint를 com.marineplay.chartplotter.SavedPoint로 변환
                    val savedPoint = SavedPoint(
                        name = closestPoint.name,
                        latitude = closestPoint.latitude,
                        longitude = closestPoint.longitude,
                        color = Color(closestPoint.color.toArgb()),
                        iconType = closestPoint.iconType,
                        timestamp = closestPoint.timestamp
                    )
                    updateSelectedPoint(savedPoint)
                    updateEditPointName(closestPoint.name)
                    updateEditSelectedColor(Color(closestPoint.color.toArgb()))
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
            pointHelper: PointHelper,
            trackManager: TrackManager,
            locationManager: LocationManager?,
            systemSettingsReader: SystemSettingsReader
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    // UseCase 생성
                    val calculateBearingUseCase = CalculateBearingUseCase()
                    val calculateDistanceUseCase = CalculateDistanceUseCase()
                    val getNextAvailablePointNumberUseCase = GetNextAvailablePointNumberUseCase(pointHelper)
                    val registerPointUseCase = RegisterPointUseCase(pointHelper)
                    val deletePointUseCase = DeletePointUseCase(pointHelper)
                    val updatePointUseCase = UpdatePointUseCase(pointHelper)
                    val mapRotationUseCase = MapRotationUseCase(locationManager, calculateBearingUseCase)
                    val zoomUseCase = ZoomUseCase()
                    val startTrackRecordingUseCase = StartTrackRecordingUseCase(trackManager)
                    val stopTrackRecordingUseCase = StopTrackRecordingUseCase(trackManager)
                    val addTrackPointUseCase = AddTrackPointUseCase(trackManager, calculateDistanceUseCase)
                    
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
                        pointHelper = pointHelper,
                        trackManager = trackManager,
                        systemSettingsReader = systemSettingsReader
                    ) as T
                }
            }
        }
    }
}
