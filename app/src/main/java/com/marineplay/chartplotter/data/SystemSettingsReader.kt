package com.marineplay.chartplotter.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * SystemSetting 앱의 ContentProvider를 통해 설정을 읽는 클래스
 */
class SystemSettingsReader(private val context: Context) {
    
    private val CONTENT_URI: Uri = Uri.parse("content://com.marineplay.systemsetting.provider/settings")
    
    fun loadSettings(): SystemSettings {
        val cursor: Cursor? = try {
            context.contentResolver.query(CONTENT_URI, null, null, null, null)
        } catch (e: Exception) {
            // SystemSetting 앱이 설치되지 않았거나 접근할 수 없는 경우 기본값 반환
            return SystemSettings()
        }
        
        if (cursor == null) {
            return SystemSettings()
        }
        
        val settingsMap = mutableMapOf<String, Any?>()
        
        try {
            val keyIndex = cursor.getColumnIndex("key")
            val valueIndex = cursor.getColumnIndex("value")
            
            if (keyIndex >= 0 && valueIndex >= 0) {
                while (cursor.moveToNext()) {
                    val key = cursor.getString(keyIndex)
                    val value = cursor.getString(valueIndex)
                    settingsMap[key] = value
                }
            }
        } finally {
            cursor.close()
        }
        
        // Map에서 설정 값 추출
        fun getString(key: String, default: String): String {
            return (settingsMap[key] as? String) ?: default
        }
        
        fun getFloat(key: String, default: Float): Float {
            return (settingsMap[key] as? String)?.toFloatOrNull() ?: default
        }
        
        fun getInt(key: String, default: Int): Int {
            return (settingsMap[key] as? String)?.toIntOrNull() ?: default
        }
        
        fun getBoolean(key: String, default: Boolean): Boolean {
            return (settingsMap[key] as? String)?.toBoolean() ?: default
        }
        
        // JSON 형식의 복잡한 설정들 파싱
        fun parseJsonMap(key: String): Map<String, Boolean> {
            val jsonString = getString(key, "{}")
            val map = mutableMapOf<String, Boolean>()
            try {
                val json = JSONObject(jsonString)
                json.keys().forEach { k ->
                    map[k] = json.getBoolean(k)
                }
            } catch (e: Exception) {
                // 기본값 사용
            }
            return map
        }
        
        fun parseJsonStringMap(key: String): Map<String, String> {
            val jsonString = getString(key, "{}")
            val map = mutableMapOf<String, String>()
            try {
                val json = JSONObject(jsonString)
                json.keys().forEach { k ->
                    map[k] = json.getString(k)
                }
            } catch (e: Exception) {
                // 기본값 사용
            }
            return map
        }
        
        fun parseJsonArray(key: String): List<String> {
            val jsonString = getString(key, "[]")
            val list = mutableListOf<String>()
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i))
                }
            } catch (e: Exception) {
                // 기본값 사용
            }
            return list
        }
        
        return SystemSettings(
            language = getString("language", "한국어"),
            vesselLength = getFloat("vessel_length", 10.0f),
            vesselWidth = getFloat("vessel_width", 3.0f),
            fontSize = getFloat("font_size", 14f),
            buttonVolume = getInt("button_volume", 50),
            timeFormat = getString("time_format", "24시간"),
            dateFormat = getString("date_format", "YYYY-MM-DD"),
            geodeticSystem = getString("geodetic_system", "WGS84"),
            coordinateFormat = getString("coordinate_format", "도"),
            declinationMode = getString("declination_mode", "자동"),
            declinationValue = getFloat("declination_value", 0f),
            pingSync = getBoolean("ping_sync", true),
            advancedFeatures = parseJsonMap("advanced_features"),
            mobileConnected = getBoolean("mobile_connected", false),
            softwareVersion = getString("software_version", "1.0.0"),
            arrivalRadius = getFloat("arrival_radius", 10.0f),
            xteLimit = getFloat("xte_limit", 50.0f),
            xteAlertEnabled = getBoolean("xte_alert_enabled", true),
            boat3DEnabled = getBoolean("boat_3d_enabled", false),
            distanceCircleRadius = getFloat("distance_circle_radius", 100.0f),
            headingLineEnabled = getBoolean("heading_line_enabled", true),
            courseLineEnabled = getBoolean("course_line_enabled", true),
            extensionLength = getFloat("extension_length", 100.0f),
            gridLineEnabled = getBoolean("grid_line_enabled", false),
            destinationVisible = getBoolean("destination_visible", true),
            routeVisible = getBoolean("route_visible", true),
            trackVisible = getBoolean("track_visible", true),
            mapHidden = getBoolean("map_hidden", false),
            alertEnabled = getBoolean("alert_enabled", true),
            alertSettings = parseJsonMap("alert_settings"),
            distanceUnit = getString("distance_unit", "nm"),
            smallDistanceUnit = getString("small_distance_unit", "m"),
            speedUnit = getString("speed_unit", "노트"),
            windSpeedUnit = getString("wind_speed_unit", "노트"),
            depthUnit = getString("depth_unit", "m"),
            altitudeUnit = getString("altitude_unit", "m"),
            altitudeDatum = getString("altitude_datum", "지오이드"),
            headingUnit = getString("heading_unit", "M"),
            temperatureUnit = getString("temperature_unit", "C"),
            capacityUnit = getString("capacity_unit", "L"),
            fuelEfficiencyUnit = getString("fuel_efficiency_unit", "L/h"),
            pressureUnit = getString("pressure_unit", "bar"),
            atmosphericPressureUnit = getString("atmospheric_pressure_unit", "hPa"),
            bluetoothEnabled = getBoolean("bluetooth_enabled", false),
            bluetoothPairedDevices = parseJsonArray("bluetooth_paired_devices"),
            wifiEnabled = getBoolean("wifi_enabled", false),
            wifiConnectedNetwork = getString("wifi_connected_network", "").takeIf { it.isNotEmpty() },
            nmea2000Enabled = getBoolean("nmea2000_enabled", false),
            nmea2000Settings = parseJsonStringMap("nmea2000_settings"),
            nmea0183Enabled = getBoolean("nmea0183_enabled", false),
            nmea0183Settings = parseJsonStringMap("nmea0183_settings"),
            mmsi = getString("mmsi", ""),
            aisCourseExtension = getFloat("ais_course_extension", 100.0f),
            vesselTrackingSettings = parseJsonMap("vessel_tracking_settings"),
            recordLength = getInt("record_length", 60)
        )
    }
}

