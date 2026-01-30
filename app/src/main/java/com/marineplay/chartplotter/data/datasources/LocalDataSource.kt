package com.marineplay.chartplotter.data.datasources

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.marineplay.chartplotter.domain.entities.Destination
import com.marineplay.chartplotter.domain.entities.Point
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.entities.TrackPoint
import com.marineplay.chartplotter.data.models.SavedPoint
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
    
    // SavedPoint 지원 (기존 PointHelper와 호환)
    fun saveSavedPoints(points: List<SavedPoint>) {
        val jsonArray = JSONArray()
        points.forEach { point ->
            val jsonObject = JSONObject().apply {
                put("name", point.name)
                put("latitude", point.latitude)
                put("longitude", point.longitude)
                put("color", point.color) // color는 이미 Int 타입
                put("iconType", point.iconType)
                put("timestamp", point.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        pointPrefs.edit().putString("saved_points", jsonArray.toString()).apply()
    }
    
    fun loadSavedPoints(): List<SavedPoint> {
        val jsonString = pointPrefs.getString("saved_points", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val points = mutableListOf<SavedPoint>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val point = SavedPoint(
                    name = jsonObject.getString("name"),
                    latitude = jsonObject.getDouble("latitude"),
                    longitude = jsonObject.getDouble("longitude"),
                    color = jsonObject.getInt("color"), // Int 타입으로 직접 사용
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
    
    fun saveTracks(tracks: List<Track>) {
        val jsonArray = JSONArray()
        tracks.forEach { track ->
            val trackJson = JSONObject().apply {
                put("id", track.id)
                put("name", track.name)
                put("colorValue", track.color.value.toLong())
                put("isVisible", track.isVisible)
                        
                        val pointsArray = JSONArray()
                track.points.forEach { point ->
                            val pointJson = JSONObject().apply {
                                put("latitude", point.latitude)
                                put("longitude", point.longitude)
                                put("timestamp", point.timestamp)
                            }
                            pointsArray.put(pointJson)
                        }
                        put("points", pointsArray)
            }
            jsonArray.put(trackJson)
        }
        trackPrefs.edit().putString("tracks", jsonArray.toString()).apply()
    }
    
    fun loadTracks(): List<Track> {
        val jsonString = trackPrefs.getString("tracks", null) ?: return emptyList()
        val tracks = mutableListOf<Track>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val trackJson = jsonArray.getJSONObject(i)
                val track = Track(
                    id = trackJson.getString("id"),
                    name = trackJson.getString("name"),
                    color = androidx.compose.ui.graphics.Color(trackJson.getLong("colorValue")),
                    isVisible = trackJson.optBoolean("isVisible", true)
                )
                
                // 하위 호환성: records가 있으면 points로 변환
                if (trackJson.has("records")) {
                val recordsArray = trackJson.getJSONArray("records")
                for (j in 0 until recordsArray.length()) {
                    val recordJson = recordsArray.getJSONObject(j)
                    val pointsArray = recordJson.getJSONArray("points")
                    for (k in 0 until pointsArray.length()) {
                        val pointJson = pointsArray.getJSONObject(k)
                            track.points.add(
                                TrackPoint(
                                    latitude = pointJson.getDouble("latitude"),
                                    longitude = pointJson.getDouble("longitude"),
                                    timestamp = pointJson.getLong("timestamp")
                                )
                            )
                        }
                    }
                } else if (trackJson.has("points")) {
                    // 새로운 형식: points 직접 사용
                    val pointsArray = trackJson.getJSONArray("points")
                    for (j in 0 until pointsArray.length()) {
                        val pointJson = pointsArray.getJSONObject(j)
                        track.points.add(
                            TrackPoint(
                                latitude = pointJson.getDouble("latitude"),
                                longitude = pointJson.getDouble("longitude"),
                                timestamp = pointJson.getLong("timestamp")
                            )
                        )
                    }
                }
                
                tracks.add(track)
            }
        } catch (e: Exception) {
            // 로드 실패 시 빈 리스트 반환
        }
        
        return tracks
    }
    
}