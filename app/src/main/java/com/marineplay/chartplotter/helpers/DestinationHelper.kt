package com.marineplay.chartplotter.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.marineplay.chartplotter.utils.DistanceCalculator
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class DestinationHelper(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("chart_plotter_prefs", Context.MODE_PRIVATE)
    
    data class Destination(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    fun loadDestinationsFromLocal(): List<Destination> {
        val jsonString = prefs.getString("saved_destinations", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val destinations = mutableListOf<Destination>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val destination = Destination(
                    name = jsonObject.getString("name"),
                    latitude = jsonObject.getDouble("latitude"),
                    longitude = jsonObject.getDouble("longitude"),
                    timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
                )
                destinations.add(destination)
            }
            destinations
        } catch (e: Exception) {
            Log.e("[DestinationHelper]", "목적지 로드 실패", e)
            emptyList()
        }
    }
    
    fun saveDestinationsToLocal(destinations: List<Destination>) {
        val jsonArray = JSONArray()
        destinations.forEach { destination ->
            val jsonObject = JSONObject().apply {
                put("name", destination.name)
                put("latitude", destination.latitude)
                put("longitude", destination.longitude)
                put("timestamp", destination.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("saved_destinations", jsonArray.toString()).apply()
    }
    
    fun addDestination(name: String, latitude: Double, longitude: Double): List<Destination> {
        val currentDestinations = loadDestinationsFromLocal().toMutableList()
        val newDestination = Destination(name, latitude, longitude)
        currentDestinations.add(newDestination)
        saveDestinationsToLocal(currentDestinations)
        return currentDestinations
    }
    
    fun updateDestination(oldName: String, newName: String, latitude: Double, longitude: Double): List<Destination> {
        val currentDestinations = loadDestinationsFromLocal().toMutableList()
        val index = currentDestinations.indexOfFirst { it.name == oldName }
        if (index != -1) {
            currentDestinations[index] = Destination(newName, latitude, longitude)
            saveDestinationsToLocal(currentDestinations)
        }
        return currentDestinations
    }
    
    fun deleteDestination(destinationName: String): List<Destination> {
        val currentDestinations = loadDestinationsFromLocal().toMutableList()
        currentDestinations.removeAll { it.name == destinationName }
        saveDestinationsToLocal(currentDestinations)
        return currentDestinations
    }
    
    fun generateUniqueDestinationName(): String {
        val existingDestinations = loadDestinationsFromLocal()
        var counter = 1
        var name = "target001"
        
        while (existingDestinations.any { it.name == name }) {
            counter++
            name = "target${counter.toString().padStart(3, '0')}"
        }
        
        return name
    }
    
    fun findClosestDestination(clickLatLng: LatLng, map: MapLibreMap, maxScreenDistance: Double = 100.0): Destination? {
        val allDestinations = loadDestinationsFromLocal()
        if (allDestinations.isEmpty()) return null
        
        val closestDestination = allDestinations.minByOrNull { destination ->
            val destinationLatLng = LatLng(destination.latitude, destination.longitude)
            DistanceCalculator.calculateScreenDistance(clickLatLng, destinationLatLng, map)
        }
        
        if (closestDestination != null) {
            val destinationLatLng = LatLng(closestDestination.latitude, closestDestination.longitude)
            val screenDistance = DistanceCalculator.calculateScreenDistance(clickLatLng, destinationLatLng, map)
            
            return if (screenDistance <= maxScreenDistance) {
                closestDestination
            } else null
        }
        
        return null
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
