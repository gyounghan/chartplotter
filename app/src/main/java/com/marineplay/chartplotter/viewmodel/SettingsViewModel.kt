package com.marineplay.chartplotter.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marineplay.chartplotter.data.SystemSettings
import com.marineplay.chartplotter.data.SystemSettingsReader

/**
 * 시스템 설정 전용 ViewModel
 * SystemSetting 앱의 ContentProvider를 통해 설정을 읽고 로컬 상태를 관리합니다.
 */
class SettingsViewModel(
    private val systemSettingsReader: SystemSettingsReader
) : ViewModel() {
    
    // 시스템 설정 상태
    var systemSettings by mutableStateOf(SystemSettings())
        private set
    
    init {
        loadSystemSettings()
    }
    
    fun loadSystemSettings() {
        systemSettings = systemSettingsReader.loadSettings()
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
    
    // ========== 지도 설정 ==========
    
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
    
    /**
     * Factory for SettingsViewModel
     */
    companion object {
        fun provideFactory(
            systemSettingsReader: SystemSettingsReader
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(systemSettingsReader) as T
                }
            }
        }
    }
}
