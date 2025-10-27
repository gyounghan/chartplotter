package com.marineplay.chartplotter.helpers

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import com.marineplay.chartplotter.utils.DistanceCalculator
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class PointHelper(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("chart_plotter_points", Context.MODE_PRIVATE)
    
    data class SavedPoint(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val color: Color,
        val iconType: String = "circle",
        val timestamp: Long = System.currentTimeMillis()
    )
    
    fun loadPointsFromLocal(): List<SavedPoint> {
        val jsonString = prefs.getString("saved_points", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val points = mutableListOf<SavedPoint>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val point = SavedPoint(
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
            Log.e("[PointHelper]", "포인트 로드 실패", e)
            emptyList()
        }
    }
    
    fun savePointsToLocal(points: List<SavedPoint>) {
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
        prefs.edit().putString("saved_points", jsonArray.toString()).apply()
    }
    
    fun addPoint(name: String, latitude: Double, longitude: Double, color: Color, iconType: String = "circle"): List<SavedPoint> {
        val currentPoints = loadPointsFromLocal().toMutableList()
        val newPoint = SavedPoint(name, latitude, longitude, color, iconType)
        currentPoints.add(newPoint)
        savePointsToLocal(currentPoints)
        return currentPoints
    }
    
    fun updatePoint(oldName: String, newName: String, latitude: Double, longitude: Double, color: Color, iconType: String = "circle"): List<SavedPoint> {
        val currentPoints = loadPointsFromLocal().toMutableList()
        val index = currentPoints.indexOfFirst { it.name == oldName }
        if (index != -1) {
            currentPoints[index] = SavedPoint(newName, latitude, longitude, color, iconType)
            savePointsToLocal(currentPoints)
        }
        return currentPoints
    }
    
    fun deletePoint(pointName: String): List<SavedPoint> {
        val currentPoints = loadPointsFromLocal().toMutableList()
        currentPoints.removeAll { it.name == pointName }
        savePointsToLocal(currentPoints)
        return currentPoints
    }
    
    fun generateUniquePointName(): String {
        val existingPoints = loadPointsFromLocal()
        var counter = 1
        var name = "point001"
        
        while (existingPoints.any { it.name == name }) {
            counter++
            name = "point${counter.toString().padStart(3, '0')}"
        }
        
        return name
    }
    
    fun findClosestPoint(clickLatLng: LatLng, map: MapLibreMap, maxScreenDistance: Double = 100.0): SavedPoint? {
        val allPoints = loadPointsFromLocal()
        if (allPoints.isEmpty()) return null
        
        val closestPoint = allPoints.minByOrNull { point ->
            val pointLatLng = LatLng(point.latitude, point.longitude)
            DistanceCalculator.calculateScreenDistance(clickLatLng, pointLatLng, map)
        }
        
        if (closestPoint != null) {
            val pointLatLng = LatLng(closestPoint.latitude, closestPoint.longitude)
            val screenDistance = DistanceCalculator.calculateScreenDistance(clickLatLng, pointLatLng, map)
            
            return if (screenDistance <= maxScreenDistance) {
                closestPoint
            } else null
        }
        
        return null
    }
}
