package com.marineplay.chartplotter.ui.modules.camera.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 카메라 모듈 관련 모델 클래스들
 */

enum class CameraTab(val label: String, val icon: ImageVector) {
    LIVE("실시간", Icons.Default.Tv),
    RECORDINGS("기록", Icons.Default.VideoLibrary),
    CAMERAS("카메라", Icons.Default.CameraAlt),
    SETTINGS("설정", Icons.Default.Settings)
}

enum class CameraPosition(val label: String) {
    BOW("전면"),
    STERN("후면"),
    PORT("좌현"),
    STARBOARD("우현")
}

enum class ViewMode {
    SINGLE, DUAL, QUAD
}

enum class CameraStatus {
    CONNECTED, DISCONNECTED
}

enum class EventType(val label: String, val color: androidx.compose.ui.graphics.Color) {
    AUTO("자동", androidx.compose.ui.graphics.Color(0xFF3B82F6)),
    MANUAL("수동", androidx.compose.ui.graphics.Color(0xFF10B981)),
    IMPACT("충격", androidx.compose.ui.graphics.Color(0xFFEF4444))
}

data class CameraInfo(
    val position: CameraPosition,
    val label: String,
    val status: CameraStatus,
    val nightMode: Boolean = false,
    val resolution: String = "1920x1080",
    val fps: Int = 30,
    val lastConnected: String? = null
)

data class Recording(
    val id: String,
    val date: String,
    val time: String,
    val camera: CameraPosition,
    val cameraLabel: String,
    val eventType: EventType,
    val duration: Int, // seconds
    val locked: Boolean
)

