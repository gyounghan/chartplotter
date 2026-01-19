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
            xteAlertEnabled = prefs.getBoolean(KEY_XTE_ALERT_ENABLED, true)
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
            apply()
        }
    }
    
    fun resetToDefaults() {
        val defaultSettings = SystemSettings()
        saveSettings(defaultSettings)
    }
}

