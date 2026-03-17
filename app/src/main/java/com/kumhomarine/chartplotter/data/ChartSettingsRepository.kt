package com.kumhomarine.chartplotter.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * ChartPlotter 앱 전용 지도/선박(차트) 설정 저장소
 * SystemSetting과 분리하여 ChartPlotter에서 직접 저장·로드합니다.
 */
class ChartSettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
    private val KEY_AIS_COURSE_EXTENSION = "ais_course_extension"
    private val KEY_VESSEL_TRACKING_SETTINGS = "vessel_tracking_settings"

    data class ChartSettings(
        val boat3DEnabled: Boolean = false,
        val distanceCircleRadius: Float = 100.0f,
        val headingLineEnabled: Boolean = true,
        val courseLineEnabled: Boolean = true,
        val extensionLength: Float = 100.0f,
        val gridLineEnabled: Boolean = false,
        val destinationVisible: Boolean = true,
        val routeVisible: Boolean = true,
        val trackVisible: Boolean = true,
        val mapHidden: Boolean = false,
        val aisCourseExtension: Float = 100.0f,
        val vesselTrackingSettings: Map<String, Boolean> = emptyMap()
    )

    fun loadChartSettings(): ChartSettings {
        val vesselTrackingJson = prefs.getString(KEY_VESSEL_TRACKING_SETTINGS, "{}") ?: "{}"
        val vesselTrackingSettings = mutableMapOf<String, Boolean>()
        try {
            val json = JSONObject(vesselTrackingJson)
            json.keys().forEach { key ->
                vesselTrackingSettings[key] = json.getBoolean(key)
            }
        } catch (e: Exception) { /* 기본값 사용 */ }

        return ChartSettings(
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
            aisCourseExtension = prefs.getFloat(KEY_AIS_COURSE_EXTENSION, 100.0f),
            vesselTrackingSettings = vesselTrackingSettings
        )
    }

    fun saveBoat3DEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BOAT_3D_ENABLED, enabled).apply()
    }

    fun saveDistanceCircleRadius(radius: Float) {
        prefs.edit().putFloat(KEY_DISTANCE_CIRCLE_RADIUS, radius).apply()
    }

    fun saveHeadingLineEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HEADING_LINE_ENABLED, enabled).apply()
    }

    fun saveCourseLineEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COURSE_LINE_ENABLED, enabled).apply()
    }

    fun saveExtensionLength(length: Float) {
        prefs.edit().putFloat(KEY_EXTENSION_LENGTH, length).apply()
    }

    fun saveGridLineEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GRID_LINE_ENABLED, enabled).apply()
    }

    fun saveDestinationVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_DESTINATION_VISIBLE, visible).apply()
    }

    fun saveRouteVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_ROUTE_VISIBLE, visible).apply()
    }

    fun saveTrackVisible(visible: Boolean) {
        prefs.edit().putBoolean(KEY_TRACK_VISIBLE, visible).apply()
    }

    fun saveMapHidden(hidden: Boolean) {
        prefs.edit().putBoolean(KEY_MAP_HIDDEN, hidden).apply()
    }

    fun saveAisCourseExtension(extension: Float) {
        prefs.edit().putFloat(KEY_AIS_COURSE_EXTENSION, extension).apply()
    }

    fun saveVesselTrackingSettings(settings: Map<String, Boolean>) {
        val json = JSONObject()
        settings.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_VESSEL_TRACKING_SETTINGS, json.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "chart_settings"
    }
}
