package com.marineplay.chartplotter.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * 시스템 설정 관리 클래스
 */
class SystemSettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("system_settings", Context.MODE_PRIVATE)
    
    private val KEY_LANGUAGE = "language"
    private val KEY_VESSEL_LENGTH = "vessel_length"
    private val KEY_VESSEL_WIDTH = "vessel_width"
    private val KEY_FONT_SIZE = "font_size"
    private val KEY_BUTTON_VOLUME = "button_volume"
    private val KEY_TIME_FORMAT = "time_format"
    private val KEY_DATE_FORMAT = "date_format"
    private val KEY_GEODETIC_SYSTEM = "geodetic_system"
    private val KEY_COORDINATE_FORMAT = "coordinate_format"
    private val KEY_DECLINATION_MODE = "declination_mode"
    private val KEY_DECLINATION_VALUE = "declination_value"
    private val KEY_PING_SYNC = "ping_sync"
    private val KEY_ADVANCED_FEATURES = "advanced_features"
    private val KEY_MOBILE_CONNECTED = "mobile_connected"
    private val KEY_SOFTWARE_VERSION = "software_version"
    private val KEY_ARRIVAL_RADIUS = "arrival_radius"
    private val KEY_XTE_LIMIT = "xte_limit"
    private val KEY_XTE_ALERT_ENABLED = "xte_alert_enabled"
    private val KEY_BOAT_3D_ENABLED = "boat_3d_enabled"
    private val KEY_DISTANCE_CIRCLE_RADIUS = "distance_circle_radius"
    private val KEY_HEADING_LINE_ENABLED = "heading_line_enabled"
    private val KEY_COURSE_LINE_ENABLED = "course_line_enabled"
    private val KEY_EXTENSION_LENGTH = "extension_length"
    private val KEY_GRID_LINE_ENABLED = "grid_line_enabled"
    private val KEY_DESTINATION_VISIBLE = "destination_visible"
    private val KEY_ROUTE_VISIBLE = "route_visible"
    private val KEY_TRACK_VISIBLE = "track_visible"
    private val KEY_MAP_HIDDEN = "map_hidden"
    private val KEY_ALERT_ENABLED = "alert_enabled"
    private val KEY_ALERT_SETTINGS = "alert_settings"
    private val KEY_DISTANCE_UNIT = "distance_unit"
    private val KEY_SMALL_DISTANCE_UNIT = "small_distance_unit"
    private val KEY_SPEED_UNIT = "speed_unit"
    private val KEY_WIND_SPEED_UNIT = "wind_speed_unit"
    private val KEY_DEPTH_UNIT = "depth_unit"
    private val KEY_ALTITUDE_UNIT = "altitude_unit"
    private val KEY_ALTITUDE_DATUM = "altitude_datum"
    private val KEY_HEADING_UNIT = "heading_unit"
    private val KEY_TEMPERATURE_UNIT = "temperature_unit"
    private val KEY_CAPACITY_UNIT = "capacity_unit"
    private val KEY_FUEL_EFFICIENCY_UNIT = "fuel_efficiency_unit"
    private val KEY_PRESSURE_UNIT = "pressure_unit"
    private val KEY_ATMOSPHERIC_PRESSURE_UNIT = "atmospheric_pressure_unit"
    private val KEY_BLUETOOTH_ENABLED = "bluetooth_enabled"
    private val KEY_BLUETOOTH_PAIRED_DEVICES = "bluetooth_paired_devices"
    private val KEY_WIFI_ENABLED = "wifi_enabled"
    private val KEY_WIFI_CONNECTED_NETWORK = "wifi_connected_network"
    private val KEY_NMEA2000_ENABLED = "nmea2000_enabled"
    private val KEY_NMEA2000_SETTINGS = "nmea2000_settings"
    private val KEY_NMEA0183_ENABLED = "nmea0183_enabled"
    private val KEY_NMEA0183_SETTINGS = "nmea0183_settings"
    private val KEY_MMSI = "mmsi"
    private val KEY_AIS_COURSE_EXTENSION = "ais_course_extension"
    private val KEY_VESSEL_TRACKING_SETTINGS = "vessel_tracking_settings"
    private val KEY_RECORD_LENGTH = "record_length"
    
    fun loadSettings(): SystemSettings {
        val advancedFeaturesJson = prefs.getString(KEY_ADVANCED_FEATURES, "{}") ?: "{}"
        val advancedFeatures = mutableMapOf<String, Boolean>()
        try {
            val json = JSONObject(advancedFeaturesJson)
            json.keys().forEach { key ->
                advancedFeatures[key] = json.getBoolean(key)
            }
        } catch (e: Exception) {
            // 기본값 사용
        }
        
        val alertSettingsJson = prefs.getString(KEY_ALERT_SETTINGS, "{}") ?: "{}"
        val alertSettings = mutableMapOf<String, Boolean>()
        try {
            val json = JSONObject(alertSettingsJson)
            json.keys().forEach { key ->
                alertSettings[key] = json.getBoolean(key)
            }
        } catch (e: Exception) {
            // 기본값 사용
        }
        
        // 블루투스 페어링 장치 목록
        val bluetoothDevicesJson = prefs.getString(KEY_BLUETOOTH_PAIRED_DEVICES, "[]") ?: "[]"
        val bluetoothDevices = mutableListOf<String>()
        try {
            val jsonArray = org.json.JSONArray(bluetoothDevicesJson)
            for (i in 0 until jsonArray.length()) {
                bluetoothDevices.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            // 기본값 사용
        }
        
        // NMEA 2000 설정
        val nmea2000SettingsJson = prefs.getString(KEY_NMEA2000_SETTINGS, "{}") ?: "{}"
        val nmea2000Settings = mutableMapOf<String, String>()
        try {
            val json = JSONObject(nmea2000SettingsJson)
            json.keys().forEach { key ->
                nmea2000Settings[key] = json.getString(key)
            }
        } catch (e: Exception) {
            // 기본값 사용
        }
        
        // NMEA 0183 설정
        val nmea0183SettingsJson = prefs.getString(KEY_NMEA0183_SETTINGS, "{}") ?: "{}"
        val nmea0183Settings = mutableMapOf<String, String>()
        try {
            val json = JSONObject(nmea0183SettingsJson)
            json.keys().forEach { key ->
                nmea0183Settings[key] = json.getString(key)
            }
        } catch (e: Exception) {
            // 기본값 사용
        }
        
        // 선박 및 추적 물표 설정
        val vesselTrackingSettingsJson = prefs.getString(KEY_VESSEL_TRACKING_SETTINGS, "{}") ?: "{}"
        val vesselTrackingSettings = mutableMapOf<String, Boolean>()
        try {
            val json = JSONObject(vesselTrackingSettingsJson)
            json.keys().forEach { key ->
                vesselTrackingSettings[key] = json.getBoolean(key)
            }
        } catch (e: Exception) {
            // 기본값 사용
        }
        
        return SystemSettings(
            language = prefs.getString(KEY_LANGUAGE, "한국어") ?: "한국어",
            vesselLength = prefs.getFloat(KEY_VESSEL_LENGTH, 10.0f),
            vesselWidth = prefs.getFloat(KEY_VESSEL_WIDTH, 3.0f),
            fontSize = prefs.getFloat(KEY_FONT_SIZE, 14f),
            buttonVolume = prefs.getInt(KEY_BUTTON_VOLUME, 50),
            timeFormat = prefs.getString(KEY_TIME_FORMAT, "24시간") ?: "24시간",
            dateFormat = prefs.getString(KEY_DATE_FORMAT, "YYYY-MM-DD") ?: "YYYY-MM-DD",
            geodeticSystem = prefs.getString(KEY_GEODETIC_SYSTEM, "WGS84") ?: "WGS84",
            coordinateFormat = prefs.getString(KEY_COORDINATE_FORMAT, "도") ?: "도",
            declinationMode = prefs.getString(KEY_DECLINATION_MODE, "자동") ?: "자동",
            declinationValue = prefs.getFloat(KEY_DECLINATION_VALUE, 0f),
            pingSync = prefs.getBoolean(KEY_PING_SYNC, true),
            advancedFeatures = advancedFeatures,
            mobileConnected = prefs.getBoolean(KEY_MOBILE_CONNECTED, false),
            softwareVersion = prefs.getString(KEY_SOFTWARE_VERSION, "1.0.0") ?: "1.0.0",
            arrivalRadius = prefs.getFloat(KEY_ARRIVAL_RADIUS, 10.0f),
            xteLimit = prefs.getFloat(KEY_XTE_LIMIT, 50.0f),
            xteAlertEnabled = prefs.getBoolean(KEY_XTE_ALERT_ENABLED, true),
            boat3DEnabled = prefs.getBoolean(KEY_BOAT_3D_ENABLED, false),
            distanceCircleRadius = prefs.getFloat(KEY_DISTANCE_CIRCLE_RADIUS, 100.0f),
            headingLineEnabled = prefs.getBoolean(KEY_HEADING_LINE_ENABLED, true),
            courseLineEnabled = prefs.getBoolean(KEY_COURSE_LINE_ENABLED, true),
            extensionLength = prefs.getFloat(KEY_EXTENSION_LENGTH, 100.0f),
            gridLineEnabled = prefs.getBoolean(KEY_GRID_LINE_ENABLED, false),
            destinationVisible = prefs.getBoolean(KEY_DESTINATION_VISIBLE, true),
            routeVisible = prefs.getBoolean(KEY_ROUTE_VISIBLE, true),
            trackVisible = prefs.getBoolean(KEY_TRACK_VISIBLE, true),
            mapHidden = prefs.getBoolean(KEY_MAP_HIDDEN, false),
            alertEnabled = prefs.getBoolean(KEY_ALERT_ENABLED, true),
            alertSettings = alertSettings,
            distanceUnit = prefs.getString(KEY_DISTANCE_UNIT, "nm") ?: "nm",
            smallDistanceUnit = prefs.getString(KEY_SMALL_DISTANCE_UNIT, "m") ?: "m",
            speedUnit = prefs.getString(KEY_SPEED_UNIT, "노트") ?: "노트",
            windSpeedUnit = prefs.getString(KEY_WIND_SPEED_UNIT, "노트") ?: "노트",
            depthUnit = prefs.getString(KEY_DEPTH_UNIT, "m") ?: "m",
            altitudeUnit = prefs.getString(KEY_ALTITUDE_UNIT, "m") ?: "m",
            altitudeDatum = prefs.getString(KEY_ALTITUDE_DATUM, "지오이드") ?: "지오이드",
            headingUnit = prefs.getString(KEY_HEADING_UNIT, "M") ?: "M",
            temperatureUnit = prefs.getString(KEY_TEMPERATURE_UNIT, "C") ?: "C",
            capacityUnit = prefs.getString(KEY_CAPACITY_UNIT, "L") ?: "L",
            fuelEfficiencyUnit = prefs.getString(KEY_FUEL_EFFICIENCY_UNIT, "L/h") ?: "L/h",
            pressureUnit = prefs.getString(KEY_PRESSURE_UNIT, "bar") ?: "bar",
            atmosphericPressureUnit = prefs.getString(KEY_ATMOSPHERIC_PRESSURE_UNIT, "hPa") ?: "hPa",
            bluetoothEnabled = prefs.getBoolean(KEY_BLUETOOTH_ENABLED, false),
            bluetoothPairedDevices = bluetoothDevices,
            wifiEnabled = prefs.getBoolean(KEY_WIFI_ENABLED, false),
            wifiConnectedNetwork = prefs.getString(KEY_WIFI_CONNECTED_NETWORK, null),
            nmea2000Enabled = prefs.getBoolean(KEY_NMEA2000_ENABLED, false),
            nmea2000Settings = nmea2000Settings,
            nmea0183Enabled = prefs.getBoolean(KEY_NMEA0183_ENABLED, false),
            nmea0183Settings = nmea0183Settings,
            mmsi = prefs.getString(KEY_MMSI, "") ?: "",
            aisCourseExtension = prefs.getFloat(KEY_AIS_COURSE_EXTENSION, 100.0f),
            vesselTrackingSettings = vesselTrackingSettings,
            recordLength = prefs.getInt(KEY_RECORD_LENGTH, 60)
        )
    }
    
    fun saveSettings(settings: SystemSettings) {
        prefs.edit().apply {
            putString(KEY_LANGUAGE, settings.language)
            putFloat(KEY_VESSEL_LENGTH, settings.vesselLength)
            putFloat(KEY_VESSEL_WIDTH, settings.vesselWidth)
            putFloat(KEY_FONT_SIZE, settings.fontSize)
            putInt(KEY_BUTTON_VOLUME, settings.buttonVolume)
            putString(KEY_TIME_FORMAT, settings.timeFormat)
            putString(KEY_DATE_FORMAT, settings.dateFormat)
            putString(KEY_GEODETIC_SYSTEM, settings.geodeticSystem)
            putString(KEY_COORDINATE_FORMAT, settings.coordinateFormat)
            putString(KEY_DECLINATION_MODE, settings.declinationMode)
            putFloat(KEY_DECLINATION_VALUE, settings.declinationValue)
            putBoolean(KEY_PING_SYNC, settings.pingSync)
            
            // 고급 기능을 JSON으로 저장
            val json = JSONObject()
            settings.advancedFeatures.forEach { (key, value) ->
                json.put(key, value)
            }
            putString(KEY_ADVANCED_FEATURES, json.toString())
            
            putBoolean(KEY_MOBILE_CONNECTED, settings.mobileConnected)
            putString(KEY_SOFTWARE_VERSION, settings.softwareVersion)
            putFloat(KEY_ARRIVAL_RADIUS, settings.arrivalRadius)
            putFloat(KEY_XTE_LIMIT, settings.xteLimit)
            putBoolean(KEY_XTE_ALERT_ENABLED, settings.xteAlertEnabled)
            putBoolean(KEY_BOAT_3D_ENABLED, settings.boat3DEnabled)
            putFloat(KEY_DISTANCE_CIRCLE_RADIUS, settings.distanceCircleRadius)
            putBoolean(KEY_HEADING_LINE_ENABLED, settings.headingLineEnabled)
            putBoolean(KEY_COURSE_LINE_ENABLED, settings.courseLineEnabled)
            putFloat(KEY_EXTENSION_LENGTH, settings.extensionLength)
            putBoolean(KEY_GRID_LINE_ENABLED, settings.gridLineEnabled)
            putBoolean(KEY_DESTINATION_VISIBLE, settings.destinationVisible)
            putBoolean(KEY_ROUTE_VISIBLE, settings.routeVisible)
            putBoolean(KEY_TRACK_VISIBLE, settings.trackVisible)
            putBoolean(KEY_MAP_HIDDEN, settings.mapHidden)
            putBoolean(KEY_ALERT_ENABLED, settings.alertEnabled)
            
            // 경보 설정을 JSON으로 저장
            val alertJson = JSONObject()
            settings.alertSettings.forEach { (key, value) ->
                alertJson.put(key, value)
            }
            putString(KEY_ALERT_SETTINGS, alertJson.toString())
            
            putString(KEY_DISTANCE_UNIT, settings.distanceUnit)
            putString(KEY_SMALL_DISTANCE_UNIT, settings.smallDistanceUnit)
            putString(KEY_SPEED_UNIT, settings.speedUnit)
            putString(KEY_WIND_SPEED_UNIT, settings.windSpeedUnit)
            putString(KEY_DEPTH_UNIT, settings.depthUnit)
            putString(KEY_ALTITUDE_UNIT, settings.altitudeUnit)
            putString(KEY_ALTITUDE_DATUM, settings.altitudeDatum)
            putString(KEY_HEADING_UNIT, settings.headingUnit)
            putString(KEY_TEMPERATURE_UNIT, settings.temperatureUnit)
            putString(KEY_CAPACITY_UNIT, settings.capacityUnit)
            putString(KEY_FUEL_EFFICIENCY_UNIT, settings.fuelEfficiencyUnit)
            putString(KEY_PRESSURE_UNIT, settings.pressureUnit)
            putString(KEY_ATMOSPHERIC_PRESSURE_UNIT, settings.atmosphericPressureUnit)
            putBoolean(KEY_BLUETOOTH_ENABLED, settings.bluetoothEnabled)
            
            // 블루투스 페어링 장치 목록을 JSON 배열로 저장
            val bluetoothDevicesArray = org.json.JSONArray()
            settings.bluetoothPairedDevices.forEach { device ->
                bluetoothDevicesArray.put(device)
            }
            putString(KEY_BLUETOOTH_PAIRED_DEVICES, bluetoothDevicesArray.toString())
            
            putBoolean(KEY_WIFI_ENABLED, settings.wifiEnabled)
            putString(KEY_WIFI_CONNECTED_NETWORK, settings.wifiConnectedNetwork)
            putBoolean(KEY_NMEA2000_ENABLED, settings.nmea2000Enabled)
            
            // NMEA 2000 설정을 JSON으로 저장
            val nmea2000Json = JSONObject()
            settings.nmea2000Settings.forEach { (key, value) ->
                nmea2000Json.put(key, value)
            }
            putString(KEY_NMEA2000_SETTINGS, nmea2000Json.toString())
            
            putBoolean(KEY_NMEA0183_ENABLED, settings.nmea0183Enabled)
            
            // NMEA 0183 설정을 JSON으로 저장
            val nmea0183Json = JSONObject()
            settings.nmea0183Settings.forEach { (key, value) ->
                nmea0183Json.put(key, value)
            }
            putString(KEY_NMEA0183_SETTINGS, nmea0183Json.toString())
            
            putString(KEY_MMSI, settings.mmsi)
            putFloat(KEY_AIS_COURSE_EXTENSION, settings.aisCourseExtension)
            
            // 선박 및 추적 물표 설정을 JSON으로 저장
            val vesselTrackingJson = JSONObject()
            settings.vesselTrackingSettings.forEach { (key, value) ->
                vesselTrackingJson.put(key, value)
            }
            putString(KEY_VESSEL_TRACKING_SETTINGS, vesselTrackingJson.toString())
            
            putInt(KEY_RECORD_LENGTH, settings.recordLength)
            apply()
        }
    }
    
    fun resetToDefaults() {
        val defaultSettings = SystemSettings()
        saveSettings(defaultSettings)
    }
}

