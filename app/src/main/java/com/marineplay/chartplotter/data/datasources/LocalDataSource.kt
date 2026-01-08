package com.marineplay.chartplotter.data.datasources

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.marineplay.chartplotter.domain.entities.Destination
import com.marineplay.chartplotter.domain.entities.Point
import com.marineplay.chartplotter.helpers.PointHelper
import org.json.JSONArray
import org.json.JSONObject

class LocalDataSource(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("chart_plotter_prefs", Context.MODE_PRIVATE)
    private val pointPrefs: SharedPreferences = context.getSharedPreferences("chart_plotter_points", Context.MODE_PRIVATE)
    
    // Points
    fun savePoints(points: List<Point>) {
        val jsonArray = JSONArray()
        points.forEach { point ->
            val jsonObject = JSONObject().apply {
                put("id", point.id)
                put("name", point.name)
                put("latitude", point.latitude)
                put("longitude", point.longitude)
                put("color", point.color.toArgb())
                put("iconType", point.iconType)
                put("timestamp", point.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("saved_points", jsonArray.toString()).apply()
    }
    
    fun loadPoints(): List<Point> {
        val jsonString = prefs.getString("saved_points", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val points = mutableListOf<Point>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val point = Point(
                    id = jsonObject.getString("id"),
                    name = jsonObject.getString("name"),
                    latitude = jsonObject.getDouble("latitude"),
                    longitude = jsonObject.getDouble("longitude"),
                    color = Color.valueOf(jsonObject.getInt("color")),
                    iconType = jsonObject.optString("iconType", "circle"),
                    timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
                )
                points.add(point)
            }
            points
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Destinations
    fun saveDestinations(destinations: List<Destination>) {
        val jsonArray = JSONArray()
        destinations.forEach { destination ->
            val jsonObject = JSONObject().apply {
                put("id", destination.id)
                put("name", destination.name)
                put("latitude", destination.latitude)
                put("longitude", destination.longitude)
                put("timestamp", destination.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("saved_destinations", jsonArray.toString()).apply()
    }
    
    fun loadDestinations(): List<Destination> {
        val jsonString = prefs.getString("saved_destinations", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val destinations = mutableListOf<Destination>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val destination = Destination(
                    id = jsonObject.getString("id"),
                    name = jsonObject.getString("name"),
                    latitude = jsonObject.getDouble("latitude"),
                    longitude = jsonObject.getDouble("longitude"),
                    timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
                )
                destinations.add(destination)
            }
            destinations
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Settings
    fun saveMapDisplayMode(mode: String) {
        prefs.edit().putString("map_display_mode", mode).apply()
    }
    
    fun loadMapDisplayMode(): String {
        return prefs.getString("map_display_mode", "노스업") ?: "노스업"
    }
    
    fun saveCourseDestination(latitude: Double, longitude: Double) {
        prefs.edit()
            .putFloat("course_destination_lat", latitude.toFloat())
            .putFloat("course_destination_lng", longitude.toFloat())
            .apply()
    }
    
    fun loadCourseDestination(): Pair<Double, Double>? {
        val lat = prefs.getFloat("course_destination_lat", 0f)
        val lng = prefs.getFloat("course_destination_lng", 0f)
        return if (lat != 0f && lng != 0f) {
            Pair(lat.toDouble(), lng.toDouble())
        } else null
    }
    
    // PointHelper.SavedPoint 지원 (기존 PointHelper와 호환)
    fun saveSavedPoints(points: List<PointHelper.SavedPoint>) {
        val jsonArray = JSONArray()
        points.forEach { point ->
            val jsonObject = JSONObject().apply {
                put("name", point.name)
                put("latitude", point.latitude)
                put("longitude", point.longitude)
                put("color", point.color.toArgb())
                put("iconType", point.iconType)
                put("timestamp", point.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        pointPrefs.edit().putString("saved_points", jsonArray.toString()).apply()
    }
    
    fun loadSavedPoints(): List<PointHelper.SavedPoint> {
        val jsonString = pointPrefs.getString("saved_points", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val points = mutableListOf<PointHelper.SavedPoint>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val point = PointHelper.SavedPoint(
                    name = jsonObject.getString("name"),
                    latitude = jsonObject.getDouble("latitude"),
                    longitude = jsonObject.getDouble("longitude"),
                    color = Color.valueOf(jsonObject.getInt("color")),
                    iconType = jsonObject.optString("iconType", "circle"),
                    timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
                )
                points.add(point)
            }
            points
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Track 관련 데이터 저장/로드
    private val trackPrefs: SharedPreferences = context.getSharedPreferences("track_prefs", Context.MODE_PRIVATE)
    
    fun saveTracks(tracks: List<com.marineplay.chartplotter.Track>) {
        val jsonArray = JSONArray()
        tracks.forEach { track ->
            val trackJson = JSONObject().apply {
                put("id", track.id)
                put("name", track.name)
                put("colorValue", track.color.value.toLong())
                put("isVisible", track.isVisible)
                
                val recordsArray = JSONArray()
                track.records.forEach { record ->
                    val recordJson = JSONObject().apply {
                        put("id", record.id)
                        put("startTime", record.startTime)
                        put("endTime", record.endTime)
                        put("title", record.title)
                        
                        val pointsArray = JSONArray()
                        record.points.forEach { point ->
                            val pointJson = JSONObject().apply {
                                put("latitude", point.latitude)
                                put("longitude", point.longitude)
                                put("timestamp", point.timestamp)
                            }
                            pointsArray.put(pointJson)
                        }
                        put("points", pointsArray)
                    }
                    recordsArray.put(recordJson)
                }
                put("records", recordsArray)
            }
            jsonArray.put(trackJson)
        }
        trackPrefs.edit().putString("tracks", jsonArray.toString()).apply()
    }
    
    fun loadTracks(): List<com.marineplay.chartplotter.Track> {
        val jsonString = trackPrefs.getString("tracks", null) ?: return emptyList()
        val tracks = mutableListOf<com.marineplay.chartplotter.Track>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val trackJson = jsonArray.getJSONObject(i)
                val track = com.marineplay.chartplotter.Track(
                    id = trackJson.getString("id"),
                    name = trackJson.getString("name"),
                    color = androidx.compose.ui.graphics.Color(trackJson.getLong("colorValue")),
                    isVisible = trackJson.optBoolean("isVisible", true)
                )
                
                val recordsArray = trackJson.getJSONArray("records")
                for (j in 0 until recordsArray.length()) {
                    val recordJson = recordsArray.getJSONObject(j)
                    val pointsList = mutableListOf<com.marineplay.chartplotter.TrackPoint>()
                    val pointsArray = recordJson.getJSONArray("points")
                    for (k in 0 until pointsArray.length()) {
                        val pointJson = pointsArray.getJSONObject(k)
                        pointsList.add(
                            com.marineplay.chartplotter.TrackPoint(
                                latitude = pointJson.getDouble("latitude"),
                                longitude = pointJson.getDouble("longitude"),
                                timestamp = pointJson.getLong("timestamp")
                            )
                        )
                    }
                    
                    track.records.add(
                        com.marineplay.chartplotter.TrackRecord(
                            id = recordJson.getString("id"),
                            trackId = track.id,
                            startTime = recordJson.getLong("startTime"),
                            endTime = recordJson.getLong("endTime"),
                            points = pointsList,
                            title = recordJson.getString("title")
                        )
                    )
                }
                
                tracks.add(track)
            }
        } catch (e: Exception) {
            // 로드 실패 시 빈 리스트 반환
        }
        
        return tracks
    }
    
    fun saveTrackSettings(settings: com.marineplay.chartplotter.TrackSettings) {
        val json = JSONObject().apply {
            put("intervalType", settings.intervalType)
            put("timeInterval", settings.timeInterval)
            put("distanceInterval", settings.distanceInterval)
        }
        trackPrefs.edit().putString("track_settings", json.toString()).apply()
    }
    
    fun loadTrackSettings(): com.marineplay.chartplotter.TrackSettings {
        val jsonString = trackPrefs.getString("track_settings", null) ?: return com.marineplay.chartplotter.TrackSettings("time", 5000L, 10.0)
        
        return try {
            val json = JSONObject(jsonString)
            com.marineplay.chartplotter.TrackSettings(
                intervalType = json.getString("intervalType"),
                timeInterval = json.getLong("timeInterval"),
                distanceInterval = json.getDouble("distanceInterval")
            )
        } catch (e: Exception) {
            com.marineplay.chartplotter.TrackSettings("time", 5000L, 10.0)
        }
    }
}