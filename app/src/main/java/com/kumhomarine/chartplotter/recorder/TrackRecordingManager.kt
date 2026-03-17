package com.kumhomarine.chartplotter.recorder

import android.content.Context
import android.util.Log
import com.kumhomarine.chartplotter.ChartPlotterApplication
import com.kumhomarine.chartplotter.domain.repositories.TrackRepository
import com.kumhomarine.chartplotter.domain.usecases.AddTrackPointUseCase
import com.kumhomarine.chartplotter.domain.usecases.CalculateDistanceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.maplibre.android.geometry.LatLng

/**
 * 항적 백그라운드 기록 관리
 * TrackRecordingService에서 위치 업데이트를 받아 기록 중인 항적에 포인트를 추가합니다.
 * (기록을 켠 항적만 저장)
 */
class TrackRecordingManager(context: Context) {

    private val app = context.applicationContext as ChartPlotterApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val trackRepository: TrackRepository get() = app.trackRepository
    private val addTrackPointUseCase = AddTrackPointUseCase(CalculateDistanceUseCase())

    /**
     * 위치 업데이트 시 호출 (백그라운드 서비스에서)
     * 기록 중인 항적에 대해 간격 조건을 만족하면 포인트 추가
     */
    fun onLocationUpdate(
        latitude: Double,
        longitude: Double,
        isTimerTriggered: Boolean = false
    ) {
        scope.launch {
            try {
                val recordingTracks = trackRepository.getRecordingTracks()
                if (recordingTracks.isEmpty()) return@launch

                val currentTime = System.currentTimeMillis()

                recordingTracks.forEach { track ->
                    val lastPoints = trackRepository.getRecentTrackPoints(track.id, 1)
                    val lastPoint = lastPoints.firstOrNull()
                    val lastTrackPointTime = lastPoint?.timestamp ?: 0L
                    val lastTrackPointLocation = lastPoint?.let {
                        LatLng(it.latitude, it.longitude)
                    }

                    val newPoint = addTrackPointUseCase.execute(
                        latitude = latitude,
                        longitude = longitude,
                        currentTime = currentTime,
                        lastTrackPointTime = lastTrackPointTime,
                        lastTrackPointLocation = lastTrackPointLocation,
                        intervalType = track.intervalType,
                        timeInterval = track.timeInterval,
                        distanceInterval = track.distanceInterval,
                        isTimerTriggered = isTimerTriggered
                    )

                    if (newPoint != null) {
                        trackRepository.addTrackPoint(track.id, newPoint)
                        Log.d(TAG, "항적 포인트 추가(백그라운드): ${track.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "백그라운드 항적 기록 실패: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "TrackRecordingManager"
    }
}
