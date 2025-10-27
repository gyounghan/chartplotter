package com.marineplay.chartplotter.data.datasources

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.marineplay.chartplotter.domain.entities.Destination
import com.marineplay.chartplotter.domain.entities.Point
import org.json.JSONArray
import org.json.JSONObject

class LocalDataSource(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("chart_plotter_prefs", Context.MODE_PRIVATE)
    
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
}