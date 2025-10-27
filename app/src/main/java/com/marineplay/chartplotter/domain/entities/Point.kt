package com.marineplay.chartplotter.domain.entities

import android.graphics.Color
import org.maplibre.android.geometry.LatLng

data class Point(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val color: Color,
    val iconType: String = "circle",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}