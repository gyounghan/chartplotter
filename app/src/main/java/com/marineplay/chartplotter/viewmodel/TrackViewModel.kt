package com.marineplay.chartplotter.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.entities.TrackPoint
import com.marineplay.chartplotter.domain.repositories.TrackRepository
import com.marineplay.chartplotter.domain.usecases.*
import org.maplibre.android.geometry.LatLng

/**
 * 항적별 기록 상태
 */
data class TrackRecordingState(
    val trackId: String,
    val startTime: Long,
    val points: MutableList<TrackPoint> = mutableListOf(),
    val lastTrackPointTime: Long = 0,
    val lastTrackPointLocation: LatLng? = null
)

/**
 * 항적 관련 UI 상태
 */
data class TrackUiState(
    val isRecordingTrack: Boolean = false,
    val currentRecordingTrack: Track? = null,
    val trackRecordingStartTime: Long = 0,
    val trackPoints: List<TrackPoint> = emptyList(),
    val lastTrackPointTime: Long = 0,
    val lastTrackPointLocation: LatLng? = null,
    val recordingTracks: Map<String, TrackRecordingState> = emptyMap(),
    val selectedTrackForRecords: Track? = null,
    val selectedTrackForSettings: Track? = null,
    val highlightedTrackRecord: Pair<String, String>? = null
)

/**
 * 항적(Track) 전용 ViewModel
 * 항적 기록, 조회, 관리 등 항적 관련 모든 비즈니스 로직을 처리합니다.
 */
class TrackViewModel(
    private val trackRepository: TrackRepository,
    private val startTrackRecordingUseCase: StartTrackRecordingUseCase,
    private val stopTrackRecordingUseCase: StopTrackRecordingUseCase,
    private val addTrackPointUseCase: AddTrackPointUseCase,
    private val calculateDistanceUseCase: CalculateDistanceUseCase
) : ViewModel() {

    // ========== UI 상태 ==========
    var trackUiState by mutableStateOf(TrackUiState())
        private set

    // 항적 정보 캐시 (성능 최적화)
    private val trackCache = mutableMapOf<String, Track>()

    init {
        // 앱 시작 시 자동 기록 시작 (isRecording=true인 항적)
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)

            val recordingTracks = trackRepository.getRecordingTracks()
            if (recordingTracks.isNotEmpty()) {
                val updatedRecordingTracks = mutableMapOf<String, TrackRecordingState>()
                recordingTracks.forEach { track ->
                    val recordingState = TrackRecordingState(
                        trackId = track.id,
                        startTime = System.currentTimeMillis()
                    )
                    updatedRecordingTracks[track.id] = recordingState

                    // 캐시에 저장
                    trackCache[track.id] = track

                    // UseCase 실행
                    startTrackRecordingUseCase.execute(track)
                }

                trackUiState = trackUiState.copy(
                    recordingTracks = updatedRecordingTracks,
                    isRecordingTrack = true,
                    currentRecordingTrack = recordingTracks.firstOrNull(),
                    trackRecordingStartTime = recordingTracks.firstOrNull()?.let {
                        updatedRecordingTracks[it.id]?.startTime ?: 0L
                    } ?: 0L
                )
            }
        }
    }

    // ========== 캐시 ==========

    private suspend fun getTrackFromCache(trackId: String): Track? {
        return trackCache[trackId] ?: run {
            trackRepository.getAllTracks().find { it.id == trackId }?.also {
                trackCache[trackId] = it
            }
        }
    }

    private fun invalidateTrackCache(trackId: String? = null) {
        if (trackId != null) {
            trackCache.remove(trackId)
        } else {
            trackCache.clear()
        }
    }

    // ========== TrackUiState 업데이트 함수들 ==========

    fun updateIsRecordingTrack(recording: Boolean) {
        trackUiState = trackUiState.copy(isRecordingTrack = recording)
    }

    fun updateCurrentRecordingTrack(track: Track?) {
        trackUiState = trackUiState.copy(currentRecordingTrack = track)
    }

    fun updateTrackRecordingStartTime(time: Long) {
        trackUiState = trackUiState.copy(trackRecordingStartTime = time)
    }

    fun updateTrackPoints(points: List<TrackPoint>) {
        trackUiState = trackUiState.copy(trackPoints = points)
    }

    fun addTrackPoint(point: TrackPoint) {
        trackUiState = trackUiState.copy(
            trackPoints = trackUiState.trackPoints + point,
            lastTrackPointTime = point.timestamp,
            lastTrackPointLocation = LatLng(point.latitude, point.longitude)
        )
    }

    fun clearTrackPoints() {
        trackUiState = trackUiState.copy(
            trackPoints = emptyList(),
            lastTrackPointTime = 0,
            lastTrackPointLocation = null
        )
    }

    fun updateSelectedTrackForRecords(track: Track?) {
        trackUiState = trackUiState.copy(selectedTrackForRecords = track)
    }

    fun updateSelectedTrackForSettings(track: Track?) {
        trackUiState = trackUiState.copy(selectedTrackForSettings = track)
    }

    fun updateHighlightedTrackRecord(record: Pair<String, String>?) {
        trackUiState = trackUiState.copy(highlightedTrackRecord = record)
    }

    // ========== 비즈니스 로직 ==========

    /**
     * 항적 기록 시작 (단일 항적만 기록 가능)
     */
    fun startTrackRecording(track: Track) {
        val currentRecordingTracks = trackUiState.recordingTracks
        if (currentRecordingTracks.isNotEmpty()) {
            currentRecordingTracks.keys.forEach { existingTrackId ->
                stopTrackRecording(existingTrackId)
            }
        }

        viewModelScope.launch {
            trackRepository.setTrackRecording(track.id, true)
        }

        val newRecordingState = TrackRecordingState(
            trackId = track.id,
            startTime = System.currentTimeMillis()
        )
        val updatedRecordingTracks = mutableMapOf<String, TrackRecordingState>()
        updatedRecordingTracks[track.id] = newRecordingState

        trackUiState = trackUiState.copy(
            recordingTracks = updatedRecordingTracks,
            isRecordingTrack = true,
            currentRecordingTrack = track,
            trackRecordingStartTime = newRecordingState.startTime
        )

        startTrackRecordingUseCase.execute(track)
    }

    /**
     * 항적 기록 중지 (특정 항적)
     */
    fun stopTrackRecording(trackId: String? = null) {
        val currentTrackUiState = trackUiState
        val targetTrackId = trackId ?: currentTrackUiState.currentRecordingTrack?.id

        if (targetTrackId == null) {
            return
        }

        val recordingState = currentTrackUiState.recordingTracks[targetTrackId] ?: return

        viewModelScope.launch {
            trackRepository.setTrackRecording(targetTrackId, false)
        }

        val updatedRecordingTracks = currentTrackUiState.recordingTracks.toMutableMap()
        updatedRecordingTracks.remove(targetTrackId)

        val isAnyRecording = updatedRecordingTracks.isNotEmpty()
        val remainingTrack = if (isAnyRecording) {
            runBlocking {
                trackRepository.getAllTracks().find { updatedRecordingTracks.containsKey(it.id) }
            }
        } else {
            null
        }

        trackUiState = trackUiState.copy(
            recordingTracks = updatedRecordingTracks,
            isRecordingTrack = isAnyRecording,
            currentRecordingTrack = remainingTrack,
            trackRecordingStartTime = if (remainingTrack != null) {
                updatedRecordingTracks[remainingTrack.id]?.startTime ?: 0L
            } else {
                0L
            }
        )
    }

    /**
     * 모든 항적 기록 중지
     */
    fun stopAllTrackRecording() {
        val recordingTrackIds = trackUiState.recordingTracks.keys.toList()
        recordingTrackIds.forEach { trackId ->
            stopTrackRecording(trackId)
        }
    }

    /**
     * 항적 점 추가 (필요한 경우)
     */
    fun addTrackPointIfNeeded(latitude: Double, longitude: Double, isTimerTriggered: Boolean = false): List<Pair<String, TrackPoint>> {
        val currentTrackUiState = trackUiState
        if (currentTrackUiState.recordingTracks.isEmpty()) {
            return emptyList()
        }

        val currentTime = System.currentTimeMillis()

        val addedPoints = mutableListOf<Pair<String, TrackPoint>>()
        val updatedRecordingTracks = currentTrackUiState.recordingTracks.toMutableMap()

        currentTrackUiState.recordingTracks.forEach { (trackId, recordingState) ->
            val track = runBlocking { getTrackFromCache(trackId) } ?: return@forEach

            val newPoint = addTrackPointUseCase.execute(
                latitude = latitude,
                longitude = longitude,
                currentTime = currentTime,
                lastTrackPointTime = recordingState.lastTrackPointTime,
                lastTrackPointLocation = recordingState.lastTrackPointLocation,
                intervalType = track.intervalType,
                timeInterval = track.timeInterval,
                distanceInterval = track.distanceInterval,
                isTimerTriggered = isTimerTriggered
            )

            if (newPoint != null) {
                viewModelScope.launch {
                    trackRepository.addTrackPoint(trackId, newPoint)
                }

                val updatedState = recordingState.copy(
                    points = (recordingState.points + newPoint).toMutableList(),
                    lastTrackPointTime = currentTime,
                    lastTrackPointLocation = LatLng(latitude, longitude)
                )
                updatedRecordingTracks[trackId] = updatedState
                addedPoints.add(Pair(trackId, newPoint))

                if (trackId == currentTrackUiState.currentRecordingTrack?.id) {
                    addTrackPoint(newPoint)
                }
            }
        }

        if (updatedRecordingTracks != currentTrackUiState.recordingTracks) {
            trackUiState = trackUiState.copy(
                recordingTracks = updatedRecordingTracks,
                lastTrackPointTime = currentTime,
                lastTrackPointLocation = LatLng(latitude, longitude)
            )
        }

        return addedPoints
    }

    // ========== CRUD ==========

    fun getTracks(): List<Track> {
        return runBlocking {
            trackRepository.getAllTracks()
        }
    }

    fun getTrack(trackId: String): Track? {
        return runBlocking {
            getTrackFromCache(trackId)
        }
    }

    fun addTrack(
        name: String,
        color: Color,
        intervalType: String = "time",
        timeInterval: Long = 5000L,
        distanceInterval: Double = 10.0
    ) {
        viewModelScope.launch {
            val newTrack = trackRepository.addTrack(name, color, intervalType, timeInterval, distanceInterval)
            trackCache[newTrack.id] = newTrack
        }
    }

    fun updateTrackSettings(
        trackId: String,
        intervalType: String? = null,
        timeInterval: Long? = null,
        distanceInterval: Double? = null
    ): Boolean {
        val result = runBlocking {
            trackRepository.updateTrackSettings(trackId, intervalType, timeInterval, distanceInterval)
        }
        if (result) {
            invalidateTrackCache(trackId)
        }
        return result
    }

    fun toggleTrackRecording(trackId: String) {
        val isRecording = runBlocking { trackRepository.isTrackRecording(trackId) }
        val track = runBlocking { getTrackFromCache(trackId) } ?: return

        if (isRecording) {
            stopTrackRecording(trackId)
        } else {
            startTrackRecording(track)
        }
    }

    fun deleteTrack(trackId: String) {
        viewModelScope.launch {
            trackRepository.deleteTrack(trackId)
            invalidateTrackCache(trackId)
        }
    }

    // ========== 항적 표시/숨김 ==========

    fun setTrackVisibility(trackId: String, isVisible: Boolean, onLimitExceeded: () -> Unit = {}): Boolean {
        if (isVisible) {
            val currentVisiblePointCount = getTracks()
                .filter { it.isVisible && it.id != trackId }
                .sumOf { it.points.size.coerceAtMost(2000) }

            val track = runBlocking { getTrackFromCache(trackId) }
            val trackPointCount = track?.points?.size?.coerceAtMost(2000) ?: 0

            if (currentVisiblePointCount + trackPointCount > 20000) {
                onLimitExceeded()
                return false
            }
        }

        viewModelScope.launch {
            trackRepository.setTrackVisibility(trackId, isVisible)
            invalidateTrackCache(trackId)
        }
        return true
    }

    // ========== 조회 ==========

    fun getPointsByDate(date: String): List<Pair<String, TrackPoint>> {
        return runBlocking {
            trackRepository.getPointsByDate(date)
        }
    }

    fun getTrackPointsByDate(trackId: String, date: String): List<TrackPoint> {
        return runBlocking {
            trackRepository.getTrackPointsByDate(trackId, date)
        }
    }

    fun getTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): List<TrackPoint> {
        return runBlocking {
            trackRepository.getTrackPointsByTimeRange(trackId, startTime, endTime)
        }
    }

    fun getRecentTrackPoints(trackId: String, limit: Int = 2000): List<TrackPoint> {
        return runBlocking {
            trackRepository.getRecentTrackPoints(trackId, limit)
        }
    }

    fun getAllDates(): List<String> {
        return runBlocking {
            trackRepository.getAllDates()
        }
    }

    fun getDatesByTrackId(trackId: String): List<String> {
        return runBlocking {
            trackRepository.getDatesByTrackId(trackId)
        }
    }

    fun getTrackPoints(trackId: String): List<TrackPoint> {
        return runBlocking {
            trackRepository.getTrackPoints(trackId)
        }
    }

    // ========== 삭제 ==========

    fun deleteTrackPointsByDate(trackId: String, date: String) {
        viewModelScope.launch {
            trackRepository.deleteTrackPointsByDate(trackId, date)
            invalidateTrackCache(trackId)
        }
    }

    fun deleteTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            trackRepository.deleteTrackPointsByTimeRange(trackId, startTime, endTime)
            invalidateTrackCache(trackId)
        }
    }

    /**
     * Factory for TrackViewModel
     */
    companion object {
        fun provideFactory(
            trackRepository: TrackRepository,
            calculateDistanceUseCase: CalculateDistanceUseCase
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val startTrackRecordingUseCase = StartTrackRecordingUseCase()
                    val stopTrackRecordingUseCase = StopTrackRecordingUseCase()
                    val addTrackPointUseCase = AddTrackPointUseCase(calculateDistanceUseCase)

                    return TrackViewModel(
                        trackRepository = trackRepository,
                        startTrackRecordingUseCase = startTrackRecordingUseCase,
                        stopTrackRecordingUseCase = stopTrackRecordingUseCase,
                        addTrackPointUseCase = addTrackPointUseCase,
                        calculateDistanceUseCase = calculateDistanceUseCase
                    ) as T
                }
            }
        }
    }
}
