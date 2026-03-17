package com.kumhomarine.chartplotter.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kumhomarine.chartplotter.data.ChartSettingsRepository
import com.kumhomarine.chartplotter.data.SystemSettings
import com.kumhomarine.chartplotter.data.SystemSettingsReader

/**
 * 시스템 설정 전용 ViewModel
 * SystemSetting 앱의 ContentProvider에서 시스템 설정을 읽고,
 * ChartSettingsRepository에서 지도/선박(차트) 설정을 읽어 merge합니다.
 */
class SettingsViewModel(
    private val systemSettingsReader: SystemSettingsReader,
    private val chartSettingsRepository: ChartSettingsRepository
) : ViewModel() {
    
    // 시스템 설정 상태 (시스템 설정 + 지도/선박 설정 merge)
    var systemSettings by mutableStateOf(SystemSettings())
        private set
    
    init {
        loadSystemSettings()
    }
    
    fun loadSystemSettings() {
        val sys = systemSettingsReader.loadSettings()
        val chart = chartSettingsRepository.loadChartSettings()
        systemSettings = sys.copy(
            boat3DEnabled = chart.boat3DEnabled,
            distanceCircleRadius = chart.distanceCircleRadius,
            headingLineEnabled = chart.headingLineEnabled,
            courseLineEnabled = chart.courseLineEnabled,
            extensionLength = chart.extensionLength,
            gridLineEnabled = chart.gridLineEnabled,
            destinationVisible = chart.destinationVisible,
            routeVisible = chart.routeVisible,
            trackVisible = chart.trackVisible,
            mapHidden = chart.mapHidden,
            aisCourseExtension = chart.aisCourseExtension,
            vesselTrackingSettings = chart.vesselTrackingSettings
        )
    }
    
    // 설정 초기화는 SystemSetting 앱에서만 가능합니다.
    // 이 함수는 최신 설정을 다시 로드합니다.
    fun reloadSystemSettings() {
        loadSystemSettings()
    }
    
    // 시스템 설정은 읽기 전용입니다.
    // 설정 변경은 SystemSetting 앱에서만 가능합니다.
    // 이 함수는 로컬 상태만 업데이트합니다.
    private fun updateSystemSettings(settings: SystemSettings) {
        systemSettings = settings
    }
    
    // ========== 일반 설정 ==========
    // 언어 설정은 SystemSetting 앱에서만 변경 가능. ChartPlotter는 ContentProvider에서 읽어서 적용만 함.
    
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
    
    // ========== 항해 설정 ==========
    
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
    
    // ========== 지도 설정 (ChartPlotter 로컬 저장) ==========
    
    fun updateBoat3DEnabled(enabled: Boolean) {
        chartSettingsRepository.saveBoat3DEnabled(enabled)
        updateSystemSettings(systemSettings.copy(boat3DEnabled = enabled))
    }
    
    fun updateDistanceCircleRadius(radius: Float) {
        chartSettingsRepository.saveDistanceCircleRadius(radius)
        updateSystemSettings(systemSettings.copy(distanceCircleRadius = radius))
    }
    
    fun updateHeadingLineEnabled(enabled: Boolean) {
        chartSettingsRepository.saveHeadingLineEnabled(enabled)
        updateSystemSettings(systemSettings.copy(headingLineEnabled = enabled))
    }
    
    fun updateCourseLineEnabled(enabled: Boolean) {
        chartSettingsRepository.saveCourseLineEnabled(enabled)
        updateSystemSettings(systemSettings.copy(courseLineEnabled = enabled))
    }
    
    fun updateExtensionLength(length: Float) {
        chartSettingsRepository.saveExtensionLength(length)
        updateSystemSettings(systemSettings.copy(extensionLength = length))
    }
    
    fun updateGridLineEnabled(enabled: Boolean) {
        chartSettingsRepository.saveGridLineEnabled(enabled)
        updateSystemSettings(systemSettings.copy(gridLineEnabled = enabled))
    }
    
    fun updateDestinationVisible(visible: Boolean) {
        chartSettingsRepository.saveDestinationVisible(visible)
        updateSystemSettings(systemSettings.copy(destinationVisible = visible))
    }
    
    fun updateRouteVisible(visible: Boolean) {
        chartSettingsRepository.saveRouteVisible(visible)
        updateSystemSettings(systemSettings.copy(routeVisible = visible))
    }
    
    fun updateTrackVisible(visible: Boolean) {
        chartSettingsRepository.saveTrackVisible(visible)
        updateSystemSettings(systemSettings.copy(trackVisible = visible))
    }
    
    fun updateMapHidden(hidden: Boolean) {
        chartSettingsRepository.saveMapHidden(hidden)
        updateSystemSettings(systemSettings.copy(mapHidden = hidden))
    }
    
    // ========== 경보 설정 ==========
    
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
    
    // ========== 단위 설정 ==========
    
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
    
    // ========== 무선 설정 ==========
    
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
    
    // ========== 네트워크 설정 ==========
    
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
    
    // ========== 선박 설정 ==========
    // MMSI, recordLength: SystemSetting에서 관리 (읽기 전용)
    // aisCourseExtension, vesselTrackingSettings: ChartPlotter 로컬 저장
    
    fun updateMmsi(mmsi: String) {
        val newSettings = systemSettings.copy(mmsi = mmsi)
        updateSystemSettings(newSettings)
    }
    
    fun updateAisCourseExtension(extension: Float) {
        chartSettingsRepository.saveAisCourseExtension(extension)
        updateSystemSettings(systemSettings.copy(aisCourseExtension = extension))
    }
    
    fun updateVesselTrackingSetting(key: String, enabled: Boolean) {
        val newVesselTrackingSettings = systemSettings.vesselTrackingSettings.toMutableMap()
        newVesselTrackingSettings[key] = enabled
        chartSettingsRepository.saveVesselTrackingSettings(newVesselTrackingSettings)
        updateSystemSettings(systemSettings.copy(vesselTrackingSettings = newVesselTrackingSettings))
    }
    
    fun updateRecordLength(length: Int) {
        val newSettings = systemSettings.copy(recordLength = length)
        updateSystemSettings(newSettings)
    }
    
    /**
     * Factory for SettingsViewModel
     */
    companion object {
        fun provideFactory(
            systemSettingsReader: SystemSettingsReader,
            chartSettingsRepository: ChartSettingsRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(systemSettingsReader, chartSettingsRepository) as T
                }
            }
        }
    }
}
