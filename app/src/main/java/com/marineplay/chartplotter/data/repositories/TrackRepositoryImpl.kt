package com.marineplay.chartplotter.data.repositories

import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.Track
import com.marineplay.chartplotter.TrackPoint
import com.marineplay.chartplotter.TrackRecord
import com.marineplay.chartplotter.TrackSettings
import com.marineplay.chartplotter.data.datasources.LocalDataSource
import com.marineplay.chartplotter.domain.repositories.TrackRepository
import java.util.UUID

/**
 * Track Repository 구현
 */
class TrackRepositoryImpl(
    private val localDataSource: LocalDataSource
) : TrackRepository {
    
    private var tracks: MutableList<Track> = mutableListOf()
    private var settings: TrackSettings = TrackSettings("time", 5000L, 10.0)
    
    init {
        // 초기화 시 데이터 로드
        tracks = localDataSource.loadTracks().toMutableList()
        settings = localDataSource.loadTrackSettings()
    }
    
    override suspend fun getAllTracks(): List<Track> {
        return tracks.toList()
    }
    
    override suspend fun addTrack(name: String, color: Color): Track {
        val track = Track(
            id = UUID.randomUUID().toString(),
            name = name,
            color = color
        )
        tracks.add(track)
        localDataSource.saveTracks(tracks)
        return track
    }
    
    override suspend fun deleteTrack(trackId: String): Boolean {
        val removed = tracks.removeAll { it.id == trackId }
        if (removed) {
            localDataSource.saveTracks(tracks)
        }
        return removed
    }
    
    override suspend fun addTrackRecord(
        trackId: String,
        startTime: Long,
        endTime: Long,
        points: List<TrackPoint>
    ): TrackRecord? {
        val track = tracks.find { it.id == trackId } ?: return null
        
        val record = TrackRecord(
            id = UUID.randomUUID().toString(),
            trackId = trackId,
            startTime = startTime,
            endTime = endTime,
            points = points,
            title = TrackRecord.generateTitle(startTime, endTime)
        )
        
        track.records.add(record)
        localDataSource.saveTracks(tracks)
        return record
    }
    
    override suspend fun deleteTrackRecord(trackId: String, recordId: String): Boolean {
        val track = tracks.find { it.id == trackId } ?: return false
        val removed = track.records.removeAll { it.id == recordId }
        if (removed) {
            localDataSource.saveTracks(tracks)
        }
        return removed
    }
    
    override suspend fun setTrackVisibility(trackId: String, isVisible: Boolean) {
        tracks.find { it.id == trackId }?.isVisible = isVisible
        localDataSource.saveTracks(tracks)
    }
    
    override suspend fun getTrackSettings(): TrackSettings {
        return settings
    }
    
    override suspend fun saveTrackSettings(newSettings: TrackSettings) {
        settings = newSettings
        localDataSource.saveTrackSettings(newSettings)
    }
}

