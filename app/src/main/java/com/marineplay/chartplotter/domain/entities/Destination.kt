package com.marineplay.chartplotter.domain.entities

import org.maplibre.android.geometry.LatLng

data class Destination(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}