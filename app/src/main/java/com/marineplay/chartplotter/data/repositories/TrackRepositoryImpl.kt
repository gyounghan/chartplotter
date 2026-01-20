package com.marineplay.chartplotter.data.repositories

import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.data.datasources.TrackLocalDataSource
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.entities.TrackPoint
import com.marineplay.chartplotter.domain.repositories.TrackRepository
import kotlinx.coroutines.flow.Flow

/**
 * Track Repository 구현
 * TrackLocalDataSource를 사용하여 데이터를 관리합니다.
 * TrackPoint를 Track에 직접 저장 (TrackRecord 제거)
 */
class TrackRepositoryImpl(
    private val trackLocalDataSource: TrackLocalDataSource
) : TrackRepository {
    
    override suspend fun getAllTracks(): List<Track> {
        return trackLocalDataSource.getTracksSuspend()
    }
    
    override fun getTracksFlow(): Flow<List<Track>> {
        return trackLocalDataSource.getTracksFlow()
    }
    
    override suspend fun addTrack(
        name: String, 
        color: Color,
        intervalType: String,
        timeInterval: Long,
        distanceInterval: Double
    ): Track {
        return trackLocalDataSource.addTrack(name, color, intervalType, timeInterval, distanceInterval)
    }
    
    override suspend fun deleteTrack(trackId: String): Boolean {
        return trackLocalDataSource.deleteTrack(trackId)
    }
    
    override suspend fun addTrackPoint(trackId: String, point: TrackPoint): Boolean {
        return trackLocalDataSource.addTrackPoint(trackId, point)
    }
    
    override suspend fun addTrackPoints(trackId: String, points: List<TrackPoint>): Boolean {
        return trackLocalDataSource.addTrackPoints(trackId, points)
    }
    
    override suspend fun getTrackPoints(trackId: String): List<TrackPoint> {
        return trackLocalDataSource.getTrackPoints(trackId)
    }
    
    override suspend fun getTrackPointsByDate(trackId: String, date: String): List<TrackPoint> {
        return trackLocalDataSource.getTrackPointsByDate(trackId, date)
    }
    
    override suspend fun getPointsByDate(date: String): List<Pair<String, TrackPoint>> {
        return trackLocalDataSource.getPointsByDate(date)
    }
    
    override suspend fun getTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): List<TrackPoint> {
        return trackLocalDataSource.getTrackPointsByTimeRange(trackId, startTime, endTime)
    }
    
    override suspend fun getRecentTrackPoints(trackId: String, limit: Int): List<TrackPoint> {
        return trackLocalDataSource.getRecentTrackPoints(trackId, limit)
    }
    
    override suspend fun deleteTrackPoints(trackId: String): Boolean {
        return trackLocalDataSource.deleteTrackPoints(trackId)
    }
    
    override suspend fun deleteTrackPointsByDate(trackId: String, date: String): Boolean {
        return trackLocalDataSource.deleteTrackPointsByDate(trackId, date)
    }
    
    override suspend fun deleteTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): Boolean {
        return trackLocalDataSource.deleteTrackPointsByTimeRange(trackId, startTime, endTime)
    }
    
    override suspend fun setTrackVisibility(trackId: String, isVisible: Boolean) {
        trackLocalDataSource.setTrackVisibility(trackId, isVisible)
    }
    
    override suspend fun updateTrackSettings(
        trackId: String,
        intervalType: String?,
        timeInterval: Long?,
        distanceInterval: Double?
    ): Boolean {
        return trackLocalDataSource.updateTrackSettings(trackId, intervalType, timeInterval, distanceInterval)
    }
    
    override suspend fun setTrackRecording(trackId: String, isRecording: Boolean): Boolean {
        return trackLocalDataSource.setTrackRecording(trackId, isRecording)
    }
    
    override suspend fun getRecordingTracks(): List<Track> {
        return trackLocalDataSource.getRecordingTracks()
    }
    
    override suspend fun isTrackRecording(trackId: String): Boolean {
        return trackLocalDataSource.isTrackRecording(trackId)
    }
    
    override suspend fun getAllDates(): List<String> {
        return trackLocalDataSource.getAllDates()
    }
    
    override suspend fun getDatesByTrackId(trackId: String): List<String> {
        return trackLocalDataSource.getDatesByTrackId(trackId)
    }
}
